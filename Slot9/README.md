# Slot9 — Part 5: Swagger / OpenAPI

Đã thêm Swagger cho Product, Inventory, Order và API Gateway theo DOC-1 → DOC-16.

## Chạy

Cần Java 21, Docker Desktop và Maven/Maven Wrapper. Chạy từ Slot9:

~~~powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
./product-service/mvnw.cmd -f pom.xml verify
./scripts/start.ps1
~~~

Nếu Assignment1 đang chạy, dừng các ứng dụng bằng ../Assignment1/scripts/stop.ps1 và database bằng docker compose -f ../Assignment1/docker-compose.yml stop để giải phóng cổng.

| Service | Swagger UI | JSON docs |
|---|---|---|
| Product | http://localhost:8080/swagger-ui.html | http://localhost:8080/api-docs |
| Order | http://localhost:8081/swagger-ui.html | http://localhost:8081/api-docs |
| Inventory | http://localhost:8082/swagger-ui.html | http://localhost:8082/api-docs |
| Gateway | **http://localhost:9000/swagger-ui.html** | http://localhost:9000/v3/api-docs |

Swagger UI trên Gateway có dropdown **Product Service**, **Order Service**, **Inventory Service**. Docs/UI truy cập tự do; API nghiệp vụ vẫn yêu cầu JWT.

## Thử API bằng Swagger

1. Mở Swagger UI của Gateway, chọn service trong dropdown.
2. Chạy ./scripts/get-token.ps1 để lấy JWT từ Keycloak đang chạy tại cổng 8181. Script đọc client credentials từ realm JSON có sẵn.
3. Nhấn **Authorize**, dán token (chỉ token, không thêm chữ Bearer).
4. Mở GET /api/product → **Try it out** → **Execute**. Không có token thì Gateway trả 401.

OpenAPI khai báo bearerAuth và server URL tương đối /, để các request Try it out từ Swagger UI Gateway đi qua chính Gateway.

## Kiểm thử

~~~powershell
# Test tất cả Part 1–5 bằng Testcontainers / WireMock
./product-service/mvnw.cmd -f pom.xml test
# Kiểm tra docs/UI/aggregate/CORS và JWT thật khi ứng dụng đang chạy
node scripts/verify-swagger.mjs
# Dừng Java process do script start tạo
./scripts/stop.ps1
~~~

Các service có SwaggerIntegrationTest kiểm tra UI, title/version, endpoint, JWT scheme và CORS. Gateway có SwaggerSecurityTest kiểm tra UI/docs public, rewrite aggregate path và API vẫn cần token. Các test Part 1–4 được giữ lại và chạy cùng.

## Dependency

Product/Inventory đang dùng Boot 3.2.4 nên dùng Springdoc 2.5.0 theo hướng dẫn. Order/Gateway đang dùng Boot 4.1.0 nên dùng Springdoc 3.1.1, tương thích Boot 4. Các phiên bản Spring Boot có sẵn được giữ nguyên.

Tham khảo tương thích: https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot

**25 test đã pass (14 test Swagger/CORS/security), không có failure/error.**

Kết quả chạy thực tế và các JSON OpenAPI được lưu trong results/. Xem results/TEST_RESULTS.md.

Inventory Service nâng Testcontainers từ 1.19.3 lên 1.21.4 để sửa cấu hình skip-host-cache đã bị MySQL 8.3 loại bỏ: https://github.com/testcontainers/testcontainers-java/issues/8130
