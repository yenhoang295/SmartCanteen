# Smart Canteen

Website đặt món & quản lý canteen. Backend: Java (built-in `HttpServer`) + JDBC.
Frontend: HTML/CSS/JS thuần. Database: SQL Server.

## 1. Chuẩn bị SQL Server

1. Mở **SQL Server Management Studio** (hoặc Azure Data Studio), kết nối vào SQL Server bằng
   **SQL Server Authentication** (user/password bạn đang dùng).
2. Chạy lần lượt 2 file:
   - `sql/schema.sql` → tạo database `SmartCanteen` và toàn bộ bảng.
   - `sql/seed.sql` → thêm dữ liệu mẫu (tài khoản admin/staff, món ăn, nguyên liệu).
3. Tài khoản demo sau khi seed (mật khẩu: `123456`):
   - Admin: `admin@canteen.edu.vn`
   - Staff: `staff@canteen.edu.vn`

> Nếu SQL Server chưa bật TCP/IP hoặc chưa bật SQL Authentication, vào
> **SQL Server Configuration Manager** bật TCP/IP (cổng 1433), và trong SSMS
> vào **Server Properties → Security → SQL Server and Windows Authentication mode**.

## 2. Cấu hình kết nối

Mở file `src/main/resources/db.properties` và sửa:

```
db.host=localhost
db.port=1433
db.name=SmartCanteen
db.user=sa
db.password=MẬT_KHẨU_CỦA_BẠN
server.port=8080
```

## 3. Mở project bằng VS Code

1. Cài extension **Extension Pack for Java** trong VS Code.
2. Mở thư mục `smart-canteen` bằng `File > Open Folder`.
3. VS Code sẽ tự nhận project Maven. Nếu cần, mở Terminal và chạy:
   ```bash
   mvn clean package
   ```

## 4. Chạy server

Cách 1 — trong VS Code: mở `src/main/java/com/smartcanteen/Main.java`, bấm **Run**.

Cách 2 — dùng terminal:
```bash
mvn clean package
java -jar target/smart-canteen-jar-with-dependencies.jar
```

Server chạy tại: **http://localhost:8080**

## 5. Cấu trúc trang web

| Đường dẫn | Vai trò |
|---|---|
| `/index.html` | Đăng nhập / đăng ký (sinh viên, giảng viên, khách OTP) |
| `/menu.html` | Xem & tìm kiếm menu |
| `/cart.html` | Giỏ hàng, chọn giờ nhận, thanh toán demo |
| `/orders.html` | Theo dõi đơn, hủy đơn (trong 5 phút), thanh toán |
| `/staff/dashboard.html` | Nhân viên: kiểm tra nguyên liệu, nhận/từ chối đơn, xác nhận mã nhận hàng |
| `/admin/dashboard.html` | Admin: CRUD món, quản lý nguyên liệu, báo cáo doanh thu |

## 6. Kiến trúc

```
Trình duyệt (HTML/CSS/JS)
        ↓ fetch() gọi REST API
Java HttpServer (Main.java)
        ↓
Handler (Auth/Menu/Order/Staff/Admin)
        ↓
DAO (JDBC)
        ↓
SQL Server (SmartCanteen)
```

## 7. Đã làm và chưa làm

**Đã có:** đăng ký/đăng nhập 3 vai trò khách + OTP demo, menu & tìm kiếm, giỏ hàng,
chọn khung giờ, đặt hàng + mã đơn/mã nhận hàng, thanh toán demo (MoMo/QR),
theo dõi đơn, hủy trong 5 phút, staff kiểm tra nguyên liệu trước khi nhận đơn,
từ chối kèm lý do, xác nhận mã nhận hàng, admin CRUD món + nguyên liệu + báo cáo doanh thu/món bán chạy.

**Đơn giản hoá / có thể làm thêm sau:** combo & khuyến mãi (đã có bảng DB nhưng
chưa có UI quản lý), đánh giá món (đã có bảng `Ratings`, chưa có UI), UI trang quản lý
người dùng cho admin (API đã có ở `/api/admin/users`), thanh toán MoMo/QR thật.
