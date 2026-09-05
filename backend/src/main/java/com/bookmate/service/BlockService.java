package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.common.OpStatus;
import com.bookmate.entity.TeacherProfile;
import com.bookmate.entity.TimeslotBlock;
import com.bookmate.mapper.TeacherProfileMapper;
import com.bookmate.mapper.TimeslotBlockMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 停课/屏蔽：老师某天（或某天某时段）暂停可约，学生当天约不到。
 * 底层复用预留表 timeslot_block(type=0 屏蔽；整天停课 start/end 为 null)。
 */
@Service
public class BlockService {
    private final TimeslotBlockMapper blockMapper;
    private final TeacherProfileMapper teacherMapper;

    public BlockService(TimeslotBlockMapper blockMapper, TeacherProfileMapper teacherMapper) {
        this.blockMapper = blockMapper;
        this.teacherMapper = teacherMapper;
    }

    // 某老师某日的屏蔽记录
    public List<TimeslotBlock> blocksOn(long teacherId, LocalDate date) {
        return blockMapper.selectList(new LambdaQueryWrapper<TimeslotBlock>()
                .eq(TimeslotBlock::getType, 0)
                .eq(TimeslotBlock::getTeacherId, teacherId)
                .eq(TimeslotBlock::getBlockDate, date));
    }

    // [s,e) 是否命中任一屏蔽：整天停课(null)恒真；否则按时间段重叠判断
    public boolean overlaps(List<TimeslotBlock> blocks, LocalTime s, LocalTime e) {
        for (TimeslotBlock b : blocks) {
            if (b.getStartTime() == null || b.getEndTime() == null) return true;
            if (s.isBefore(b.getEndTime()) && b.getStartTime().isBefore(e)) return true;
        }
        return false;
    }

    // 该 start-end 时段是否处于停课中（供管理员直排拦截）
    public boolean isSlotBlocked(long teacherId, LocalDateTime start, LocalDateTime end) {
        return overlaps(blocksOn(teacherId, start.toLocalDate()), start.toLocalTime(), end.toLocalTime());
    }

    // 新增停课：全天(双 null)或精确时段；整天重复记录返回 CONFLICT
    public OpStatus addBlock(long teacherId, LocalDate date, LocalTime start, LocalTime end, String reason) {
        if (teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, teacherId)) == null) return OpStatus.NOT_FOUND;
        boolean allDay = start == null && end == null;
        if (!allDay) {
            if (start == null || end == null) return OpStatus.BAD_TIME;
            if (!start.isBefore(end)) return OpStatus.BAD_TIME;
        }
        if (allDay) {
            Long cnt = blockMapper.selectCount(new LambdaQueryWrapper<TimeslotBlock>()
                    .eq(TimeslotBlock::getType, 0)
                    .eq(TimeslotBlock::getTeacherId, teacherId)
                    .eq(TimeslotBlock::getBlockDate, date)
                    .isNull(TimeslotBlock::getStartTime));
            if (cnt != null && cnt > 0) return OpStatus.CONFLICT;
        }
        TimeslotBlock b = new TimeslotBlock();
        b.setTeacherId(teacherId); b.setBlockDate(date); b.setType(0);
        b.setStartTime(start); b.setEndTime(end); b.setReason(reason);
        blockMapper.insert(b);
        return OpStatus.OK;
    }

    // 删除停课（管理员恢复；权限已在 Controller 校验）
    public boolean removeBlock(long id) {
        TimeslotBlock b = blockMapper.selectById(id);
        if (b == null) return false;
        blockMapper.deleteById(id);
        return true;
    }

    // 该老师全部停课记录（倒序）
    public List<TimeslotBlock> listBlocks(long teacherId) {
        return blockMapper.selectList(new LambdaQueryWrapper<TimeslotBlock>()
                .eq(TimeslotBlock::getType, 0)
                .eq(TimeslotBlock::getTeacherId, teacherId)
                .orderByDesc(TimeslotBlock::getBlockDate));
    }
}
