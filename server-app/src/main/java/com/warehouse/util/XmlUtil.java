package com.warehouse.util;

import com.warehouse.model.Product;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class XmlUtil {

    public static String exportProducts(List<Product> products) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("products");
        root.setAttribute("count", String.valueOf(products.size()));
        doc.appendChild(root);

        for (Product p : products) {
            Element el = doc.createElement("product");
            el.setAttribute("code", safe(p.getCode()));
            addElement(doc, el, "name", p.getName());
            addElement(doc, el, "categoryId", String.valueOf(p.getCategoryId()));
            addElement(doc, el, "categoryName", p.getCategoryName());
            addElement(doc, el, "brand", p.getBrand());
            addElement(doc, el, "unit", p.getUnit());
            addElement(doc, el, "costPrice", p.getCostPrice() != null ? p.getCostPrice().toPlainString() : "0");
            addElement(doc, el, "sellPrice", p.getSellPrice() != null ? p.getSellPrice().toPlainString() : "0");
            addElement(doc, el, "quantity", String.valueOf(p.getQuantity()));
            addElement(doc, el, "minQuantity", String.valueOf(p.getMinQuantity()));
            addElement(doc, el, "description", p.getDescription());
            root.appendChild(el);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    public static List<Product> importProducts(String xmlContent) throws Exception {
        if (xmlContent == null || xmlContent.isBlank()) throw new Exception("Nội dung XML rỗng");
        List<Product> list = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName("product");
        if (nodes.getLength() == 0) throw new Exception("Không tìm thấy thẻ <product> trong XML");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            Product p = new Product();
            p.setCode(el.getAttribute("code").trim());
            p.setName(getText(el, "name"));
            try { p.setCategoryId(Integer.parseInt(getText(el, "categoryId"))); } catch (Exception e) { p.setCategoryId(1); }
            p.setBrand(getText(el, "brand"));
            String unit = getText(el, "unit");
            p.setUnit(unit.isEmpty() ? "Cái" : unit);
            try { p.setCostPrice(new BigDecimal(getText(el, "costPrice"))); } catch (Exception e) { p.setCostPrice(BigDecimal.ZERO); }
            try { p.setSellPrice(new BigDecimal(getText(el, "sellPrice"))); } catch (Exception e) { throw new Exception("Sản phẩm " + p.getCode() + ": giá bán không hợp lệ"); }
            try { p.setQuantity(Integer.parseInt(getText(el, "quantity"))); } catch (Exception e) { p.setQuantity(0); }
            try { p.setMinQuantity(Integer.parseInt(getText(el, "minQuantity"))); } catch (Exception e) { p.setMinQuantity(5); }
            p.setDescription(getText(el, "description"));
            p.setStatus("ACTIVE");
            if (p.getCode().isEmpty() || p.getName().isEmpty()) throw new Exception("Sản phẩm dòng " + (i+1) + " thiếu mã hoặc tên");
            list.add(p);
        }
        return list;
    }

    private static void addElement(Document doc, Element parent, String tag, String value) {
        Element el = doc.createElement(tag);
        el.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(el);
    }

    private static String getText(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        Node node = nl.item(0).getFirstChild();
        return node != null ? node.getNodeValue().trim() : "";
    }

    private static String safe(String s) { return s != null ? s : ""; }
}
