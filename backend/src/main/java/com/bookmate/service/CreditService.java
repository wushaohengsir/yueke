package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.dto.CreditView;
import com.bookmate.entity.CreditLog;
import com.bookmate.entity.StudentCredit;
import com.bookmate.entity.Subject;
import com.bookmate.entity.TeacherSubject;
import com.bookmate.mapper.CreditLogMapper;
import com.bookmate.mapper.StudentCreditMapper;
import com.bookmate.mapper.TeacherSubjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 课时账本：分课程课时的扣减/返还/入账与流水（CreditLog）唯一出口。
 * 原先同一套「查 StudentCredit → 改 used/total → 写 CreditLog」逻辑散落在
 * BookingService（约课扣减、请假返还）、TeacherService（审批返还）、ContractService（签署入账）
 * 四个地方，收敛于此：规则改一处，全站生效。
 */
@Service
public class CreditService {
    private final StudentCreditMapper creditMapper;
    private final CreditLogMapper creditLogMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final SubjectService subjectService;

    public CreditService(StudentCreditMapper sc, CreditLogMapper cl,
                         TeacherSubjectMapper ts, SubjectService ss) {
        this.creditMapper = sc; this.creditLogMapper = cl;
        this.teacherSubjectMapper = ts; this.subjectService = ss;
    }

    /** 老师主科目（teacher_subject 第一条；未设置返回 null） */
    public Long primarySubjectId(long teacherId) {
        List<TeacherSubject> tss = teacherSubjectMapper.selectList(
                new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, teacherId));
        return tss.isEmpty() ? null : tss.get(0).getSubjectId();
    }

    /** 校验课时有剩余（约课前预检）；不足抛 IllegalStateException（Controller 转 409） */
    public void requireRemaining(long studentId, Long subjectId) {
        StudentCredit sc = find(studentId, subjectId);
        if (sc == null || (sc.getCreditsTotal() - sc.getCreditsUsed()) <= 0) {
            throw new IllegalStateException("该课程课时不足，请先购买对应课程课时包");
        }
    }

    /** 约课扣减 1 课时；课时不足抛 IllegalStateException（Controller 转 409） */
    public void consume(long studentId, Long subjectId, long bookingId) {
        requireRemaining(studentId, subjectId);
        StudentCredit sc = find(studentId, subjectId);
        sc.setCreditsUsed(sc.getCreditsUsed() + 1);
        creditMapper.updateById(sc);
        writeLog(studentId, subjectId, -1, "约课消耗", bookingId);
    }

    /** 请假审批通过返还 1 课时（无课时记录则跳过，不报错） */
    public void refund(long studentId, Long subjectId, long bookingId) {
        StudentCredit sc = find(studentId, subjectId);
        if (sc == null) return;
        sc.setCreditsUsed(Math.max(0, sc.getCreditsUsed() - 1));
        creditMapper.updateById(sc);
        writeLog(studentId, subjectId, 1, "请假返还", bookingId);
    }

    /** 课时入账（签署合同）；无记录则先建档 */
    public void grant(long studentId, long subjectId, int credits, String reason) {
        StudentCredit sc = find(studentId, subjectId);
        if (sc == null) {
            sc = new StudentCredit();
            sc.setStudentId(studentId); sc.setSubjectId(subjectId);
            sc.setCreditsTotal(0); sc.setCreditsUsed(0);
            creditMapper.insert(sc);
        }
        sc.setCreditsTotal(sc.getCreditsTotal() + credits);
        creditMapper.updateById(sc);
        writeLog(studentId, subjectId, credits, reason, null);
    }

    /** 学员分课程课时视图（首页/我的信息展示用） */
    public List<CreditView> viewsOf(long studentId) {
        List<StudentCredit> list = creditMapper.selectList(
                new LambdaQueryWrapper<StudentCredit>().eq(StudentCredit::getStudentId, studentId));
        Map<Long, Subject> subjById = subjectService.allById();
        List<CreditView> views = new ArrayList<>();
        for (StudentCredit sc : list) {
            Subject s = subjById.get(sc.getSubjectId());
            CreditView cv = new CreditView();
            cv.setSubjectId(sc.getSubjectId());
            cv.setSubjectName(s != null ? s.getName() : "");
            cv.setCategory(s != null ? s.getCategory() : "");
            cv.setTotal(sc.getCreditsTotal());
            cv.setUsed(sc.getCreditsUsed());
            cv.setRemaining(sc.getCreditsTotal() - sc.getCreditsUsed());
            views.add(cv);
        }
        return views;
    }

    private StudentCredit find(long studentId, Long subjectId) {
        return creditMapper.selectOne(new LambdaQueryWrapper<StudentCredit>()
                .eq(StudentCredit::getStudentId, studentId)
                .eq(StudentCredit::getSubjectId, subjectId));
    }

    private void writeLog(long studentId, Long subjectId, int delta, String reason, Long refBooking) {
        CreditLog log = new CreditLog();
        log.setStudentId(studentId); log.setSubjectId(subjectId);
        log.setDelta(delta); log.setReason(reason); log.setRefBooking(refBooking);
        creditLogMapper.insert(log);
    }
}
