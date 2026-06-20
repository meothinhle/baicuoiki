# Phần mềm Quản lý Kho hàng - Cửa hàng Linh kiện Máy tính

Mô hình **Java Desktop Client/Server** (Swing + TCP Socket + MySQL), gồm 2 module Maven độc lập:
- `server-app`  : Server (TCP Socket, JDBC, xử lý nghiệp vụ, BCrypt + AES)
- `client-app`  : Client (Swing UI, kết nối Server qua Socket)

---

## 1. YÊU CẦU MÔI TRƯỜNG

| Thành phần | Phiên bản tối thiểu |
|---|---|
| JDK | 17 trở lên (khuyến nghị 17 hoặc 21) |
| MySQL Server | 8.0 trở lên |
| Maven | 3.8 trở lên (IntelliJ có sẵn Maven, không bắt buộc cài riêng) |
| IntelliJ IDEA | Community hoặc Ultimate |

---

## 2. CÀI ĐẶT DATABASE

1. Mở **MySQL Workbench** hoặc command line MySQL.
2. Chạy toàn bộ script trong file: `database/warehouse_db.sql`
    - Script này sẽ tự tạo database `warehouse_db`, toàn bộ bảng, và dữ liệu mẫu.
3. Kiểm tra lại thông tin kết nối tại:
   `server-app/src/main/resources/config.properties`
   ```properties
   database.url=jdbc:mysql://localhost:3306/warehouse_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
   database.username=root
   database.password=        <-- Sửa lại mật khẩu MySQL của bạn tại đây
   ```

---

## 3. MỞ DỰ ÁN TRONG INTELLIJ IDEA

Có 2 cách:

### Cách A — Mở từng module riêng (khuyến nghị để chạy nhanh)
1. `File > Open` → chọn thư mục `server-app` → Open as Maven project (IntelliJ tự nhận `pom.xml`)
2. `File > Open` (cửa sổ mới) → chọn thư mục `client-app` → tương tự

### Cách B — Mở cả 2 module trong cùng 1 cửa sổ
1. `File > Open` → chọn thư mục gốc `warehouse-management`
2. Vào `File > Project Structure > Modules > +` → Import Module → trỏ tới `server-app/pom.xml` và `client-app/pom.xml`
3. IntelliJ sẽ tự tải dependency qua Maven (cần Internet để tải lần đầu)

Sau khi mở, đợi thanh trạng thái dưới cùng hiển thị "Indexing..." hoàn tất và Maven tải xong dependencies (góc phải dưới có icon Maven, bấm Reload nếu cần).

---

## 4. CHẠY SERVER TRƯỚC

1. Mở file: `server-app/src/main/java/com/warehouse/server/MainServer.java`
2. Click chuột phải vào file → **Run 'MainServer.main()'**
3. Console sẽ hiển thị:
   ```
   ✓ Kết nối database thành công
   ===========================================
     WAREHOUSE SERVER đang chạy trên cổng 9000
     ThreadPool: 10 threads
     Nhấn Ctrl+C để dừng
   ===========================================
   ```
4. Giữ nguyên cửa sổ Server đang chạy — không tắt cho đến khi dùng xong Client.

> Nếu báo lỗi "KHÔNG THỂ KẾT NỐI DATABASE": kiểm tra lại MySQL đã chạy chưa, và username/password trong `config.properties` đã đúng chưa.

---

## 5. CHẠY CLIENT

1. Mở file: `client-app/src/main/java/com/warehouse/client/MainClient.java`
2. Click chuột phải → **Run 'MainClient.main()'**
3. Cửa sổ đăng nhập sẽ hiện ra.
4. Đăng nhập với tài khoản mẫu có sẵn:

| Username | Password | Vai trò |
|---|---|---|
| admin | Admin@123 | Quản trị viên (toàn quyền) |
| user01 | User@123 | Nhân viên |
| user02 | User@123 | Nhân viên |

Bạn có thể mở nhiều Client cùng lúc (Run nhiều lần MainClient) để mô phỏng nhiều máy trạm kết nối đồng thời vào 1 Server.

---

## 6. CẤU HÌNH KẾT NỐI CLIENT → SERVER (nếu cần đổi)

Nếu Server chạy trên máy khác (không phải localhost), sửa file:
`client-app/src/main/resources/client.properties`
```properties
server.host=localhost
server.port=9000
```

---

## 7. CHỨC NĂNG CHÍNH

- Đăng nhập / Phân quyền: ADMIN (toàn quyền) và USER (giới hạn xem + tạo phiếu)
- Dashboard: thống kê tổng quan, cảnh báo tồn kho thấp
- Quản lý Danh mục: CRUD danh mục linh kiện
- Quản lý Sản phẩm: CRUD sản phẩm, Import/Export CSV
- Quản lý Nhà cung cấp: CRUD nhà cung cấp
- Phiếu Nhập kho: tạo phiếu nhập (transaction: tăng tồn kho)
- Phiếu Xuất kho: tạo phiếu xuất (transaction: kiểm tra & trừ tồn kho)
- Quản lý tài khoản (Admin): tạo tài khoản, khoá tài khoản, đặt lại mật khẩu
- Nhật ký hệ thống (Admin): log mọi thao tác quan trọng

## 8. BẢO MẬT

- Mật khẩu: hash bằng BCrypt (1 chiều, không thể giải mã)
- Email / SĐT / Địa chỉ: mã hoá AES (2 chiều, có thể giải mã khi hiển thị)
- Khoá tài khoản tự động sau 5 lần đăng nhập sai
- Giao tiếp Client–Server qua TCP Socket, JSON (Gson), mỗi client xử lý trên 1 thread riêng (ThreadPool 10 threads)

---

## 9. CẤU TRÚC THƯ MỤC

```
warehouse-management/
├── database/
│   └── warehouse_db.sql        (Script tạo DB + dữ liệu mẫu)
├── server-app/
│   ├── pom.xml
│   └── src/main/java/com/warehouse/
│       ├── server/    (MainServer, ClientHandler, RequestHandler)
│       ├── service/   (AuthService)
│       ├── dao/        (các lớp truy xuất DB)
│       ├── model/      (các entity)
│       ├── security/   (PasswordUtil - BCrypt, AESUtil - AES)
│       └── util/        (CsvUtil, XmlUtil)
└── client-app/
    ├── pom.xml
    └── src/main/java/com/warehouse/
        ├── client/   (MainClient - entry point)
        ├── ui/        (LoginFrame, MainFrame, panels/*)
        ├── service/   (ServerConnection, ApiClient, ClientSession)
        └── util/       (UITheme, FormatUtil)
```

---

## 10. XỬ LÝ SỰ CỐ THƯỜNG GẶP

| Lỗi | Nguyên nhân và cách khắc phục |
|---|---|
| Không thể kết nối server (khi đăng nhập) | Chưa chạy MainServer, hoặc sai client.properties |
| KHÔNG THỂ KẾT NỐI DATABASE | MySQL chưa chạy, sai user/pass trong config.properties, hoặc chưa import warehouse_db.sql |
| Maven không tải được dependency | Kiểm tra kết nối Internet, hoặc File > Invalidate Caches / Restart trong IntelliJ |
| Lỗi font tiếng Việt hiển thị ô vuông | Đổi font hệ thống IDE / chọn font hỗ trợ Unicode (Segoe UI có sẵn trên Windows) |
| Đăng nhập đúng mật khẩu vẫn báo "Sai mật khẩu" / "Read timed out" | Đã sửa ở bản cập nhật này (bỏ socket timeout 15s/30s gây rớt kết nối khi Client idle). Nếu vẫn còn báo "Sai mật khẩu" do lỗi cũ để lại, chạy file `database/reset_fail_count.sql` để mở khoá / reset lại `fail_count` cho toàn bộ tài khoản |