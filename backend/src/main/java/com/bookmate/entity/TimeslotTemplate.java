package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

@TableName("timeslot_template")
public class TimeslotTemplate {
    @TableId(type = IdType.AUTO) private Long id;
    private Long teacherId;
    private Integer weekday;
    @JsonFormat(pattern = "HH:mm") private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm") private LocalTime endTime;
    private Long subjectId;
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long t) { this.teacherId = t; }
    public Integer getWeekday() { return weekday; }
    public void setWeekday(Integer w) { this.weekday = w; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime s) { this.startTime = s; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime e) { this.endTime = e; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long s) { this.subjectId = s; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer e) { this.enabled = e; }
}
