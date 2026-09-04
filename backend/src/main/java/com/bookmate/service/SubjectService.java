package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.Subject;
import com.bookmate.entity.TeacherSubject;
import com.bookmate.mapper.SubjectMapper;
import com.bookmate.mapper.TeacherSubjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SubjectService {
    private final SubjectMapper subjectMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;

    public SubjectService(SubjectMapper s, TeacherSubjectMapper ts) {
        this.subjectMapper = s;
        this.teacherSubjectMapper = ts;
    }

    public List<Map<String, Object>> listAll() {
        List<Subject> ss = subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Subject s : ss) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("category", s.getCategory());
            out.add(m);
        }
        return out;
    }

    /** 全科目 id → 实体映射（原先各 Service 各自 selectList(null).toMap，复制了 5 处） */
    public Map<Long, Subject> allById() {
        return subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
    }

    /** 某老师的授课科目列表（关联 teacher_subject，过滤已删除科目） */
    public List<Subject> ofTeacher(long teacherId) {
        Map<Long, Subject> byId = allById();
        return teacherSubjectMapper.selectList(
                        new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, teacherId))
                .stream().map(ts -> byId.get(ts.getSubjectId()))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }
}
