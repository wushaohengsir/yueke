package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("student_credit")
public class StudentCredit {
    @TableId(type = IdType.AUTO) private Long id;
    private Long studentId;
    private Long subjectId;
    private Integer creditsTotal;
    private Integer creditsUsed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long s) { this.studentId = s; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long s) { this.subjectId = s; }
    public Integer getCreditsTotal() { return creditsTotal; }
    public void setCreditsTotal(Integer t) { this.creditsTotal = t; }
    public Integer getCreditsUsed() { return creditsUsed; }
    public void setCreditsUsed(Integer u) { this.creditsUsed = u; }
}
