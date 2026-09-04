package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ContractService {
    private final ContractMapper contractMapper;
    private final UserMapper userMapper;
    private final SubjectService subjectService;
    private final CreditService creditService;

    public ContractService(ContractMapper c, UserMapper u, SubjectService ss, CreditService cs) {
        this.contractMapper = c; this.userMapper = u;
        this.subjectService = ss; this.creditService = cs;
    }

    // 学员合同列表（带老师/科目信息）
    public List<Map<String, Object>> listByStudent(long studentId) {
        List<Contract> cs = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getStudentId, studentId).orderByDesc(Contract::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Contract c : cs) {
            User t = userMapper.selectById(c.getTeacherId());
            String subjName = subjectService.ofTeacher(c.getTeacherId()).stream()
                    .map(Subject::getName).findFirst().orElse("");
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
        c.setSignedAt(java.time.LocalDateTime.now());
        contractMapper.updateById(c);

        // 该老师主科目入账
        Long subjectId = creditService.primarySubjectId(c.getTeacherId());
        if (subjectId == null) return false;
        creditService.grant(studentId, subjectId, c.getTotalCredits(), "购买课时包(" + c.getId() + ")");
        return true;
    }
}
