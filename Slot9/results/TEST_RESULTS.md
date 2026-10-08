# Slot9 Part 5 — Kết quả

Swagger đã hoạt động tại **http://localhost:9000/swagger-ui.html**, dropdown đủ Product Service, Order Service và Inventory Service. Assignment1 đã được tắt; Slot9 dùng đúng cổng 8080, 8081, 8082, 9000 và Keycloak 8181.

- **25 test pass**, 0 failure, 0 error; gồm 14 test Swagger/CORS/security và các test Part 1–4.
- Build ban đầu của cả 4 project thành công.
- 11 kiểm tra trên ứng dụng thật đã pass: docs trực tiếp/tổng hợp, UI, dropdown, CORS, API không token trả 401 và JWT Keycloak thật gọi API thành công.

| Project | Tests | Kết quả |
|---|---:|---|
| product-service | 5 | PASS |
| inventory-service | 6 | PASS |
| order-service | 5 | PASS |
| api-gateway | 9 | PASS |

Kết quả chi tiết: [test-summary.json](test-summary.json), XML JUnit trong junit/, [swagger-live-results.json](swagger-live-results.json) và các OpenAPI JSON trong thư mục này.

Đã sửa hai vấn đề test: Testcontainers 1.19.3 của Inventory không hỗ trợ MySQL 8.3 nên dùng 1.21.4; file properties riêng của test Order cần khai báo lại /api-docs. Bước verify thử khi service đang chạy bị Windows khóa JAR ở repackage; các test còn lại đã chạy bằng Maven test và đều pass.
