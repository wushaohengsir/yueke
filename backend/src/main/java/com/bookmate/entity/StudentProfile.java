package com.bookmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("student_profile")
public class StudentProfile {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Integer creditsTotal;
    private Integer creditsUsed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long u) { this.userId = u; }
    public Integer getCreditsTotal() { return creditsTotal; }
    public void setCreditsTotal(Integer t) { this.creditsTotal = t; }
    public Integer getCreditsUsed() { return creditsUsed; }
    public void setCreditsUsed(Integer u) { this.creditsUsed = u; }
}
