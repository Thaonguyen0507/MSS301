# FUCinemaBookingSystem

Backend của Assignment 01, gồm ba service độc lập và API Gateway. Giữ Controller – Service – Repository, DTO record, Bean Validation; không thêm discovery server, message broker hay framework ngoài yêu cầu.

| Ứng dụng | Cổng | Database |
|---|---:|---|
| API Gateway | 9000 | — |
| customer-service | 8081 | SQL Server 2022 / cinema_customer |
| movie-service | 8082 | MongoDB 7 / cinema_movie |
| booking-service | 8083 | MySQL 8 / cinema_booking |

SQL Server Docker dùng **cổng host 14330**, vì máy hiện có SQL Server dùng 1433. Trong container vẫn là 1433. MongoDB: 27017; MySQL: 3306.

## Chạy trên Windows

Cần Java 21 trở lên, Docker Desktop (Linux containers), Maven hoặc Maven Wrapper, Node.js cho Newman.

Chạy từ thư mục Assignment1:

~~~powershell
# Nếu JAVA_HOME chưa có, script tự dùng C:/Program Files/Java/jdk-21 khi có.
./scripts/build.ps1
./scripts/start.ps1
npm ci
npm run test:api
~~~

Nếu Maven chỉ có trong IntelliJ:

~~~powershell
./scripts/build.ps1 -Maven 'C:/Program Files/JetBrains/IntelliJ IDEA 2025.1.1.1/plugins/maven/lib/maven3/bin/mvn.cmd'
~~~

Script start chạy docker compose và init database, mở các Java process ẩn, ghi PID/log vào results/, rồi kiểm tra login và API qua Gateway. Dừng các Java process của bài bằng ./scripts/stop.ps1. Dừng container bằng docker compose stop. Các script không xóa dữ liệu database.

Có thể chạy từng service trong IDE hoặc java -Duser.timezone=Asia/Ho_Chi_Minh -jar <service>/target/<service>-0.0.1-SNAPSHOT.jar. Dùng cùng múi giờ cho ba service.

## Tài khoản mẫu

| Vai trò | Email | Mật khẩu |
|---|---|---|
| ADMIN | admin@fucinema.com | @@abc123@@ |
| CUSTOMER | an@gmail.com | 123456 |
| CUSTOMER | binh@gmail.com | 123456 |
| INACTIVE | chi@gmail.com | 123456 |

Admin đọc từ properties; customer dùng BCrypt. Password không có trong response. JWT HS256 có sub, uid, role, iat, exp. Gateway kiểm tra role và xóa mọi header X-User-* của client trước khi chèn context từ JWT.

Đây là cấu hình local của bài tập; database credentials và JWT secret dùng chung đều nằm trong properties/Compose để dễ chạy.

## Postman

Import hai file trong postman/ vào Postman, chọn FUCinema-Local rồi chạy toàn bộ collection từ trên xuống bằng Collection Runner. Bộ kiểm thử cũng chạy bằng Newman, dùng đúng collection và assertions của Postman.

Collection tạo email/tên mới bằng biến động và tạo lịch chiếu sau 7 ngày để chạy lại trên dữ liệu hiện có. Không đổi mật khẩu của tài khoản seed. Các booking thử được lưu lại trong DB; report kiểm tra tổng từ dữ liệu trả về và loại booking đã hủy, không giả định DB rỗng.

Các kiểm tra bổ sung có thể chạy lại:

~~~powershell
node scripts/verify-extra.mjs concurrency
./scripts/test-outage.ps1
./scripts/verify-databases.ps1
node scripts/report.mjs
~~~

Chỉ chạy collection BR14 khi movie-service đang được dừng; script test-outage tự dừng và phục hồi service.

Có sửa hai điểm không nhất quán trong hướng dẫn: kiểm tra xóa genre đang có phim dùng genre khoa học viễn tưởng (genre hành động trong seed chưa có phim); kiểm tra cùng giờ khác phòng dùng phòng mới mỗi lượt để tránh trùng lịch của lượt cũ.

## Lưu trữ và nghiệp vụ

- SQL Server: Flyway V1 tạo schema, V2 nạp ba customer bằng NVARCHAR và chuỗi Unicode N'...'.
- MongoDB: bốn collection, ObjectId cố định cho seed, index unique tên genre/phòng, ticketPrice Decimal128. Seeder chỉ nạp từng collection khi collection đó rỗng.
- MySQL: Flyway V1 tạo booking và details; V2 thêm unique ghế còn hiệu lực. Một transaction ghi toàn bộ booking; khi hủy, lịch sử giữ nguyên và ghế được giải phóng. Unique index bảo vệ cả request đồng thời.
- Booking gọi Movie trực tiếp bằng OpenFeign, snapshot thông tin phim/phòng/giờ chiếu; lỗi kết nối trả 503. Giá do server tính.
- Report lọc booking CONFIRMED trong khoảng [startDate 00:00, endDate + 1 ngày 00:00), bookings và doanh thu theo phim giảm dần.

## Kết quả kiểm thử

Kết quả đã đạt: **110 request Postman / 225 assertion, 0 lỗi**, 4/4 Spring context pass; các kiểm tra đặt vé đồng thời, lỗi 503 và restart seed đều pass.

Các kết quả chạy thực tế nằm trong results/: báo cáo Newman JSON/JUnit, log build và Spring context, kiểm tra database và các ca bổ sung. Xem results/TEST_RESULTS.md sau khi hoàn tất kiểm thử.

Chạy Spring context test khi database đang chạy:

~~~powershell
mvn -f pom.xml test
~~~

Tham khảo API Gateway MVC: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/gateway-handler-filter-functions.html
