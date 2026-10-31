# Kết quả kiểm tra Slot16

Đã build cả 3 project bằng JDK 21 với Spring Boot 4.1.0, Spring Cloud 2025.1.2 và springdoc 3.0.3.

- 11 test tự động đạt: 2 test validation UserDTO và 9 test nghiệp vụ PostService.
- 81 lượt kiểm tra trực tiếp đạt qua HTTP với SQL Server thật: CRUD, PUT một phần, validation, mã HTTP/status, phân trang/lọc, OpenFeign, giá hiện tại, định dạng ngày/timestamp, soft delete và lỗi service không khả dụng.
- Swagger UI, /v3/api-docs và CORS đạt trên cổng 8080, 8081, 8082; gateway có đủ hai định nghĩa API.
- Đã chạy 3 script SQL trên database tạm mới: tạo database, hai bảng đúng script đề, constraints và seed có 2 user ACTIVE. Database tạm đã xóa sau kiểm tra.
- Database MSS301_2026_PE_MEDIA trên máy đã có sẵn hai bảng đúng cấu trúc và ban đầu rỗng. Đã thêm 3 user seed; dữ liệu tạo riêng để test đã được dọn.
- Cả hai service giữ spring.jpa.hibernate.ddl-auto=none. Chạy được với database tạo bằng SQL, không dựa vào Hibernate tạo bảng.

Chi tiết 81 lượt HTTP: test-evidence/live-checks.txt. Log kiểm tra nằm ngoài thư mục source nộp bài.

Đã dừng các tiến trình kiểm tra và chạy Maven clean ở cả 3 project. Không tạo ZIP theo yêu cầu.
