-- ================================================================
-- QUẢN LÝ KHO HÀNG CỬA HÀNG LINH KIỆN MÁY TÍNH
-- Database Schema + Sample Data
-- ================================================================

CREATE DATABASE IF NOT EXISTS warehouse_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE warehouse_db;

-- ================================================================
-- 1. BẢNG USERS (Tài khoản người dùng)
-- ================================================================
CREATE TABLE IF NOT EXISTS users (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,  -- BCrypt hash
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255),           -- AES encrypted
    phone       VARCHAR(255),           -- AES encrypted
    role        ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    status      ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    fail_count  INT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ================================================================
-- 2. BẢNG CATEGORIES (Danh mục linh kiện)
-- ================================================================
CREATE TABLE IF NOT EXISTS categories (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- 3. BẢNG PRODUCTS (Sản phẩm / Linh kiện)
-- ================================================================
CREATE TABLE IF NOT EXISTS products (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    code          VARCHAR(50)     NOT NULL UNIQUE,
    name          VARCHAR(255)    NOT NULL,
    category_id   INT             NOT NULL,
    brand         VARCHAR(100),
    unit          VARCHAR(30)     NOT NULL DEFAULT 'Cái',
    cost_price    DECIMAL(15,2)   NOT NULL DEFAULT 0,
    sell_price    DECIMAL(15,2)   NOT NULL DEFAULT 0,
    quantity      INT             NOT NULL DEFAULT 0,
    min_quantity  INT             NOT NULL DEFAULT 5,
    description   TEXT,
    status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- ================================================================
-- 4. BẢNG SUPPLIERS (Nhà cung cấp)
-- ================================================================
CREATE TABLE IF NOT EXISTS suppliers (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    contact     VARCHAR(100),
    phone       VARCHAR(255),   -- AES encrypted
    email       VARCHAR(255),   -- AES encrypted
    address     VARCHAR(500),
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ================================================================
-- 5. BẢNG IMPORT_ORDERS (Phiếu nhập kho)
-- ================================================================
CREATE TABLE IF NOT EXISTS import_orders (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(30)   NOT NULL UNIQUE,
    supplier_id     INT           NOT NULL,
    user_id         INT           NOT NULL,
    total_amount    DECIMAL(15,2) NOT NULL DEFAULT 0,
    note            TEXT,
    status          ENUM('PENDING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    import_date     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ================================================================
-- 6. BẢNG IMPORT_ORDER_ITEMS (Chi tiết phiếu nhập)
-- ================================================================
CREATE TABLE IF NOT EXISTS import_order_items (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    import_order_id INT           NOT NULL,
    product_id      INT           NOT NULL,
    quantity        INT           NOT NULL,
    unit_price      DECIMAL(15,2) NOT NULL,
    total_price     DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (import_order_id) REFERENCES import_orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ================================================================
-- 7. BẢNG EXPORT_ORDERS (Phiếu xuất kho)
-- ================================================================
CREATE TABLE IF NOT EXISTS export_orders (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(30)   NOT NULL UNIQUE,
    customer_name   VARCHAR(150),
    customer_phone  VARCHAR(255),   -- AES encrypted
    user_id         INT           NOT NULL,
    total_amount    DECIMAL(15,2) NOT NULL DEFAULT 0,
    note            TEXT,
    status          ENUM('PENDING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    export_date     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ================================================================
-- 8. BẢNG EXPORT_ORDER_ITEMS (Chi tiết phiếu xuất)
-- ================================================================
CREATE TABLE IF NOT EXISTS export_order_items (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    export_order_id INT           NOT NULL,
    product_id      INT           NOT NULL,
    quantity        INT           NOT NULL,
    unit_price      DECIMAL(15,2) NOT NULL,
    total_price     DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (export_order_id) REFERENCES export_orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ================================================================
-- 9. BẢNG SYSTEM_LOGS (Log hệ thống)
-- ================================================================
CREATE TABLE IF NOT EXISTS system_logs (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50),
    action      VARCHAR(100) NOT NULL,
    target      VARCHAR(100),
    detail      TEXT,
    status      ENUM('SUCCESS','FAIL') NOT NULL DEFAULT 'SUCCESS',
    ip_address  VARCHAR(50),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- DỮ LIỆU MẪU
-- ================================================================

-- Users (password: Admin@123 và User@123 - BCrypt hash)
INSERT INTO users (username, password, full_name, email, phone, role, status) VALUES
('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4oVWpFQl1S', 'Nguyễn Văn Admin', 'YWRtaW5AZXhhbXBsZS5jb20=', 'MDkwMTIzNDU2Nzg=', 'ADMIN', 'ACTIVE'),
('user01', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Trần Thị Bán Hàng', 'dXNlcjAxQGV4YW1wbGUuY29t', 'MDkwOTg3NjU0MzI=', 'USER', 'ACTIVE'),
('user02', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Lê Văn Kho', 'dXNlcjAyQGV4YW1wbGUuY29t', 'MDkwMTExMjIzMzQ=', 'USER', 'ACTIVE');

-- Categories
INSERT INTO categories (code, name, description) VALUES
('CPU', 'Bộ vi xử lý', 'CPU Intel, AMD các thế hệ'),
('RAM', 'Bộ nhớ RAM', 'RAM DDR4, DDR5 các hãng'),
('SSD', 'Ổ cứng SSD', 'SSD NVMe, SATA các dung lượng'),
('HDD', 'Ổ cứng HDD', 'HDD 2.5 inch, 3.5 inch'),
('VGA', 'Card đồ họa', 'GPU NVIDIA, AMD'),
('MB', 'Bo mạch chủ', 'Mainboard Intel, AMD platform'),
('PSU', 'Nguồn máy tính', 'PSU 80+ Bronze, Gold, Platinum'),
('CASE', 'Vỏ máy tính', 'Case ATX, mATX, ITX'),
('COOL', 'Tản nhiệt', 'Tản nhiệt khí, tản nhiệt nước'),
('NET', 'Thiết bị mạng', 'Card mạng, USB Wifi, Switch');

-- Suppliers
INSERT INTO suppliers (code, name, contact, phone, email, address) VALUES
('SUP001', 'Công ty TNHH Phân phối Linh kiện Tân Phát', 'Nguyễn Tân Phát', 'MDI4MTIzNDU2Nzg=', 'dGFucGhhdEBleGFtcGxlLmNvbQ==', '123 Nguyễn Văn Linh, Q.7, TP.HCM'),
('SUP002', 'Công ty CP Công nghệ Tiến Đạt', 'Trần Tiến Đạt', 'MDI4OTg3NjU0MzI=', 'dGllbmRhdEBleGFtcGxlLmNvbQ==', '456 Lê Văn Việt, Q.9, TP.HCM'),
('SUP003', 'Nhà phân phối FPT Trading', 'Lê Minh Tuấn', 'MDI4MTExMjIzMzQ=', 'ZnB0dHJhZGluZ0BleGFtcGxlLmNvbQ==', '789 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM'),
('SUP004', 'Công ty TNHH Intel Việt Nam', 'Phạm Văn Intel', 'MDI4NTU1NjY3Nzg=', 'aW50ZWx2bkBleGFtcGxlLmNvbQ==', '321 Võ Văn Tần, Q.3, TP.HCM');

-- Products
INSERT INTO products (code, name, category_id, brand, unit, cost_price, sell_price, quantity, min_quantity, description) VALUES
('CPU001', 'Intel Core i5-13400F', 1, 'Intel', 'Cái', 3800000, 4200000, 15, 5, 'CPU Intel Core i5-13400F, 10 nhân 16 luồng, Socket LGA1700'),
('CPU002', 'Intel Core i7-13700K', 1, 'Intel', 'Cái', 8500000, 9200000, 8, 3, 'CPU Intel Core i7-13700K, 16 nhân 24 luồng, Socket LGA1700'),
('CPU003', 'AMD Ryzen 5 7600X', 1, 'AMD', 'Cái', 5200000, 5800000, 12, 5, 'CPU AMD Ryzen 5 7600X, 6 nhân 12 luồng, Socket AM5'),
('CPU004', 'AMD Ryzen 9 7950X', 1, 'AMD', 'Cái', 15000000, 16500000, 4, 2, 'CPU AMD Ryzen 9 7950X, 16 nhân 32 luồng, Socket AM5'),
('RAM001', 'Kingston Fury Beast 16GB DDR4 3200', 2, 'Kingston', 'Thanh', 850000, 980000, 30, 10, 'RAM DDR4 16GB 3200MHz, CL16'),
('RAM002', 'Corsair Vengeance 32GB DDR4 3600', 2, 'Corsair', 'Thanh', 1800000, 2100000, 20, 8, 'RAM DDR4 32GB (2x16GB) 3600MHz'),
('RAM003', 'G.Skill Trident Z5 32GB DDR5 6000', 2, 'G.Skill', 'Thanh', 3200000, 3700000, 10, 4, 'RAM DDR5 32GB 6000MHz CL36'),
('SSD001', 'Samsung 980 Pro 1TB NVMe', 3, 'Samsung', 'Cái', 2100000, 2400000, 25, 8, 'SSD NVMe PCIe 4.0 1TB, Đọc 7000MB/s'),
('SSD002', 'WD Black SN850X 2TB', 3, 'WD', 'Cái', 3800000, 4300000, 15, 5, 'SSD NVMe PCIe 4.0 2TB'),
('SSD003', 'Crucial MX500 500GB SATA', 3, 'Crucial', 'Cái', 950000, 1100000, 35, 10, 'SSD SATA 2.5" 500GB'),
('VGA001', 'RTX 4060 8GB GDDR6', 5, 'NVIDIA', 'Cái', 8500000, 9500000, 10, 3, 'Card đồ họa NVIDIA GeForce RTX 4060 8GB'),
('VGA002', 'RX 7600 8GB GDDR6', 5, 'AMD', 'Cái', 7200000, 8000000, 8, 3, 'Card đồ họa AMD Radeon RX 7600 8GB'),
('MB001', 'ASUS ROG STRIX B660-F Gaming', 6, 'ASUS', 'Cái', 4500000, 5000000, 12, 4, 'Bo mạch chủ Intel B660, Socket LGA1700, DDR5'),
('MB002', 'MSI MAG X670E Tomahawk', 6, 'MSI', 'Cái', 6800000, 7500000, 8, 3, 'Bo mạch chủ AMD X670E, Socket AM5, DDR5'),
('PSU001', 'Corsair RM750x 750W 80+ Gold', 7, 'Corsair', 'Cái', 2800000, 3200000, 15, 5, 'Nguồn máy tính 750W 80+ Gold, Full Modular'),
('PSU002', 'Seasonic Focus GX-850 850W', 7, 'Seasonic', 'Cái', 3500000, 3900000, 10, 4, 'Nguồn 850W 80+ Gold, 10 năm bảo hành'),
('HDD001', 'Seagate Barracuda 2TB 3.5"', 4, 'Seagate', 'Cái', 1200000, 1400000, 20, 8, 'HDD 3.5" 2TB 7200RPM SATA III'),
('CASE001', 'Lian Li Lancool 216', 8, 'Lian Li', 'Cái', 1800000, 2100000, 8, 3, 'Case ATX Mid-Tower, 2 quạt 160mm'),
('COOL001', 'Noctua NH-D15', 9, 'Noctua', 'Cái', 1700000, 2000000, 10, 4, 'Tản nhiệt khí cao cấp, 2 quạt 140mm'),
('NET001', 'TP-Link Archer T3U Plus USB Wifi', 10, 'TP-Link', 'Cái', 350000, 420000, 25, 8, 'USB Wifi AC1300 Dual Band');
