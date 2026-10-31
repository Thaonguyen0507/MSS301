# MSS301 PE Sample DE190669

Ba project độc lập dùng JDK 21, Spring Boot 4.1.0. PostService và Gateway dùng Spring Cloud 2025.1.2; Swagger dùng springdoc 3.0.3.

## 1. Chuẩn bị SQL Server

SQL Server ở localhost:1433, tài khoản sa, mật khẩu sa theo đề. Mở SSMS và chạy lần lượt:

1. sql/01_create_database.sql
2. sql/02_create_tables.sql (chỉ chạy khi chưa có hai bảng)
3. sql/03_seed_users.sql

Database: MSS301_2026_PE_MEDIA. Tên bảng, cột, kiểu và ràng buộc giữ nguyên script đề.
Hai service đều đặt spring.jpa.hibernate.ddl-auto=none; entity ánh xạ vào dbo.users và dbo.posts, Hibernate không tạo bảng.
Seed có creator_a, creator_b ACTIVE và creator_c SUSPENDED; script seed chạy lại không trùng username.

## 2. Chạy ứng dụng

Mở từng project bằng IntelliJ, chọn SDK 21, Load Maven và chạy main class tương ứng.
Hoặc mở 3 terminal PowerShell, mỗi terminal vào một thư mục project rồi chạy:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\mvnw.cmd spring-boot:run
```

Thứ tự: DE190669UserService (8081), DE190669PostService (8082), DE190669MediaGateway (8080).

Swagger chung: http://localhost:8080/swagger-ui.html — chọn user-service hoặc post-service.
Swagger riêng: http://localhost:8081/swagger-ui.html và http://localhost:8082/swagger-ui.html.
OpenAPI của mỗi project: /v3/api-docs. Swagger gửi các lệnh API qua gateway 8080.

## 3. Kiểm tra nhanh

Dùng requests.http trong IntelliJ hoặc Swagger. Mẫu tạo post từ 25/07/2026 đến 28/07/2026 với rate 500000 cho totalCost 1500000; numberOfPosts không nhân vào totalCost.
PUT chỉ thay các field được gửi; totalCost luôn tính lại từ giá hiện tại của user. Điều kiện user ACTIVE áp dụng khi tạo campaign theo đề.
DELETE user đặt DEACTIVATED; DELETE post đặt CANCELLED, không xóa dòng.

HTTP/status theo đề: 201 hoặc 200/1 thành công, 406/2 validation, 226/3 trùng username, 404/4 không tìm thấy, 400/5 user không ACTIVE khi tạo campaign, 500/0 lỗi server.
Ngày dùng dd/MM/yyyy; timestamp ISO 8601 UTC. Danh sách phân trang page=0, size=10, tối đa 100.

## 4. Build và nộp bài

Trong từng project chạy .\mvnw.cmd test để kiểm tra, hoặc .\mvnw.cmd package để build JAR.
Trước khi nộp chạy .\mvnw.cmd clean trong cả 3 project rồi nén thư mục DE190669. Tên ZIP bắt đầu bằng DE190669; không đưa target vào bài nộp.
