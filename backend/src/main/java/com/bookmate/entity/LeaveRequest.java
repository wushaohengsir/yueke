package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("leave_request")
public class LeaveRequest {
    @TableId(type = IdType.AUTO) private Long id;
    private Long bookingId;
    private Long studentId;
    private String reason;
    private Integer status; // 0待审批1批准2驳回
    private Long handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long b) { this.bookingId = b; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long s) { this.studentId = s; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer s) { this.status = s; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long h) { this.handledBy = h; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime h) { this.handledAt = h; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
}
