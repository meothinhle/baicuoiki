package com.warehouse.util;

import com.warehouse.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CsvUtil {

    private static final String CSV_HEADER = "code,name,categoryId,brand,unit,costPrice,sellPrice,quantity,minQuantity,description";

    public static String exportProducts(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (Product p : products) {
            sb.append(escape(p.getCode())).append(",");
            sb.append(escape(p.getName())).append(",");
            sb.append(p.getCategoryId()).append(",");
            sb.append(escape(p.getBrand())).append(",");
            sb.append(escape(p.getUnit())).append(",");
            sb.append(p.getCostPrice()).append(",");
            sb.append(p.getSellPrice()).append(",");
            sb.append(p.getQuantity()).append(",");
            sb.append(p.getMinQuantity()).append(",");
            sb.append(escape(p.getDescription())).append("\n");
        }
        return sb.toString();
    }

    public static List<Product> importProducts(String csvContent) throws Exception {
        if (csvContent == null || csvContent.isBlank()) throw new Exception("Nội dung CSV rỗng");
        List<Product> list = new ArrayList<>();
        String[] lines = csvContent.split("\n");
        if (lines.length < 2) throw new Exception("File CSV không có dữ liệu");
        // Bỏ qua dòng header
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = parseCsvLine(line);
            if (cols.length < 9) throw new Exception("Dòng " + (i+1) + " không đúng định dạng (thiếu cột)");
            Product p = new Product();
            p.setCode(cols[0].trim());
            p.setName(cols[1].trim());
            try { p.setCategoryId(Integer.parseInt(cols[2].trim())); } catch (NumberFormatException e) { throw new Exception("Dòng " + (i+1) + ": categoryId không hợp lệ"); }
            p.setBrand(cols[3].trim());
            p.setUnit(cols[4].trim().isEmpty() ? "Cái" : cols[4].trim());
            try { p.setCostPrice(new BigDecimal(cols[5].trim())); } catch (NumberFormatException e) { throw new Exception("Dòng " + (i+1) + ": giá nhập không hợp lệ"); }
            try { p.setSellPrice(new BigDecimal(cols[6].trim())); } catch (NumberFormatException e) { throw new Exception("Dòng " + (i+1) + ": giá bán không hợp lệ"); }
            try { p.setQuantity(Integer.parseInt(cols[7].trim())); } catch (NumberFormatException e) { p.setQuantity(0); }
            try { p.setMinQuantity(Integer.parseInt(cols[8].trim())); } catch (NumberFormatException e) { p.setMinQuantity(5); }
            p.setDescription(cols.length > 9 ? cols[9].trim() : "");
            p.setStatus("ACTIVE");
            if (p.getCode().isEmpty() || p.getName().isEmpty()) throw new Exception("Dòng " + (i+1) + ": mã hoặc tên sản phẩm rỗng");
            list.add(p);
        }
        return list;
    }

    private static String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { result.add(current.toString()); current = new StringBuilder(); }
            else { current.append(c); }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
