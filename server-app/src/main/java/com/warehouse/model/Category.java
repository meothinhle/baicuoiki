package com.warehouse.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String code;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public Category() {}

    public Category(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = "ACTIVE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}
