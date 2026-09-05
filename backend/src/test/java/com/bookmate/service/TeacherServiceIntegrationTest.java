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

import com.bookmate.common.OpStatus;
import com.bookmate.common.OpStatus;
import com.bookmate.dto.SlotView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

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

    @Autowired
    private BookingService bookingService;

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

        OpStatus r = teacherService.completeBooking(TEACHER_ID, future.getId());
        assertEquals(OpStatus.NOT_TIME, r);
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

        OpStatus r = teacherService.completeBooking(TEACHER_ID, past.getId());
        assertEquals(OpStatus.OK, r);
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
    void addTemplate_结束时间不晚于开始应返回bad_time且不插入() {
        OpStatus r = teacherService.addTemplate(TEACHER_ID, 5, "20:00", "19:00", 1L);
        assertEquals(OpStatus.BAD_TIME, r);
        // 不应有任何周五 20:00 起始的模板被插入
        Long cnt = templateMapper.selectCount(new LambdaQueryWrapper<TimeslotTemplate>()
                .eq(TimeslotTemplate::getTeacherId, TEACHER_ID)
                .eq(TimeslotTemplate::getWeekday, 5)
                .eq(TimeslotTemplate::getStartTime, LocalTime.of(20, 0)));
        assertEquals(0L, cnt);
        // 相同值（如 20:00-20:00）同样拒绝
        OpStatus r2 = teacherService.addTemplate(TEACHER_ID, 5, "20:00", "20:00", 1L);
        assertEquals(OpStatus.BAD_TIME, r2);
    }

    @Test
    void updateTemplate_onlyDisabledEditable_rejectsEnabledAndBadTime() {
        // test-data 里模板1(周一 18-19)、模板2(周一 18-20)均停用
        // 1) 停用模板可改时间
        assertEquals(OpStatus.OK, teacherService.updateTemplate(TEACHER_ID, 1L, "09:00", "10:00"));
        TimeslotTemplate up = templateMapper.selectById(1L);
        assertEquals(LocalTime.of(9, 0), up.getStartTime());
        assertEquals(LocalTime.of(10, 0), up.getEndTime());
        // 2) 启用后改时间被拒（模板2仍停用，9-10 不与之重叠，可启用）
        assertEquals(OpStatus.OK, teacherService.toggleTemplate(1L));
        assertEquals(OpStatus.ENABLED, teacherService.updateTemplate(TEACHER_ID, 1L, "11:00", "12:00"));
        // 3) 倒序被拒（先停用再改）
        assertEquals(OpStatus.OK, teacherService.toggleTemplate(1L));
        assertEquals(OpStatus.BAD_TIME, teacherService.updateTemplate(TEACHER_ID, 1L, "20:00", "19:00"));
        // 4) 非本人模板
        assertEquals(OpStatus.NOT_FOUND, teacherService.updateTemplate(99L, 1L, "09:00", "10:00"));
    }

    @Test
    void toggleTemplate_启用重叠模板返回conflict() {
        // test-data.sql 里模板1(18-19)和模板2(18-20)都停用且同天(周一)重叠
        // 先启用模板1
        assertEquals(OpStatus.OK, teacherService.toggleTemplate(1L));
        // 再启用重叠的模板2，应返回 conflict
        assertEquals(OpStatus.CONFLICT, teacherService.toggleTemplate(2L));
        // 模板2应保持停用
        assertEquals(0, templateMapper.selectById(2L).getEnabled());
    }

    @Test
    void toggleTemplate_停用操作不受重叠限制() {
        // 启用模板1
        assertEquals(OpStatus.OK, teacherService.toggleTemplate(1L));
        // 停用模板1（enable=false），应 ok
        assertEquals(OpStatus.OK, teacherService.toggleTemplate(1L));
        assertEquals(0, templateMapper.selectById(1L).getEnabled());
    }

    // ---- 排课辅助：指定日期时段生成 + 模板时段归属 ----
    private static LocalDate nextWeekday(int dow) {
        LocalDate d = LocalDate.now().plusDays(1); // 从明天起找，避免"今天"剔除已过时段导致不稳定
        while (d.getDayOfWeek().getValue() != dow) d = d.plusDays(1);
        return d;
    }

    @Test
    void generateSlotsOn_按启用模板匹配指定日期并标记占用() {
        teacherService.toggleTemplate(1L); // 启用模板1(周一18-19)
        LocalDate mon = nextWeekday(1);
        List<SlotView> slots = bookingService.generateSlotsOn(TEACHER_ID, mon);
        assertEquals(1, slots.size());
        assertEquals("available", slots.get(0).getStatus());
        assertEquals(LocalDateTime.of(mon, LocalTime.of(18, 0)), slots.get(0).getStartAt());

        // 插入一条同 startAt 的活跃预约后，该时段应标记为已占用
        Booking occ = new Booking();
        occ.setTeacherId(TEACHER_ID); occ.setStudentId(2L); occ.setSubjectId(1L);
        occ.setStartAt(LocalDateTime.of(mon, LocalTime.of(18, 0)));
        occ.setEndAt(LocalDateTime.of(mon, LocalTime.of(19, 0)));
        occ.setStatus(1);
        bookingMapper.insert(occ);
        slots = bookingService.generateSlotsOn(TEACHER_ID, mon);
        assertEquals("booked", slots.get(0).getStatus());
    }

    @Test
    void generateSlotsOn_非模板星期应返回空() {
        teacherService.toggleTemplate(1L); // 老师只有周一(1)的模板
        assertEquals(0, bookingService.generateSlotsOn(TEACHER_ID, nextWeekday(3)).size());
    }

    @Test
    void isOpenTemplateSlot_仅启用模板精确时段为真() {
        teacherService.toggleTemplate(1L); // 启用模板1(周一18-19)
        LocalDate mon = nextWeekday(1);
        assertTrue(bookingService.isOpenTemplateSlot(TEACHER_ID,
                LocalDateTime.of(mon, LocalTime.of(18, 0)), LocalDateTime.of(mon, LocalTime.of(19, 0))));
        // 不在模板内的起止
        assertFalse(bookingService.isOpenTemplateSlot(TEACHER_ID,
                LocalDateTime.of(mon, LocalTime.of(18, 30)), LocalDateTime.of(mon, LocalTime.of(19, 30))));
        // 未启用模板(模板2 周一18-20)的时段
        assertFalse(bookingService.isOpenTemplateSlot(TEACHER_ID,
                LocalDateTime.of(mon, LocalTime.of(18, 0)), LocalDateTime.of(mon, LocalTime.of(20, 0))));
    }

    // ---- D5：已下课不可请假 / 老师待完成提醒 ----
    private Booking insertBooking(int status, LocalDateTime start, LocalDateTime end) {
        Booking b = new Booking();
        b.setTeacherId(TEACHER_ID); b.setStudentId(2L); b.setSubjectId(1L);
        b.setStartAt(start); b.setEndAt(end); b.setStatus(status);
        bookingMapper.insert(b);
        return b;
    }

    @Test
    void submitLeave_已下课课程拒绝_未来课程可请_重复被拒() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        Booking past = insertBooking(1, now.minusHours(2), now.minusHours(1));
        assertEquals(OpStatus.ENDED, teacherService.submitLeave(2L, past.getId(), "补假"));
        Booking future = insertBooking(1, now.plusDays(1).withHour(18), now.plusDays(1).withHour(19));
        assertEquals(OpStatus.OK, teacherService.submitLeave(2L, future.getId(), "有事"));
        // 已提交待审批后不可重复提交
        assertEquals(OpStatus.NOT_FOUND, teacherService.submitLeave(2L, future.getId(), "重复"));
    }

    @Test
    void pendingCompletions_仅下课超20分钟未完成() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        Booking a = insertBooking(1, now.minusMinutes(90), now.minusMinutes(40)); // 下课 40 分钟未完成 -> 提醒
        Booking b = insertBooking(1, now.minusMinutes(30), now.minusMinutes(5));  // 下课 5 分钟 -> 不提醒
        Booking c = insertBooking(1, now.minusHours(3), now.minusHours(2));
        teacherService.completeBooking(TEACHER_ID, c.getId());                    // 已完成 -> 不提醒
        Booking d = insertBooking(4, now.minusHours(5), now.minusHours(4));       // 已请假(status4) -> 不提醒

        java.util.List<Long> ids = teacherService.pendingCompletions(TEACHER_ID).stream()
                .map(m -> (Long) m.get("id")).collect(java.util.stream.Collectors.toList());
        assertTrue(ids.contains(a.getId()));
        assertFalse(ids.contains(b.getId()));
        assertFalse(ids.contains(c.getId()));
        assertFalse(ids.contains(d.getId()));
    }
}
