package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContractService {
    private final ContractMapper contractMapper;
    private final StudentCreditMapper creditMapper;
    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final SubjectMapper subjectMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final TeacherProfileMapper teacherMapper;

    public ContractService(ContractMapper c, StudentCreditMapper sc, CreditLogMapper cl,
                           UserMapper u, SubjectMapper s, TeacherSubjectMapper ts, TeacherProfileMapper tm) {
        this.contractMapper = c; this.creditMapper = sc; this.creditLogMapper = cl;
        this.userMapper = u; this.subjectMapper = s; this.teacherSubjectMapper = ts; this.teacherMapper = tm;
    }

    // 学员合同列表（带老师/科目信息）
    public List<Map<String, Object>> listByStudent(long studentId) {
        List<Contract> cs = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getStudentId, studentId).orderByDesc(Contract::getId));
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Contract c : cs) {
            User t = userMapper.selectById(c.getTeacherId());
            List<TeacherSubject> tss = teacherSubjectMapper.selectList(
                    new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, c.getTeacherId()));
            String subjName = tss.stream().map(ts -> subjById.get(ts.getSubjectId()))
                    .filter(Objects::nonNull).map(Subject::getName).findFirst().orElse("");
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("teacherName", t != null ? t.getName() : "");
            m.put("subjectName", subjName);
            m.put("totalCredits", c.getTotalCredits());
            m.put("status", c.getStatus());
            m.put("createdAt", c.getCreatedAt());
            m.put("signedAt", c.getSignedAt());
            out.add(m);
        }
        return out;
    }

    // 购买课时包：生成待签署合同
    public Contract purchase(long studentId, long teacherId, int credits) {
        Contract c = new Contract();
        c.setStudentId(studentId); c.setTeacherId(teacherId);
        c.setTotalCredits(credits); c.setStatus(0);
        contractMapper.insert(c);
        return c;
    }

    // 签署合同：生效 + 对应课程课时入账（分课程课时，V1.0 线下结算）
    @Transactional
    public boolean sign(long studentId, long contractId) {
        Contract c = contractMapper.selectById(contractId);
        if (c == null || !c.getStudentId().equals(studentId) || c.getStatus() != 0) return false;
        c.setStatus(1);
        c.setSignedAt(LocalDateTime.now());
        contractMapper.updateById(c);

        // 该老师主科目入账
        List<TeacherSubject> tss = teacherSubjectMapper.selectList(
                new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, c.getTeacherId()));
        if (tss.isEmpty()) return false;
        Long subjectId = tss.get(0).getSubjectId();

        StudentCredit sc = creditMapper.selectOne(new LambdaQueryWrapper<StudentCredit>()
                .eq(StudentCredit::getStudentId, studentId)
                .eq(StudentCredit::getSubjectId, subjectId));
        if (sc == null) {
            sc = new StudentCredit();
            sc.setStudentId(studentId); sc.setSubjectId(subjectId);
            sc.setCreditsTotal(0); sc.setCreditsUsed(0);
            creditMapper.insert(sc);
        }
        sc.setCreditsTotal(sc.getCreditsTotal() + c.getTotalCredits());
        creditMapper.updateById(sc);

        CreditLog log = new CreditLog();
        log.setStudentId(studentId); log.setSubjectId(subjectId);
        log.setDelta(c.getTotalCredits()); log.setReason("购买课时包(" + c.getId() + ")");
        creditLogMapper.insert(log);
        return true;
    }
}
