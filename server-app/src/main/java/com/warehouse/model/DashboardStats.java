package com.warehouse.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalProducts;
    private int lowStockProducts;
    private int totalSuppliers;
    private int totalCategories;
    private int totalImportOrders;
    private int totalExportOrders;
    private BigDecimal totalImportValue;
    private BigDecimal totalExportValue;
    private List<Map<String, Object>> recentImports;
    private List<Map<String, Object>> recentExports;
    private List<Map<String, Object>> lowStockList;
    private Map<String, Integer> productsByCategory;

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

    public int getLowStockProducts() { return lowStockProducts; }
    public void setLowStockProducts(int lowStockProducts) { this.lowStockProducts = lowStockProducts; }

    public int getTotalSuppliers() { return totalSuppliers; }
    public void setTotalSuppliers(int totalSuppliers) { this.totalSuppliers = totalSuppliers; }

    public int getTotalCategories() { return totalCategories; }
    public void setTotalCategories(int totalCategories) { this.totalCategories = totalCategories; }

    public int getTotalImportOrders() { return totalImportOrders; }
    public void setTotalImportOrders(int totalImportOrders) { this.totalImportOrders = totalImportOrders; }

    public int getTotalExportOrders() { return totalExportOrders; }
    public void setTotalExportOrders(int totalExportOrders) { this.totalExportOrders = totalExportOrders; }

    public BigDecimal getTotalImportValue() { return totalImportValue; }
    public void setTotalImportValue(BigDecimal totalImportValue) { this.totalImportValue = totalImportValue; }

    public BigDecimal getTotalExportValue() { return totalExportValue; }
    public void setTotalExportValue(BigDecimal totalExportValue) { this.totalExportValue = totalExportValue; }

    public List<Map<String, Object>> getRecentImports() { return recentImports; }
    public void setRecentImports(List<Map<String, Object>> recentImports) { this.recentImports = recentImports; }

    public List<Map<String, Object>> getRecentExports() { return recentExports; }
    public void setRecentExports(List<Map<String, Object>> recentExports) { this.recentExports = recentExports; }

    public List<Map<String, Object>> getLowStockList() { return lowStockList; }
    public void setLowStockList(List<Map<String, Object>> lowStockList) { this.lowStockList = lowStockList; }

    public Map<String, Integer> getProductsByCategory() { return productsByCategory; }
    public void setProductsByCategory(Map<String, Integer> productsByCategory) { this.productsByCategory = productsByCategory; }
}
