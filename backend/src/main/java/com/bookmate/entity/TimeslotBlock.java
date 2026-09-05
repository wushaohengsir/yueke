package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalTime;

@TableName("timeslot_block")
public class TimeslotBlock {
    @TableId(type = IdType.AUTO) private Long id;
    private Long teacherId;
    private LocalDate blockDate;   // 停课日期
    private Integer type;          // 0 屏蔽（本版仅用屏蔽；1 加开预留）
    private LocalTime startTime;   // 整天停课为 null
    private LocalTime endTime;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long t) { this.teacherId = t; }
    public LocalDate getBlockDate() { return blockDate; }
    public void setBlockDate(LocalDate d) { this.blockDate = d; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime s) { this.startTime = s; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime e) { this.endTime = e; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
}
