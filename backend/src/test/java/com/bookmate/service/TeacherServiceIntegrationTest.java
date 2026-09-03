package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.Booking;
import com.bookmate.entity.TimeslotTemplate;
import com.bookmate.mapper.BookingMapper;
import com.bookmate.mapper.TimeslotTemplateMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TeacherServiceIntegrationTest {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private TimeslotTemplateMapper templateMapper;

    // 老师 id=1
    private static final long TEACHER_ID = 1L;

    @Test
    void completeBooking_未来课应拒绝并返回not_time() {
        // 插入一条未来上课的已确认 booking（endAt 在未来）
        Booking future = new Booking();
        future.setTeacherId(TEACHER_ID);
        future.setStudentId(2L);
        future.setSubjectId(1L);
        future.setStartAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusDays(1).withHour(18).withMinute(0));
        future.setEndAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusDays(1).withHour(19).withMinute(0));
        future.setStatus(1);
        bookingMapper.insert(future);

        String r = teacherService.completeBooking(TEACHER_ID, future.getId());
        assertEquals("not_time", r);
        // 状态应保持已确认，未变成已完成
        Booking after = bookingMapper.selectById(future.getId());
        assertEquals(1, after.getStatus());
    }

    @Test
    void completeBooking_已过结束时间应返回ok() {
        // 插入一条已结束上课的已确认 booking（endAt 在过去）
        Booking past = new Booking();
        past.setTeacherId(TEACHER_ID);
        past.setStudentId(2L);
        past.setSubjectId(1L);
        past.setStartAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusDays(2).withHour(18).withMinute(0));
        past.setEndAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusDays(2).withHour(19).withMinute(0));
        past.setStatus(1);
        bookingMapper.insert(past);

        String r = teacherService.completeBooking(TEACHER_ID, past.getId());
        assertEquals("ok", r);
        Booking after = bookingMapper.selectById(past.getId());
        assertEquals(2, after.getStatus());
    }

    @Test
    void addTemplate_默认停用() {
        teacherService.addTemplate(TEACHER_ID, 3, "10:00", "11:00", 1L);
        TimeslotTemplate t = templateMapper.selectOne(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, TEACHER_ID)
                        .eq(TimeslotTemplate::getWeekday, 3)
                        .eq(TimeslotTemplate::getEnabled, 0)
                        .orderByDesc(TimeslotTemplate::getId)
                        .last("limit 1"));
        assertNotNull(t);
        assertEquals(0, t.getEnabled());
    }

    @Test
    void toggleTemplate_启用重叠模板返回conflict() {
        // test-data.sql 里模板1(18-19)和模板2(18-20)都停用且同天(周一)重叠
        // 先启用模板1
        assertEquals("ok", teacherService.toggleTemplate(1L));
        // 再启用重叠的模板2，应返回 conflict
        assertEquals("conflict", teacherService.toggleTemplate(2L));
        // 模板2应保持停用
        assertEquals(0, templateMapper.selectById(2L).getEnabled());
    }

    @Test
    void toggleTemplate_停用操作不受重叠限制() {
        // 启用模板1
        assertEquals("ok", teacherService.toggleTemplate(1L));
        // 停用模板1（enable=false），应 ok
        assertEquals("ok", teacherService.toggleTemplate(1L));
        assertEquals(0, templateMapper.selectById(1L).getEnabled());
    }
}
