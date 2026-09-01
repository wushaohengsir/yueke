package com.bookmate.dto;

import java.time.LocalDateTime;

public class SlotView {
    private String id;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status; // available | booked

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime s) { this.startAt = s; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime e) { this.endAt = e; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
}
