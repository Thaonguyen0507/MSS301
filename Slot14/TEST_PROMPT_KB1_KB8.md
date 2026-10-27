# Prompt hướng dẫn kiểm thử Saga Slot14 từ KB1 đến KB8

Sao chép nguyên khối bên dưới vào phiên làm việc có quyền đọc dự án và chạy API. Nguồn yêu cầu là `Slot14/Saga_guide.md`, mục 6.3 và 6.4. Chọn **KB1 và KB2** làm hai ca báo cáo chi tiết. Các giá trị tuyệt đối bên dưới chỉ áp dụng khi dữ liệu vừa reset và chạy KB1 trước KB2; khi chạy lại phải so sánh độ chênh lệch với snapshot.

```text
Bạn là người hướng dẫn kiểm thử bài MSS301 Slot14 về Saga Orchestration.
Hãy đọc Saga_guide.md, đặc biệt mục 6.3 và 6.4, rồi hướng dẫn tôi test
step-by-step KB1–KB8 bằng Postman. Chọn KB1 và KB2 để kiểm tra
chi tiết. Cung cấp nội dung bằng tiếng Việt để tôi tự viết báo cáo Word;
không tạo file Word. Nếu chưa chạy, chỉ trình bày kết quả mong đợi.

NGUYÊN TẮC
1. Đọc code, application config và Compose trước khi gửi request.
   Xác định đúng service trên từng port; không nhầm 404 do gọi sai service
   với 404 nghiệp vụ sản phẩm/ví không tồn tại.
2. Theo guide: orderUrl=http://localhost:8081,
   inventoryUrl=http://localhost:8082, paymentUrl=http://localhost:8083.
   API order là /api/orders, không phải /orders.
3. Kiểm tra GET inventoryUrl/api/inventory/products,
   GET paymentUrl/api/payments/wallets và GET orderUrl/api/orders.
   Seed mong đợi: P1 stock 10, P2 stock 5, C1 balance 5000,
   C2 balance 100. Ghi lại thực tế; không tự reset/xóa volume đang có dữ liệu.
4. Nếu API hoặc hạ tầng chưa tồn tại, ghi FAIL hoặc BLOCKED cùng response
   thật và nguyên nhân. Không đổi expected để làm xanh test; không giả lập
   ảnh Postman, log Saga hay số liệu chạy được.
5. Mỗi KB gồm: mục tiêu → tiền điều kiện → snapshot → request → kiểm tra
   response → kiểm tra kho/ví/reservation/payment → log service → saga-logs
   → kết luận PASS/FAIL/BLOCKED → khôi phục môi trường.
6. Lưu timestamp Asia/Saigon, URL, method, body, HTTP status, response,
   orderId và log gắn đúng orderId. Không dùng log của đơn khác.
7. Phân biệt HTTP của order API và HTTP của service con.
   KB2: POST /api/orders trả HTTP 200 theo guide, body thể hiện thất bại
   nghiệp vụ; payment-service mới là nơi trả 402. Nếu cần chứng minh 402
   trực tiếp, gọi /api/payments/pay với orderId dành riêng cho kiểm thử.
8. KB6/KB8 chạy thủ công, không tự stop container/service ngoài bài này.
   Không chạy KB8 khi chưa xác định cách bật lại đúng service.

CHUẨN BỊ POSTMAN
- Tạo collection “Slot14 Saga KB1 KB8”, các biến orderUrl,
  inventoryUrl, paymentUrl, kb1OrderId, kb2OrderId… kb8OrderId.
- Request JSON dùng Content-Type: application/json.
- Mở View → Show Postman Console để xem request/response.
- Tạo các folder KB1…KB8. KB6 và KB8 ghi rõ “manual”.
- Trước mỗi KB lưu stock và balance vào collection variables.
- Post-response scripts phải có assertion HTTP status, status/sagaStatus,
  failureReason, độ chênh lệch kho/ví và saga steps theo kịch bản.
- pm.test thất bại phải được ghi vào báo cáo; không bỏ qua lỗi parse JSON
  hoặc thiếu orderId sau khi request tạo order thất bại.

KB1 HẠNH PHÚC
1. Snapshot products và wallets, lưu stock_P1 và bal_C1.
2. POST {{orderUrl}}/api/orders:
   {"customerId":"C1","productId":"P1","quantity":2,"amount":2000}
3. Kiểm tra HTTP 200, status CONFIRMED, sagaStatus COMPLETED,
   failureReason null; lưu response.id vào kb1OrderId.
4. GET products và wallets: P1 giảm 2; C1 giảm 2000;
   P2 và C2 không đổi. Nếu seed mới: P1=8, C1=3000.
5. GET inventoryUrl/api/inventory/reservations: order này RESERVED.
   GET paymentUrl/api/payments: order này PAID.
6. GET {{orderUrl}}/api/orders/{{kb1OrderId}}/saga-logs:
   CREATE_ORDER → RESERVE_INVENTORY → PROCESS_PAYMENT → CONFIRM_ORDER;
   result SUCCESS. Kiểm tra time hợp lệ và thuộc lần chạy này.
7. Đối chiếu log 3 service: tạo đơn, reserve, pay và confirm cùng orderId.

KB2 THANH TOÁN THẤT BẠI 402
1. Chạy sau KB1; snapshot P1 và C2. Khi seed mới P1=8, C2=100.
2. POST {{orderUrl}}/api/orders:
   {"customerId":"C2","productId":"P1","quantity":1,"amount":1000}
3. Kiểm tra HTTP order 200, status CANCELLED, sagaStatus COMPENSATED,
   failureReason bắt đầu 402 và có nội dung số dư không đủ.
   Lưu response.id vào kb2OrderId.
4. GET products/wallets: P1 bằng snapshot, C2 bằng snapshot 100.
   GET reservations: order này RELEASED. Không có payment PAID
   cho order thất bại; không có khoản trừ tiền C2.
5. GET {{orderUrl}}/api/orders/{{kb2OrderId}}/saga-logs:
   CREATE_ORDER/SUCCESS → RESERVE_INVENTORY/SUCCESS → SAGA/FAILED
   → COMPENSATION/SUCCESS → CANCEL_ORDER/SUCCESS.
6. Log inventory phải có RESERVE rồi RELEASE cùng orderId.
   Nếu stock trước là 8: RESERVE còn 7 rồi RELEASE về 8.
   Log payment ghi lỗi số dư; log order ghi lỗi và compensation.
7. Chụp Postman response, Test Results, saga-logs và log service.
   Chỉ kết luận PASS khi tất cả kiểm tra khớp.

KB3 HẾT HÀNG 409
1. Snapshot P2 và C1.
2. POST /api/orders:
   {"customerId":"C1","productId":"P2","quantity":100,"amount":2000}
3. HTTP order 200; CANCELLED/COMPENSATED; failureReason ^409.
4. P2 và C1 không đổi. Không có reservation thành công/payment cho đơn.
5. Saga steps: CREATE_ORDER → SAGA → CANCEL_ORDER.
   Không có RESERVE_INVENTORY thành công và không gọi release/pay.
   Ghi rõ reserve thất bại nên stack compensation chưa có release.

KB4 SẢN PHẨM KHÔNG TỒN TẠI 404
1. Xác nhận không có P9; snapshot products và wallets.
2. POST /api/orders:
   {"customerId":"C1","productId":"P9","quantity":1,"amount":10}
3. HTTP order 200; CANCELLED/COMPENSATED;
   failureReason ^404 và nhắc sản phẩm P9.
4. Kho/ví không đổi; không có payment/reservation thành công cho đơn.
5. Saga steps CREATE_ORDER → SAGA → CANCEL_ORDER; không gọi pay/release.

KB5 VÍ KHÔNG TỒN TẠI 404
1. Xác nhận không có C9; snapshot P2 và các ví.
2. POST /api/orders:
   {"customerId":"C9","productId":"P2","quantity":1,"amount":20}
3. HTTP order 200; CANCELLED/COMPENSATED;
   failureReason ^404 và nhắc ví C9, không nhầm lỗi product 404.
4. P2 bằng snapshot sau release; không tạo thêm ví C9;
   reservation RELEASED và không có payment PAID cho đơn.
5. Saga steps giống KB2; log inventory RESERVE rồi RELEASE.

KB6 PAYMENT SERVICE BỊ TẮT
1. Snapshot P2 và C1; xác định chính xác process/container payment của bài.
2. Docker: docker compose stop payment-service.
   Chạy từ IDE: chỉ Stop payment-service của Slot14.
3. POST /api/orders:
   {"customerId":"C1","productId":"P2","quantity":1,"amount":20}
4. HTTP order 200; CANCELLED/COMPENSATED; reason bắt đầu
   “Service không phản hồi”. Không kết luận đây là lỗi 402.
5. P2 bằng snapshot, reservation RELEASED; saga steps giống KB2.
6. Bật lại payment: docker compose start payment-service hoặc Run IDE.
   Chờ sẵn sàng, đọc ví và payment: C1 không đổi, không payment PAID
   cho đơn này. Kiểm tra log order có network failure và release thành công.

KB7 IDEMPOTENT
1. Cần order thành công KB1 còn RESERVED/PAID và đơn KB2 RELEASED.
   Dùng ID thật, không cố định 1 và 2. Snapshot toàn bộ kho/ví.
2. POST inventoryUrl/api/inventory/release/{{kb2OrderId}} hai lần:
   mỗi lần HTTP 200, text RELEASED; P1 không tăng thêm.
3. POST inventoryUrl/api/inventory/reserve:
   {"orderId":<kb1OrderId>,"productId":"P1","quantity":2}
   HTTP 200, reservation cũ RESERVED; P1 không giảm thêm.
4. POST paymentUrl/api/payments/pay:
   {"orderId":<kb1OrderId>,"customerId":"C1","amount":2000}
   HTTP 200, payment cũ PAID; C1 không giảm thêm.
5. POST paymentUrl/api/payments/refund/{{kb2OrderId}}:
   HTTP 200, text REFUNDED; C2 không tăng vì đơn chưa được thanh toán.
6. So sánh kho/ví và số record trước/sau từng bước.
   Log release lặp báo không có gì để hoàn tác. KB7 kiểm tra API
   idempotent, không yêu cầu tạo thêm order/saga mới.

KB8 COMPENSATION CŨNG LỖI
1. Snapshot P2 và C1. Chỉ làm khi có PAYMENT_DELAY_MS hoạt động.
2. Bật order-service với PAYMENT_DELAY_MS=15000. Docker cần recreate
   container nếu env chưa áp dụng: kiểm tra env/log trước khi gửi request.
   Cách IDE: thêm env hoặc breakpoint trước paymentClient.pay.
3. POST /api/orders:
   {"customerId":"C1","productId":"P2","quantity":1,"amount":20}
4. Xác nhận log RESERVE đã thành công. Trong thời gian delay,
   stop payment-service và inventory-service thuộc Slot14.
5. Chờ hết retry, lưu orderId. Mong đợi HTTP order 200,
   PENDING/COMPENSATING, failureReason network failure.
6. GET saga-logs: có 3 dòng step COMPENSATION/result FAILED,
   không có CANCEL_ORDER. Không được báo COMPENSATED khi release lỗi.
7. Bật inventory/payment, chờ sẵn sàng. P2 bằng snapshot trừ 1,
   reservation vẫn RESERVED, ví C1 không đổi.
8. Lưu bằng chứng trạng thái treo trước khi khôi phục.
   Gọi POST inventoryUrl/api/inventory/release/<kb8OrderId> thủ công,
   kiểm tra P2 trở lại snapshot và reservation RELEASED.
   Không tự coi saga đã COMPENSATED chỉ vì release thủ công thành công.
9. Trả PAYMENT_DELAY_MS về 0, khởi động lại order đúng cấu hình ban đầu.

HAI CA THỰC THI VÀ BÁO CÁO WORD
- Chọn KB1 và KB2 để minh họa thành công và compensation khi payment 402.
- Mỗi ca ghi: tiền điều kiện, các bước Postman có URL/method/body,
  expected, actual, assertion, stock/balance trước-sau, orderId,
  response và trích log cùng orderId, kết luận và lý do.
- Nếu chưa có Postman UI, có thể chạy chính collection bằng Newman
  và ghi rõ dùng Newman; không ghi đã thao tác Postman Desktop.
- Nếu code không có contract theo guide, giữ kết quả FAIL/BLOCKED.
  Có thể chạy bổ sung API hiện tại để chẩn đoán nhưng phải tách riêng,
  không dùng nó thay thế hay gọi là KB1/KB2 đã PASS theo guide.
- Cung cấp dàn ý để tôi tự viết Word: mục tiêu, tiền điều kiện,
  quy trình, expected/actual, minh chứng, kết luận và đề xuất sửa.
  Không tạo file Word và không điền actual từ dữ liệu chưa chạy.
- Lưu raw responses, log, collection và kết quả Runner cạnh báo cáo
  để tái lập. Chỉ ghi KB3–KB8 là hướng dẫn nếu chưa thực thi.
```
