package com.warehouse.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExportOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String code;
    private String customerName;
    private String customerPhone;  // AES encrypted
    private int userId;
    private String userName;
    private BigDecimal totalAmount;
    private String note;
    private String status;
    private LocalDateTime exportDate;
    private LocalDateTime createdAt;
    private List<ExportOrderItem> items = new ArrayList<>();

    public ExportOrder() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getExportDate() { return exportDate; }
    public void setExportDate(LocalDateTime exportDate) { this.exportDate = exportDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ExportOrderItem> getItems() { return items; }
    public void setItems(List<ExportOrderItem> items) { this.items = items; }
}
