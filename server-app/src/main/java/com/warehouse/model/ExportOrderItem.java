package com.warehouse.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class ExportOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int exportOrderId;
    private int productId;
    private String productCode;
    private String productName;
    private String productUnit;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public ExportOrderItem() {}

    public ExportOrderItem(int productId, String productCode, String productName,
                           String productUnit, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.productUnit = productUnit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getExportOrderId() { return exportOrderId; }
    public void setExportOrderId(int exportOrderId) { this.exportOrderId = exportOrderId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductUnit() { return productUnit; }
    public void setProductUnit(String productUnit) { this.productUnit = productUnit; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
