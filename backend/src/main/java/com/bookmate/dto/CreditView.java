package com.bookmate.dto;

public class CreditView {
    private Long subjectId;
    private String subjectName;
    private String category;
    private int total;
    private int used;
    private int remaining;

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long s) { this.subjectId = s; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String n) { this.subjectName = n; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public int getTotal() { return total; }
    public void setTotal(int t) { this.total = t; }
    public int getUsed() { return used; }
    public void setUsed(int u) { this.used = u; }
    public int getRemaining() { return remaining; }
    public void setRemaining(int r) { this.remaining = r; }
}
