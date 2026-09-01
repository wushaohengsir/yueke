package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("teacher_profile")
public class TeacherProfile {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String title;
    private String intro;
    private BigDecimal rating;
    private Integer auditStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long u) { this.userId = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getIntro() { return intro; }
    public void setIntro(String i) { this.intro = i; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal r) { this.rating = r; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer a) { this.auditStatus = a; }
}
