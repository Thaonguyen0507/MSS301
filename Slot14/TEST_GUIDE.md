# Kiểm thử Saga Slot14

## Trạng thái hiện tại

Đã chạy thật KB1 và KB2 bằng Newman 6.2.3 ngày 27/10/2026: **20 request, 47 assertion, 0 lỗi**. KB1 là order **5**, KB2 là order **6**. Ba database Docker healthy; các service chạy trên cổng **18081–18083** để tránh ứng dụng khác đang dùng 8081–8082. KB3–KB8 chưa được chạy trong đợt này.

Ảnh báo cáo từ kết quả thật nằm trong `test-evidence/images/`; đây là ảnh được xuất từ dữ liệu Newman, không phải ảnh chụp giao diện Postman Desktop. Xem `TEST_RESULTS_KB1_KB2.md` để dùng số liệu và chú thích khi viết Word.

Báo cáo Word rút gọn nằm tại [Slot14_DE190669.docx](Slot14_DE190669.docx), gồm phần giới thiệu và 6 ảnh bằng chứng. Các file ở đây cung cấp quy trình, request và assertion tương ứng.

## Bản dùng cho bài Slot14

Dùng **`saga-demo/`** cho yêu cầu mục 6.3 của `Saga_guide.md`. Ba thư mục service trực tiếp bên ngoài là bản H2 cũ, có port và API khác. Không chạy đồng thời hai bản.

| Service | Port | Database |
|---|---:|---|
| order-service | 8081 | MySQL 8.4, host 3307 |
| inventory-service | 8082 | MongoDB 8.0 replica set, host 27017 |
| payment-service | 8083 | SQL Server 2022, host 14330 |

Spring Boot 4.1.1, target Java 21. Bản mẫu triển khai từ guide có `RestClient`, local transaction riêng từng bước, compensation theo stack LIFO, retry compensation tối đa 3 lần và bảng saga log. Khi release lỗi sau retry, saga giữ `COMPENSATING`.

## Chạy hệ thống

Máy cần Java 21 trở lên, Docker Desktop Linux containers và dung lượng trống đủ cho các image/volume. Từ `D:\MSS301`:

```powershell
powershell -ExecutionPolicy Bypass -File .\Slot14\tools\Start-SagaDemo.ps1 -Build
```

Nếu cổng mặc định đã có ứng dụng khác, dùng cổng riêng như lần chạy đã ghi nhận:

```powershell
powershell -ExecutionPolicy Bypass -File .\Slot14\tools\Start-SagaDemo.ps1 -OrderPort 18081 -InventoryPort 18082 -PaymentPort 18083
```

Script dùng cách A của guide: database chạy Docker, service chạy Java trên host. Log service nằm ở `Slot14/test-evidence/`. Các container có tên riêng `slot14-saga-*` để không dùng nhầm database của bài khác.

Cách B của guide cũng được cấu hình:

```powershell
cd D:\MSS301\Slot14\saga-demo
docker compose up -d --build
```

Chọn một cách, không chạy cả A và B. Không chạy `docker compose down -v` trên dữ liệu cần giữ. Khi cần dữ liệu mới, chỉ reset volume của đúng project `slot14-saga` sau khi đã lưu kết quả.

## Chạy Postman

1. Import `postman/Saga_KB1_KB8.postman_collection.json` vào Postman Desktop.
2. Nếu dùng cổng mặc định theo guide: orderUrl 8081, inventoryUrl 8082, paymentUrl 8083. Với môi trường của lần chạy đã ghi nhận, đặt lần lượt `http://localhost:18081`, `http://localhost:18082`, `http://localhost:18083`.
3. Mở Postman Console để xem request/response. Không ghi đè các ID được script lưu.
4. Run collection: chỉ chọn `0 - Service readiness`, `KB1 - Happy path`, `KB2 - Thanh toán thất bại 402` để chạy hai ca được chọn cho báo cáo. Bỏ chọn các folder khác và folder Manual.
5. Xem **Test Results**, response và endpoint saga-logs cho mỗi orderId. Lưu ảnh Postman nếu cần nộp minh chứng giao diện. Nếu dùng Newman, ghi rõ công cụ đó khi lập báo cáo.
6. KB1 hợp lệ khi kho P1 giảm 2 và C1 giảm 2000. KB2 hợp lệ khi kho P1 không đổi sau hoàn tác, C2 không đổi, reservation RELEASED, order CANCELLED và saga COMPENSATED.
7. **HTTP order ở KB2 là 200**; lỗi payment 402 nằm trong `failureReason`. Có thể gửi trực tiếp `POST http://localhost:8083/api/payments/pay` với orderId dành riêng, customerId C2, amount 1000 để quan sát 402.

Đọc `TEST_PROMPT_KB1_KB8.md` để có prompt đầy đủ và các bước KB3–KB8, bao gồm cách dừng/khôi phục service ở KB6 và KB8. Collection đã có các folder tương ứng; KB6/KB8 cần thao tác thủ công đúng thời điểm.

## Chạy Newman

Không cần cài Newman toàn cục:

```powershell
npm.cmd install --prefix .\Slot14\.tools\newman newman --no-audit --no-fund
node .\Slot14\tools\run-selected-tests.cjs
```

Với cổng riêng:

```powershell
node .\Slot14\tools\run-selected-tests.cjs --order-url http://localhost:18081 --inventory-url http://localhost:18082 --payment-url http://localhost:18083
```

`test-evidence/KB1-KB2-results.json` chứa từng URL, body, response và assertion; `newman-KB1-KB2.json` là output Newman đầy đủ của lần chạy. Các bước dùng snapshot để kiểm tra chênh lệch; KB1 cần đủ tồn kho/số dư nếu chạy lại nhiều lần. Các test script được giới hạn phạm vi biến riêng cho từng request để chạy liên tiếp trong Newman.

## Xem và dừng service

```powershell
Get-Content .\Slot14\test-evidence\order-service.stdout.log -Tail 50
Get-Content .\Slot14\test-evidence\inventory-service.stdout.log -Tail 50
Get-Content .\Slot14\test-evidence\payment-service.stdout.log -Tail 50
powershell -ExecutionPolicy Bypass -File .\Slot14\tools\Stop-SagaDemo.ps1 -StopDatabases
```

Script stop chỉ dừng PID có command line trỏ vào `Slot14/saga-demo` và ba database của Compose này. Dữ liệu volume được giữ lại.
