# Phạm vi xác minh

- `build-verification.json`: ba service mới đã `BUILD SUCCESS` bằng Maven package với `-DskipTests`.
- `static-validation.json`: collection JSON hợp lệ; 78 request và 78 post-response script đã được kiểm tra cú pháp, 10 body JSON parse được sau khi thay biến bằng giá trị mẫu.
- Script Node và PowerShell đã qua kiểm tra cú pháp.

**Đã chạy thật KB1 và KB2** bằng Newman 6.2.3 lúc 20:29:54 ngày 27/10/2026 (Asia/Saigon): 20 request, 47 assertion, 0 lỗi. Order của lần chạy là 5 và 6. KB3–KB8 chưa được chạy.

- `KB1-KB2-results.json`: request, response và từng assertion trong lần chạy thành công cuối.
- `newman-KB1-KB2.json`: dữ liệu Newman nguyên bản.
- `service-log-excerpts.txt`: log ứng dụng UTF8 gắn đúng orderId 5/6.
- `images/`: 6 ảnh báo cáo từ dữ liệu thực thi; không phải ảnh chụp Postman Desktop. `image-manifest.json` ghi hash SHA256 của file dữ liệu nguồn.

Các file build/static-validation giữ lại phạm vi và thời điểm kiểm tra ban đầu; xem kết quả HTTP mới trong `KB1-KB2-results.json`.

Các log `*.stdout.log`, `*.stderr.log`, `*.application.log` đầy đủ và file PID cục bộ không đưa vào Git. Những log H2 cũ do quá trình chuẩn bị tạo ra không phải bằng chứng của bản `saga-demo`. Bản trích log trong `service-log-excerpts.txt` được lấy trực tiếp từ log UTF8 của bản theo guide.
