# Đối chiếu Slot13 với guide

Ngày kiểm tra: 22/10/2026.

**Kết luận:** code hiện tại triển khai phần demo H2 trong `chapter6_guide.md`, có thêm saga state, retry, idempotency key và correlation ID. Chưa đạt đầy đủ checklist của guide này và chưa đáp ứng bài Docker/multiple databases trong `Saga_guide.md`. Hai guide có kiến trúc và API khác nhau; không thể xem chúng là cùng một checklist.

## Các vấn đề cần sửa, theo mức độ ưu tiên

1. **[P1] Báo hoàn tác thành công dù release kho thất bại.**
   `order-service/src/main/java/com/example/order_service/saga/SagaOrchestrator.java:105` bắt lỗi release rồi tiếp tục gán cả saga và order thành `COMPENSATED` ở dòng 109–114. Nếu inventory-service mất kết nối sau khi reserve, hàng vẫn bị giữ nhưng kết quả lại báo đã hoàn tác. Phải giữ `COMPENSATING` khi hết retry và lưu lỗi để có thể xử lý lại. Đây cũng là tình huống KB8 của `Saga_guide.md`.

2. **[P1] Reserve kho chưa idempotent và chỉ giữ được một order cho mỗi sản phẩm.**
   `inventory-service/src/main/java/com/example/inventory_service/service/InventoryService.java:20` luôn trừ kho và ghi đè `reservedFor`. Gửi hai lần reserve cùng order sẽ trừ hai lần. Reserve A, reserve B rồi release A sẽ không hoàn kho cho A vì `reservedFor` đã thành B (dòng 35). Retry sau khi response bị mất cũng có thể trừ kho lặp. Cần reservation riêng theo order, ràng buộc duy nhất và xử lý tồn kho an toàn khi đồng thời.

3. **[P1] Transaction của order bao trùm cả lời gọi HTTP và toàn bộ saga.**
   `order-service/src/main/java/com/example/order_service/service/OrderService.java:36` có `@Transactional` và gọi `executeSaga` ở dòng 66. Các lần lưu saga state tham gia cùng transaction nên chưa commit sau từng bước. Nếu service dừng giữa luồng, trạng thái theo dõi có thể mất trong khi inventory/payment đã commit. Nên tách các thao tác lưu order/state thành local transaction riêng; điều phối HTTP nằm ngoài transaction đó.

4. **[P2] API chưa tương thích request mẫu của các guide.**
   Code nhận JSON cho reserve/process trong khi `chapter6_guide.md` minh họa query parameters. API tra kho là `GET /inventory/{productId}` thay vì `/inventory/check/{productId}`; API saga state là `/saga/{orderId}` thay vì `/orders/{id}/saga-state`. `Saga_guide.md` yêu cầu toàn bộ API `/api/...` và endpoint saga-logs, hiện chưa có. Chạy nguyên request mẫu/collection của guide sẽ không đạt.

5. **[P2] Các TODO nâng cao của chapter6 mới hoàn thành một phần.**
   Có retry 3 lần với backoff, nhưng retry mọi `Exception`, chưa có `@Recover` và log `[RETRY]` theo checklist TODO-2. Có cấu hình/annotation circuit breaker nhưng chưa expose `/actuator/circuitbreakers`, chưa bật health indicator theo TODO-4. Correlation ID được truyền giữa service, nhưng controller order tạo ID mới thay cho ID filter đã nhận; cấu hình log chưa có `%X{correlationId}` theo TODO-5.

6. **[P2] Chưa kiểm tra dữ liệu đầu vào.**
   DTO/controller chưa có validation cho quantity/amount dương. Ví dụ reserve quantity âm làm tăng tồn kho. Cần kiểm tra ở API và lớp nghiệp vụ trước khi cập nhật dữ liệu.

## Đối chiếu riêng với Saga_guide.md

| Yêu cầu | Hiện trạng |
|---|---|
| Spring Boot 4.1.1, Java 21 | Đã nâng ba service lên Boot 4.1.1; POM vẫn target Java 21 |
| Order 8081/MySQL, inventory 8082/MongoDB, payment 8083/SQL Server | Chưa đạt: đang dùng 8080/8081/8082, cả ba đều H2 in-memory, `create-drop` |
| TODO-0: Compose database, healthcheck, volume, Mongo replica set | Chưa có |
| TODO-1/2: Product, Reservation, MongoTransactionManager, reserve/release idempotent | Chưa đạt: dùng entity JPA Inventory và một trường reservedFor |
| TODO-3/4: Wallet, số dư C1/C2, pay/refund SQL Server | Chưa đạt: payment chỉ kiểm tra amount > 1000, không có ví/số dư |
| TODO-5/6: OrderStatus và SagaStatus riêng, SagaLog, local transactions | Có Order và SagaState, nhưng thiếu customerId, SagaLog và các trạng thái/transaction đúng yêu cầu |
| TODO-7: InventoryClient/PaymentClient dùng RestClient | Chưa đạt: dùng RestTemplate trực tiếp trong orchestrator |
| TODO-8: stack compensation LIFO, refund/release/cancel, native RetryTemplate | Chưa đạt: luồng cố định chỉ release khi payment lỗi; refundPayment có khai báo nhưng không được gọi trong executeSaga |
| TODO-9: `/api/orders`, `/api/orders/{id}/saga-logs` | Chưa có đúng contract |
| TODO-10/11: Dockerfile, chạy Compose toàn hệ thống, Postman KB1–KB7 | Chưa có Dockerfile/Compose/collection trong Slot13; chưa có bằng chứng Runner |

## Những thay đổi đã thực hiện

- Nâng parent của `order-service`, `inventory-service`, `payment-service` từ **3.5.15 → 4.1.1**.
- Đổi sang `spring-boot-starter-webmvc` và `spring-boot-starter-webmvc-test`.
- Thêm module `spring-boot-h2console` để giữ chức năng H2 Console trên Boot 4.
- Order: đổi AOP starter thành `spring-boot-starter-aspectj`; đổi Resilience4j sang `resilience4j-spring-boot4:2.4.0`.
- Order: khai báo tường minh `spring-retry:2.0.13`, giữ cách retry hiện có của chapter6; thêm starter RestClient cho hỗ trợ HTTP client của Boot 4.
- Test order: thêm `spring-boot-resttestclient`, đổi package `TestRestTemplate` và thêm `@AutoConfigureTestRestTemplate`.

Các lỗi nghiệp vụ và phần kiến trúc còn thiếu phía trên được ghi nhận qua review; chưa được sửa trong lần nâng phiên bản này.

## Kiểm thử

Chạy `mvnw.cmd -B test` tại từng service, trên JDK 23.0.1 của máy và target Java 21 trong POM:

| Service | Tests | Failures | Errors | Kết quả |
|---|---:|---:|---:|---|
| order-service | 5 | 0 | 0 | BUILD SUCCESS |
| inventory-service | 1 | 0 | 0 | BUILD SUCCESS |
| payment-service | 1 | 0 | 0 | BUILD SUCCESS |

Bốn integration test order kiểm tra happy path, inventory lỗi, payment lỗi rồi release thành công và tạo order lặp idempotency key. HTTP inventory/payment trong các test này được mock; hai service còn lại chỉ có context-load test. Vì vậy **7 test pass xác nhận bộ test hiện có chạy được sau nâng cấp**, chưa chứng minh các lỗi ở trên đã được khắc phục hay hệ thống đạt kịch bản Docker/Postman trong guide. Báo cáo gốc nằm trong `target/surefire-reports` của từng service; log Maven nằm ở `build-check.log`.

Tham khảo nâng cấp: [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [Spring Boot 4.1.1 release](https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/).
