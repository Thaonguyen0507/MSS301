# Kết quả thực thi KB1 và KB2

Thời điểm: **27/10/2026 20:29:54, Asia/Saigon**. Công cụ: **Newman 6.2.3**, chạy collection Postman. Kết quả: **20 request, 47 assertion, 0 lỗi**. Ảnh trong tài liệu này được xuất từ dữ liệu chạy thật, không phải ảnh chụp giao diện Postman Desktop.

## Môi trường

MySQL 8.4, MongoDB 8.0 replica set và SQL Server 2022 thuộc Docker project `slot14-saga`, cả ba healthy. Service Spring Boot 4.1.1 chạy trên host với cổng order **18081**, inventory **18082**, payment **18083**. Cổng được đổi để tránh ứng dụng khác đang dùng 8081–8082.

## KB1 thành công

Request: `POST http://localhost:18081/api/orders` với body `{"customerId":"C1","productId":"P1","quantity":2,"amount":2000}`.

- HTTP 200; order **5**: `CONFIRMED / COMPLETED`, `failureReason = null`.
- P1: **10 → 8**. C1: **5000 → 3000**. P2 và C2 không đổi.
- Reservation order 5: `RESERVED`; Payment order 5: `PAID`.
- Saga: `CREATE_ORDER → RESERVE_INVENTORY → PROCESS_PAYMENT → CONFIRM_ORDER`, mọi bước `SUCCESS`.
- Kết luận: **PASS** theo các assertion đã chạy.

## KB2 thanh toán thất bại 402

Request: `POST http://localhost:18081/api/orders` với body `{"customerId":"C2","productId":"P1","quantity":1,"amount":1000}`.

- HTTP order **200**; order **6**: `CANCELLED / COMPENSATED`.
- `failureReason`: `402 - Số dư không đủ: còn 100.0, cần 1000.0`.
- Log inventory xác nhận reserve làm P1 **8 → 7**, rồi release đưa về **8**. Ví C2 vẫn **100**.
- Reservation order 6: `RELEASED`; không có Payment của order 6.
- Saga: `CREATE_ORDER/SUCCESS → RESERVE_INVENTORY/SUCCESS → SAGA/FAILED → COMPENSATION/SUCCESS → CANCEL_ORDER/SUCCESS`.
- Request trực tiếp `POST http://localhost:18083/api/payments/pay` cho order 6/C2/1000 trả HTTP **402** và message số dư không đủ.
- Kết luận: **PASS** theo các assertion đã chạy. Bước SAGA/FAILED là lỗi nghiệp vụ mong đợi của ca này, không phải một assertion thất bại.

## Ảnh để chèn vào Word

| File | Nội dung và chú thích đề xuất |
|---|---|
| [00-newman-summary.png](test-evidence/images/00-newman-summary.png) | Tổng hợp lần chạy collection: 20 request, 47 assertion, 0 lỗi |
| [01-KB1-response.png](test-evidence/images/01-KB1-response.png) | Request, response và assertion tạo order 5 thành công |
| [02-KB1-data-saga.png](test-evidence/images/02-KB1-data-saga.png) | Kho/ví trước–sau, reservation/payment và saga log của KB1 |
| [03-KB2-response-402.png](test-evidence/images/03-KB2-response-402.png) | Order 6 được hoàn tác và payment-service trả HTTP 402 |
| [04-KB2-data-saga.png](test-evidence/images/04-KB2-data-saga.png) | Kho được hoàn lại, ví giữ nguyên và thứ tự compensation của KB2 |
| [05-service-logs.png](test-evidence/images/05-service-logs.png) | Log UTF8 của ba service gắn đúng orderId 5 và 6 |

Các ID 1–4 thuộc các lần chuẩn bị/chạy trước. Các đơn thành công của lần chuẩn bị đã được hoàn kho và hoàn tiền qua API compensation để đưa snapshot về 10/5000 trước lần chạy được báo cáo. Không xóa lịch sử đơn hàng; chỉ sử dụng orderId 5 và 6 để đối chiếu ảnh và dữ liệu lần này.

## Tái lập

```powershell
powershell -ExecutionPolicy Bypass -File .\Slot14\tools\Start-SagaDemo.ps1 -OrderPort 18081 -InventoryPort 18082 -PaymentPort 18083
node .\Slot14\tools\run-selected-tests.cjs --order-url http://localhost:18081 --inventory-url http://localhost:18082 --payment-url http://localhost:18083
```

Nếu service đang chạy thì bỏ qua lệnh Start. Lần chạy lại sinh orderId mới và dùng snapshot mới. Không tự áp dụng số liệu/ID trong báo cáo cho các lần chạy khác. KB3–KB8 chưa được chạy trong đợt này.
