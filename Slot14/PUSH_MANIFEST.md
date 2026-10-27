# Danh sách file dự định push cho Slot14

Nhánh: `feat/slot14-saga-test-report`. Danh sách ban đầu gồm 82 file đã được duyệt và push. Đợt kiểm thử thực tế bổ sung 12 file bằng chứng/công cụ bên dưới và báo cáo Word đã được duyệt, tổng cộng 95 file trong PR.

## Phạm vi

- Prompt và hướng dẫn KB1–KB8; chọn KB1 và KB2 để viết báo cáo.
- Collection Postman cùng assertion và runner Newman.
- Mã nguồn ba service theo Saga_guide, Spring Boot 4.1.1 và cấu hình hạ tầng để tái lập sau này.
- Kết quả build và kiểm tra cú pháp đã thực hiện; không ghi các ca HTTP chưa chạy là PASS.
- Báo cáo Word: `Slot14/Slot14_DE190669.docx`, gồm 6 trang và 6 ảnh bằng chứng từ kết quả Newman thật.

## Không đưa vào push

- Ba thư mục H2 cũ `Slot14/order-service/`, `Slot14/inventory-service/`, `Slot14/payment-service/`.
- `Slot14/REVIEW.md`, `Slot14/chapter6_guide.md` và README cũ sao chép từ bài trước.
- `target/`, JAR, cache khôi phục trên D, npm dependencies, file PID và log khởi động H2 cũ.
- Hai file GIT_CONVENTIONS.docx ngoài phạm vi Slot14.
- Các file thuộc Slot13, Assignment1 hoặc phần khác của repository.

## Kiểm tra đã thực hiện

- Ba Maven package BUILD SUCCESS, có dùng skipTests.
- Collection JSON và 10 body JSON hợp lệ; 78 post-response script parse được.
- Node runner và hai script PowerShell parse được.
- Đã chạy KB1 và KB2 bằng Newman 6.2.3 ngày 27/10/2026: 20 request, 47 assertion, 0 lỗi; orderId 5 và 6. KB3–KB8 chưa chạy.

## Commit dự kiến

Theo Conventional Commits, tiêu đề viết thường, dạng mệnh lệnh, tối đa 50 ký tự. Tách phần build, inventory, payment, order saga, collection và tài liệu. Commit ban đầu ghi chuẩn bị kiểm tra; các commit bổ sung lưu kết quả thực thi và ảnh của KB1/KB2.

## Danh sách đầy đủ

**82 file của đợt push đầu**:

```text
Slot14/.gitignore
Slot14/.tools/.gitignore
Slot14/PUSH_MANIFEST.md
Slot14/README_TESTING.md
Slot14/Saga_guide.md
Slot14/TEST_GUIDE.md
Slot14/TEST_PROMPT_KB1_KB8.md
Slot14/postman/Saga_KB1_KB8.postman_collection.json
Slot14/saga-demo/.gitignore
Slot14/saga-demo/docker-compose.yml
Slot14/saga-demo/inventory-service/.dockerignore
Slot14/saga-demo/inventory-service/.gitattributes
Slot14/saga-demo/inventory-service/.gitignore
Slot14/saga-demo/inventory-service/.mvn/wrapper/maven-wrapper.properties
Slot14/saga-demo/inventory-service/Dockerfile
Slot14/saga-demo/inventory-service/mvnw
Slot14/saga-demo/inventory-service/mvnw.cmd
Slot14/saga-demo/inventory-service/pom.xml
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/InventoryServiceApplication.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/config/DataSeeder.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/config/MongoConfig.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/controller/InventoryController.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/dto/ReserveRequest.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/entity/Product.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/entity/Reservation.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/entity/ReservationStatus.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/repository/ProductRepository.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/repository/ReservationRepository.java
Slot14/saga-demo/inventory-service/src/main/java/com/mss301/inventory/service/InventoryService.java
Slot14/saga-demo/inventory-service/src/main/resources/application.properties
Slot14/saga-demo/order-service/.dockerignore
Slot14/saga-demo/order-service/.gitattributes
Slot14/saga-demo/order-service/.gitignore
Slot14/saga-demo/order-service/.mvn/wrapper/maven-wrapper.properties
Slot14/saga-demo/order-service/Dockerfile
Slot14/saga-demo/order-service/mvnw
Slot14/saga-demo/order-service/mvnw.cmd
Slot14/saga-demo/order-service/pom.xml
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/OrderServiceApplication.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/client/InventoryClient.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/client/PaymentClient.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/config/RestClientConfig.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/controller/OrderController.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/dto/CreateOrderRequest.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/dto/PaymentRequest.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/dto/ReserveRequest.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/entity/Order.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/entity/OrderStatus.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/entity/SagaLog.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/entity/SagaStatus.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/repository/OrderRepository.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/repository/SagaLogRepository.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/saga/OrderSagaOrchestrator.java
Slot14/saga-demo/order-service/src/main/java/com/mss301/order/service/OrderService.java
Slot14/saga-demo/order-service/src/main/resources/application.properties
Slot14/saga-demo/payment-service/.dockerignore
Slot14/saga-demo/payment-service/.gitattributes
Slot14/saga-demo/payment-service/.gitignore
Slot14/saga-demo/payment-service/.mvn/wrapper/maven-wrapper.properties
Slot14/saga-demo/payment-service/Dockerfile
Slot14/saga-demo/payment-service/mvnw
Slot14/saga-demo/payment-service/mvnw.cmd
Slot14/saga-demo/payment-service/pom.xml
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/PaymentServiceApplication.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/controller/PaymentController.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/dto/PaymentRequest.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/entity/Payment.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/entity/PaymentStatus.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/entity/Wallet.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/repository/PaymentRepository.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/repository/WalletRepository.java
Slot14/saga-demo/payment-service/src/main/java/com/mss301/payment/service/PaymentService.java
Slot14/saga-demo/payment-service/src/main/resources/application.properties
Slot14/saga-demo/payment-service/src/main/resources/data.sql
Slot14/test-evidence/.gitignore
Slot14/test-evidence/README.md
Slot14/test-evidence/build-verification.json
Slot14/test-evidence/static-validation.json
Slot14/tools/Start-SagaDemo.ps1
Slot14/tools/Stop-SagaDemo.ps1
Slot14/tools/create-postman-collection.cjs
Slot14/tools/run-selected-tests.cjs
```

## Bổ sung sau khi kiểm thử thực tế

Đã sửa healthcheck MySQL, hỗ trợ cổng riêng và log UTF8, giới hạn phạm vi biến của test script trong collection. 12 file mới được bổ sung cùng những sửa đổi này:

```text
Slot14/TEST_RESULTS_KB1_KB2.md
Slot14/test-evidence/KB1-KB2-results.json
Slot14/test-evidence/images/00-newman-summary.png
Slot14/test-evidence/images/01-KB1-response.png
Slot14/test-evidence/images/02-KB1-data-saga.png
Slot14/test-evidence/images/03-KB2-response-402.png
Slot14/test-evidence/images/04-KB2-data-saga.png
Slot14/test-evidence/images/05-service-logs.png
Slot14/test-evidence/images/image-manifest.json
Slot14/test-evidence/newman-KB1-KB2.json
Slot14/test-evidence/service-log-excerpts.txt
Slot14/tools/export-newman-images.py
```

Ảnh là báo cáo xuất từ kết quả Newman thật theo lựa chọn của người dùng, không phải ảnh chụp giao diện Postman Desktop. File ZIP tiện tải về được tạo cục bộ; Git lưu từng ảnh PNG và dữ liệu gốc.

## Bổ sung báo cáo Word

`Slot14/Slot14_DE190669.docx` dùng bố cục từ báo cáo Assignment1, gồm phần giới thiệu ngắn, thông tin sinh viên DE190669 và 6 ảnh kết quả KB1–KB2. Không đưa file khóa Word (`~$...docx`) hoặc bản render tạm vào Git.
