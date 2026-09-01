package com.bookmate.dto;

import java.math.BigDecimal;
import java.util.List;

public class TeacherView {
    private Long id;        // teacher user_id
    private String name;
    private String title;
    private String intro;
    private BigDecimal rating;
    private List<String> subjects;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getIntro() { return intro; }
    public void setIntro(String i) { this.intro = i; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal r) { this.rating = r; }
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> s) { this.subjects = s; }
}
