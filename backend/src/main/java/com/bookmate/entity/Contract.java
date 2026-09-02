package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("contract")
public class Contract {
    @TableId(type = IdType.AUTO) private Long id;
    private Long studentId;
    private Long teacherId;
    private Integer totalCredits;
    private Integer status; // 0待签署1生效2结束
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long s) { this.studentId = s; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long t) { this.teacherId = t; }
    public Integer getTotalCredits() { return totalCredits; }
    public void setTotalCredits(Integer t) { this.totalCredits = t; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer s) { this.status = s; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime s) { this.signedAt = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
}
