# Kiểm thử Saga Slot14

## Trạng thái hiện tại

Đã build thành công ba service theo guide và chuẩn bị prompt/collection. Theo yêu cầu, không tiếp tục chạy Docker hoặc kiểm thử HTTP trong đợt này. Chưa có kết quả Postman/Newman và chưa xác nhận KB1–KB8 PASS. Các trạng thái và số liệu trong prompt là **kết quả mong đợi theo guide** để đối chiếu khi chạy.

Bạn tự soạn báo cáo Word; các file ở đây cung cấp quy trình, request và assertion để sử dụng cho báo cáo đó.

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

Script dùng cách A của guide: database chạy Docker, service chạy Java trên host. Log service nằm ở `Slot14/test-evidence/`. Các container có tên riêng `slot14-saga-*` để không dùng nhầm database của bài khác.

Cách B của guide cũng được cấu hình:

```powershell
cd D:\MSS301\Slot14\saga-demo
docker compose up -d --build
```

Chọn một cách, không chạy cả A và B. Không chạy `docker compose down -v` trên dữ liệu cần giữ. Khi cần dữ liệu mới, chỉ reset volume của đúng project `slot14-saga` sau khi đã lưu kết quả.

## Chạy Postman

1. Import `postman/Saga_KB1_KB8.postman_collection.json` vào Postman Desktop.
2. Kiểm tra collection variables: orderUrl 8081, inventoryUrl 8082, paymentUrl 8083.
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

Sau khi thực sự chạy script, `test-evidence/KB1-KB2-results.json` sẽ chứa từng URL, body, response và assertion; `newman-KB1-KB2.json` sẽ là output Newman đầy đủ. Hiện chưa có hai file kết quả này. Các bước dùng snapshot để kiểm tra chênh lệch; KB1 cần đủ tồn kho/số dư nếu chạy lại nhiều lần.

## Xem và dừng service

```powershell
Get-Content .\Slot14\test-evidence\order-service.stdout.log -Tail 50
Get-Content .\Slot14\test-evidence\inventory-service.stdout.log -Tail 50
Get-Content .\Slot14\test-evidence\payment-service.stdout.log -Tail 50
powershell -ExecutionPolicy Bypass -File .\Slot14\tools\Stop-SagaDemo.ps1 -StopDatabases
```

Script stop chỉ dừng PID có command line trỏ vào `Slot14/saga-demo` và ba database của Compose này. Dữ liệu volume được giữ lại.
