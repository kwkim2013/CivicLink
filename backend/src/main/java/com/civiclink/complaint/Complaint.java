package com.civiclink.complaint;

public class Complaint {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String status;

    public Complaint() {
    }

    public Complaint(Long id, String title, String content, String category, String status) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
