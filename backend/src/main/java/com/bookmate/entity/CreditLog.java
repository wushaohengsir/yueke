package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("credit_log")
public class CreditLog {
    @TableId(type = IdType.AUTO) private Long id;
    private Long studentId;
    private Long subjectId;
    private Integer delta;
    private String reason;
    private Long refBooking;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long s) { this.studentId = s; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long s) { this.subjectId = s; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer d) { this.delta = d; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public Long getRefBooking() { return refBooking; }
    public void setRefBooking(Long b) { this.refBooking = b; }
}
