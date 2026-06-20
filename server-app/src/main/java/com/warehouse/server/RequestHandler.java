package com.warehouse.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.warehouse.dao.*;
import com.warehouse.model.*;
import com.warehouse.service.AuthService;
import com.warehouse.util.CsvUtil;
import com.warehouse.util.GsonFactory;
import com.warehouse.util.XmlUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Xử lý toàn bộ request từ client, phân loại theo action
 */
public class RequestHandler {
    private static final Logger logger = Logger.getLogger(RequestHandler.class.getName());

    private final Gson gson = GsonFactory.create();
    private final AuthService authService;

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ImportOrderDAO importOrderDAO = new ImportOrderDAO();
    private final ExportOrderDAO exportOrderDAO = new ExportOrderDAO();
    private final SystemLogDAO logDAO = new SystemLogDAO();
    private final DashboardDAO dashboardDAO = new DashboardDAO();

    public RequestHandler(AuthService authService) {
        this.authService = authService;
    }

    public Response handle(Request request, String clientIp) {
        if (request == null || request.getAction() == null) {
            return Response.fail("Request không hợp lệ");
        }

        String action = request.getAction();
        String token = request.getToken();
        logger.info("Xử lý action: " + action + " từ " + clientIp);

        // Các action không cần auth
        if ("LOGIN".equals(action)) {
            return handleLogin(request, clientIp);
        }
        if ("LOGOUT".equals(action)) {
            authService.logout(token, clientIp);
            return Response.ok("Đăng xuất thành công");
        }

        // Kiểm tra token
        User currentUser = authService.validateToken(token);
        if (currentUser == null) {
            return Response.fail("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại");
        }

        try {
            return switch (action) {
                // ---- DASHBOARD ----
                case "DASHBOARD_STATS" -> Response.ok("OK", dashboardDAO.getStats());

                // ---- USERS (Admin only) ----
                case "USER_FIND_ALL" -> requireAdmin(currentUser) ? Response.ok("OK", userDAO.findAll()) : Response.fail("Không có quyền");
                case "USER_SEARCH" -> requireAdmin(currentUser) ? Response.ok("OK", userDAO.search(dataAsString(request))) : Response.fail("Không có quyền");
                case "USER_CREATE" -> handleUserCreate(request, currentUser, clientIp);
                case "USER_UPDATE" -> handleUserUpdate(request, currentUser, clientIp);
                case "USER_DELETE" -> handleUserDelete(request, currentUser, clientIp);
                case "USER_UNLOCK" -> handleUserUnlock(request, currentUser, clientIp);
                case "USER_CHANGE_PASSWORD" -> handleChangePassword(request, currentUser, clientIp);

                // ---- CATEGORIES ----
                case "CATEGORY_FIND_ALL" -> Response.ok("OK", categoryDAO.findAll());
                case "CATEGORY_SEARCH" -> Response.ok("OK", categoryDAO.search(dataAsString(request)));
                case "CATEGORY_CREATE" -> handleCategoryCreate(request, currentUser, clientIp);
                case "CATEGORY_UPDATE" -> handleCategoryUpdate(request, currentUser, clientIp);
                case "CATEGORY_DELETE" -> handleCategoryDelete(request, currentUser, clientIp);

                // ---- PRODUCTS ----
                case "PRODUCT_FIND_ALL" -> Response.ok("OK", productDAO.findAll());
                case "PRODUCT_SEARCH" -> Response.ok("OK", productDAO.search(dataAsString(request)));
                case "PRODUCT_FIND_BY_CATEGORY" -> Response.ok("OK", productDAO.findByCategory(dataAsInt(request)));
                case "PRODUCT_LOW_STOCK" -> Response.ok("OK", productDAO.findLowStock());
                case "PRODUCT_CREATE" -> handleProductCreate(request, currentUser, clientIp);
                case "PRODUCT_UPDATE" -> handleProductUpdate(request, currentUser, clientIp);
                case "PRODUCT_DELETE" -> handleProductDelete(request, currentUser, clientIp);

                // ---- SUPPLIERS ----
                case "SUPPLIER_FIND_ALL" -> Response.ok("OK", supplierDAO.findAll());
                case "SUPPLIER_SEARCH" -> Response.ok("OK", supplierDAO.search(dataAsString(request)));
                case "SUPPLIER_CREATE" -> handleSupplierCreate(request, currentUser, clientIp);
                case "SUPPLIER_UPDATE" -> handleSupplierUpdate(request, currentUser, clientIp);
                case "SUPPLIER_DELETE" -> handleSupplierDelete(request, currentUser, clientIp);

                // ---- IMPORT ORDERS ----
                case "IMPORT_FIND_ALL" -> Response.ok("OK", importOrderDAO.findAll());
                case "IMPORT_SEARCH" -> Response.ok("OK", importOrderDAO.search(dataAsString(request)));
                case "IMPORT_FIND_BY_ID" -> Response.ok("OK", importOrderDAO.findById(dataAsInt(request)));
                case "IMPORT_CREATE" -> handleImportCreate(request, currentUser, clientIp);
                case "IMPORT_GENERATE_CODE" -> Response.ok("OK", importOrderDAO.generateCode());

                // ---- EXPORT ORDERS ----
                case "EXPORT_FIND_ALL" -> Response.ok("OK", exportOrderDAO.findAll());
                case "EXPORT_SEARCH" -> Response.ok("OK", exportOrderDAO.search(dataAsString(request)));
                case "EXPORT_FIND_BY_ID" -> Response.ok("OK", exportOrderDAO.findById(dataAsInt(request)));
                case "EXPORT_CREATE" -> handleExportCreate(request, currentUser, clientIp);
                case "EXPORT_GENERATE_CODE" -> Response.ok("OK", exportOrderDAO.generateCode());

                // ---- LOGS ----
                case "LOG_FIND_ALL" -> requireAdmin(currentUser) ? Response.ok("OK", logDAO.findAll(200)) : Response.fail("Không có quyền");

                // ---- IMPORT/EXPORT FILE ----
                case "FILE_EXPORT_PRODUCTS_CSV" -> handleExportProductsCsv(currentUser, clientIp);
                case "FILE_IMPORT_PRODUCTS_CSV" -> handleImportProductsCsv(request, currentUser, clientIp);
                case "FILE_EXPORT_PRODUCTS_XML" -> handleExportProductsXml(currentUser, clientIp);
                case "FILE_IMPORT_PRODUCTS_XML" -> handleImportProductsXml(request, currentUser, clientIp);

                default -> Response.fail("Action không hợp lệ: " + action);
            };
        } catch (Exception e) {
            logger.severe("Lỗi xử lý action " + action + ": " + e.getMessage());
            return Response.fail("Lỗi server: " + e.getMessage());
        }
    }

    // ======================== AUTH ========================
    private Response handleLogin(Request request, String clientIp) {
        try {
            Map<String, String> creds = gson.fromJson(gson.toJson(request.getData()), new TypeToken<Map<String, String>>(){}.getType());
            User user = authService.login(creds.get("username"), creds.get("password"), clientIp);
            return Response.ok("Đăng nhập thành công", user);
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    // ======================== USERS ========================
    private Response handleUserCreate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        try {
            User user = gson.fromJson(gson.toJson(request.getData()), User.class);
            if (user.getUsername() == null || user.getUsername().isBlank()) return Response.fail("Tên đăng nhập không được để trống");
            if (user.getPassword() == null || user.getPassword().length() < 6) return Response.fail("Mật khẩu phải ít nhất 6 ký tự");
            if (userDAO.findByUsername(user.getUsername()) != null) return Response.fail("Tên đăng nhập đã tồn tại");
            boolean ok = userDAO.insert(user);
            if (ok) logDAO.log(currentUser.getUsername(), "CREATE_USER", user.getUsername(), "Tạo tài khoản mới", true, clientIp);
            return ok ? Response.ok("Tạo tài khoản thành công") : Response.fail("Tạo tài khoản thất bại");
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    private Response handleUserUpdate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        User user = gson.fromJson(gson.toJson(request.getData()), User.class);
        boolean ok = userDAO.update(user);
        if (ok) logDAO.log(currentUser.getUsername(), "UPDATE_USER", user.getUsername(), "Cập nhật tài khoản", true, clientIp);
        return ok ? Response.ok("Cập nhật thành công") : Response.fail("Cập nhật thất bại");
    }

    private Response handleUserDelete(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        int id = dataAsInt(request);
        User target = userDAO.findById(id);
        if (target != null && "admin".equals(target.getUsername())) return Response.fail("Không thể khoá tài khoản admin chính");
        boolean ok = userDAO.delete(id);
        if (ok && target != null) logDAO.log(currentUser.getUsername(), "DELETE_USER", target.getUsername(), "Khoá tài khoản", true, clientIp);
        return ok ? Response.ok("Khoá tài khoản thành công") : Response.fail("Thao tác thất bại");
    }

    private Response handleUserUnlock(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        int id = dataAsInt(request);
        User target = userDAO.findById(id);
        boolean ok = userDAO.unlock(id);
        if (ok && target != null) logDAO.log(currentUser.getUsername(), "UNLOCK_USER", target.getUsername(), "Mở khoá tài khoản", true, clientIp);
        return ok ? Response.ok("Mở khoá tài khoản thành công") : Response.fail("Thao tác thất bại");
    }

    private Response handleChangePassword(Request request, User currentUser, String clientIp) {
        try {
            Map<String, Object> data = gson.fromJson(gson.toJson(request.getData()), new TypeToken<Map<String, Object>>(){}.getType());
            int userId = ((Double) data.get("userId")).intValue();
            String newPassword = (String) data.get("newPassword");
            // User thường chỉ đổi mật khẩu của mình
            if (!requireAdmin(currentUser) && currentUser.getId() != userId) return Response.fail("Không có quyền");
            if (newPassword == null || newPassword.length() < 6) return Response.fail("Mật khẩu phải ít nhất 6 ký tự");
            boolean ok = userDAO.updatePassword(userId, newPassword);
            if (ok) logDAO.log(currentUser.getUsername(), "CHANGE_PASSWORD", "userId=" + userId, "Đổi mật khẩu", true, clientIp);
            return ok ? Response.ok("Đổi mật khẩu thành công") : Response.fail("Đổi mật khẩu thất bại");
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    // ======================== CATEGORIES ========================
    private Response handleCategoryCreate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        Category c = gson.fromJson(gson.toJson(request.getData()), Category.class);
        if (c.getCode() == null || c.getCode().isBlank()) return Response.fail("Mã danh mục không được để trống");
        if (c.getName() == null || c.getName().isBlank()) return Response.fail("Tên danh mục không được để trống");
        boolean ok = categoryDAO.insert(c);
        if (ok) logDAO.log(currentUser.getUsername(), "CREATE_CATEGORY", c.getCode(), c.getName(), true, clientIp);
        return ok ? Response.ok("Thêm danh mục thành công") : Response.fail("Mã danh mục đã tồn tại hoặc lỗi hệ thống");
    }

    private Response handleCategoryUpdate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        Category c = gson.fromJson(gson.toJson(request.getData()), Category.class);
        boolean ok = categoryDAO.update(c);
        if (ok) logDAO.log(currentUser.getUsername(), "UPDATE_CATEGORY", c.getCode(), c.getName(), true, clientIp);
        return ok ? Response.ok("Cập nhật danh mục thành công") : Response.fail("Cập nhật thất bại");
    }

    private Response handleCategoryDelete(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        int id = dataAsInt(request);
        boolean ok = categoryDAO.delete(id);
        if (ok) logDAO.log(currentUser.getUsername(), "DELETE_CATEGORY", "id=" + id, "Xoá danh mục", true, clientIp);
        return ok ? Response.ok("Xoá danh mục thành công") : Response.fail("Xoá thất bại");
    }

    // ======================== PRODUCTS ========================
    private Response handleProductCreate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        Product p = gson.fromJson(gson.toJson(request.getData()), Product.class);
        if (p.getCode() == null || p.getCode().isBlank()) return Response.fail("Mã sản phẩm không được để trống");
        if (p.getName() == null || p.getName().isBlank()) return Response.fail("Tên sản phẩm không được để trống");
        if (p.getSellPrice() == null || p.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) return Response.fail("Giá bán phải lớn hơn 0");
        if (productDAO.findByCode(p.getCode()) != null) return Response.fail("Mã sản phẩm đã tồn tại");
        boolean ok = productDAO.insert(p);
        if (ok) logDAO.log(currentUser.getUsername(), "CREATE_PRODUCT", p.getCode(), p.getName(), true, clientIp);
        return ok ? Response.ok("Thêm sản phẩm thành công") : Response.fail("Thêm sản phẩm thất bại");
    }

    private Response handleProductUpdate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        Product p = gson.fromJson(gson.toJson(request.getData()), Product.class);
        boolean ok = productDAO.update(p);
        if (ok) logDAO.log(currentUser.getUsername(), "UPDATE_PRODUCT", p.getCode(), p.getName(), true, clientIp);
        return ok ? Response.ok("Cập nhật sản phẩm thành công") : Response.fail("Cập nhật thất bại");
    }

    private Response handleProductDelete(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        int id = dataAsInt(request);
        boolean ok = productDAO.delete(id);
        if (ok) logDAO.log(currentUser.getUsername(), "DELETE_PRODUCT", "id=" + id, "Xoá sản phẩm", true, clientIp);
        return ok ? Response.ok("Xoá sản phẩm thành công") : Response.fail("Xoá thất bại");
    }

    // ======================== SUPPLIERS ========================
    private Response handleSupplierCreate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        Supplier s = gson.fromJson(gson.toJson(request.getData()), Supplier.class);
        if (s.getCode() == null || s.getCode().isBlank()) return Response.fail("Mã nhà cung cấp không được để trống");
        if (s.getName() == null || s.getName().isBlank()) return Response.fail("Tên nhà cung cấp không được để trống");
        boolean ok = supplierDAO.insert(s);
        if (ok) logDAO.log(currentUser.getUsername(), "CREATE_SUPPLIER", s.getCode(), s.getName(), true, clientIp);
        return ok ? Response.ok("Thêm nhà cung cấp thành công") : Response.fail("Mã nhà cung cấp đã tồn tại hoặc lỗi hệ thống");
    }

    private Response handleSupplierUpdate(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        Supplier s = gson.fromJson(gson.toJson(request.getData()), Supplier.class);
        boolean ok = supplierDAO.update(s);
        if (ok) logDAO.log(currentUser.getUsername(), "UPDATE_SUPPLIER", s.getCode(), s.getName(), true, clientIp);
        return ok ? Response.ok("Cập nhật nhà cung cấp thành công") : Response.fail("Cập nhật thất bại");
    }

    private Response handleSupplierDelete(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        int id = dataAsInt(request);
        boolean ok = supplierDAO.delete(id);
        if (ok) logDAO.log(currentUser.getUsername(), "DELETE_SUPPLIER", "id=" + id, "Xoá NCC", true, clientIp);
        return ok ? Response.ok("Xoá nhà cung cấp thành công") : Response.fail("Xoá thất bại");
    }

    // ======================== IMPORT ORDER ========================
    private Response handleImportCreate(Request request, User currentUser, String clientIp) {
        try {
            ImportOrder order = gson.fromJson(gson.toJson(request.getData()), ImportOrder.class);
            order.setUserId(currentUser.getId());
            if (order.getItems() == null || order.getItems().isEmpty()) return Response.fail("Phiếu nhập không có sản phẩm");
            boolean ok = importOrderDAO.insertWithTransaction(order);
            if (ok) logDAO.log(currentUser.getUsername(), "CREATE_IMPORT", order.getCode(), "Tạo phiếu nhập kho", true, clientIp);
            return ok ? Response.ok("Tạo phiếu nhập kho thành công") : Response.fail("Tạo phiếu nhập thất bại");
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    // ======================== EXPORT ORDER ========================
    private Response handleExportCreate(Request request, User currentUser, String clientIp) {
        try {
            ExportOrder order = gson.fromJson(gson.toJson(request.getData()), ExportOrder.class);
            order.setUserId(currentUser.getId());
            if (order.getItems() == null || order.getItems().isEmpty()) return Response.fail("Phiếu xuất không có sản phẩm");
            // Kiểm tra tồn kho
            for (var item : order.getItems()) {
                var product = productDAO.findById(item.getProductId());
                if (product == null) return Response.fail("Sản phẩm không tồn tại: " + item.getProductCode());
                if (product.getQuantity() < item.getQuantity()) {
                    return Response.fail("Tồn kho không đủ: " + product.getName() + " (còn " + product.getQuantity() + " " + product.getUnit() + ")");
                }
            }
            boolean ok = exportOrderDAO.insertWithTransaction(order);
            if (ok) logDAO.log(currentUser.getUsername(), "CREATE_EXPORT", order.getCode(), "Tạo phiếu xuất kho", true, clientIp);
            return ok ? Response.ok("Tạo phiếu xuất kho thành công") : Response.fail("Tạo phiếu xuất thất bại");
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    // ======================== FILE IMPORT/EXPORT ========================
    private Response handleExportProductsCsv(User currentUser, String clientIp) {
        try {
            List<Product> products = productDAO.findAll();
            String csv = CsvUtil.exportProducts(products);
            logDAO.log(currentUser.getUsername(), "EXPORT_CSV", "products", "Export " + products.size() + " sản phẩm", true, clientIp);
            return Response.ok("Export thành công " + products.size() + " sản phẩm", csv);
        } catch (Exception e) {
            return Response.fail("Lỗi export CSV: " + e.getMessage());
        }
    }

    private Response handleImportProductsCsv(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        try {
            String csvContent = dataAsString(request);
            List<Product> products = CsvUtil.importProducts(csvContent);
            int count = 0;
            for (Product p : products) {
                if (productDAO.findByCode(p.getCode()) == null) {
                    if (productDAO.insert(p)) count++;
                } else {
                    productDAO.update(p);
                    count++;
                }
            }
            logDAO.log(currentUser.getUsername(), "IMPORT_CSV", "products", "Import " + count + "/" + products.size() + " sản phẩm", true, clientIp);
            return Response.ok("Import thành công " + count + "/" + products.size() + " sản phẩm");
        } catch (Exception e) {
            return Response.fail("Lỗi import CSV: " + e.getMessage());
        }
    }

    private Response handleExportProductsXml(User currentUser, String clientIp) {
        try {
            List<Product> products = productDAO.findAll();
            String xml = XmlUtil.exportProducts(products);
            logDAO.log(currentUser.getUsername(), "EXPORT_XML", "products", "Export " + products.size() + " sản phẩm", true, clientIp);
            return Response.ok("Export XML thành công", xml);
        } catch (Exception e) {
            return Response.fail("Lỗi export XML: " + e.getMessage());
        }
    }

    private Response handleImportProductsXml(Request request, User currentUser, String clientIp) {
        if (!requireAdmin(currentUser)) return Response.fail("Không có quyền");
        try {
            String xmlContent = dataAsString(request);
            List<Product> products = XmlUtil.importProducts(xmlContent);
            int count = 0;
            for (Product p : products) {
                if (productDAO.findByCode(p.getCode()) == null) {
                    if (productDAO.insert(p)) count++;
                } else {
                    productDAO.update(p);
                    count++;
                }
            }
            logDAO.log(currentUser.getUsername(), "IMPORT_XML", "products", "Import " + count + "/" + products.size() + " sản phẩm", true, clientIp);
            return Response.ok("Import XML thành công " + count + "/" + products.size() + " sản phẩm");
        } catch (Exception e) {
            return Response.fail("Lỗi import XML: " + e.getMessage());
        }
    }

    // ======================== HELPERS ========================
    private boolean requireAdmin(User user) {
        return user != null && user.isAdmin();
    }

    private String dataAsString(Request request) {
        return request.getData() != null ? request.getData().toString() : "";
    }

    private int dataAsInt(Request request) {
        if (request.getData() == null) return 0;
        try {
            return ((Number) request.getData()).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}