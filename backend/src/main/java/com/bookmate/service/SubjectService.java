package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.Subject;
import com.bookmate.mapper.SubjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubjectService {
    private final SubjectMapper subjectMapper;

    public SubjectService(SubjectMapper s) { this.subjectMapper = s; }

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
}
