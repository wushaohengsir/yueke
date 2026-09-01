package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("teacher_subject")
public class TeacherSubject {
    private Long teacherId;
    private Long subjectId;

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long t) { this.teacherId = t; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long s) { this.subjectId = s; }
}
