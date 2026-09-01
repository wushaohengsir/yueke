package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.Booking;
import com.bookmate.entity.TeacherProfile;
import com.bookmate.mapper.BookingMapper;
import com.bookmate.mapper.TeacherProfileMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {
    private final BookingMapper bookingMapper;
    private final TeacherProfileMapper teacherMapper;

    public BookingService(BookingMapper b, TeacherProfileMapper t) {
        this.bookingMapper = b;
        this.teacherMapper = t;
    }

    public List<TeacherProfile> listTeachers() {
        return teacherMapper.selectList(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getAuditStatus, 1));
    }

    public List<Booking> listByStudent(long studentId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStudentId, studentId).orderByDesc(Booking::getStartAt));
    }

    // 防冲突：同一老师同一开始时间仅一个活跃预约；唯一索引兜底
    @Transactional
    public Booking create(long teacherId, long studentId, LocalDateTime startAt, LocalDateTime endAt) {
        Long cnt = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId)
                .eq(Booking::getStartAt, startAt)
                .in(Booking::getStatus, 0, 1));
        if (cnt != null && cnt > 0) {
            throw new IllegalStateException("该时段已被占用，请选相邻时段");
        }
        Booking b = new Booking();
        b.setTeacherId(teacherId);
        b.setStudentId(studentId);
        b.setStartAt(startAt);
        b.setEndAt(endAt);
        b.setStatus(1);
        try {
            bookingMapper.insert(b);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("该时段刚被占用，请选相邻时段");
        }
        return b;
    }
}
