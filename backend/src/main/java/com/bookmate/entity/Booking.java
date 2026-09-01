package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("booking")
public class Booking {
    @TableId(type = IdType.AUTO) private Long id;
    private Long teacherId;
    private Long studentId;
    private Long subjectId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long t) { this.teacherId = t; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long s) { this.studentId = s; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long s) { this.subjectId = s; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime s) { this.startAt = s; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime e) { this.endAt = e; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer s) { this.status = s; }
    public String getRemark() { return remark; }
    public void setRemark(String r) { this.remark = r; }
}
