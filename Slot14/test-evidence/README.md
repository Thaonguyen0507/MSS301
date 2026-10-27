# Phạm vi xác minh

- `build-verification.json`: ba service mới đã `BUILD SUCCESS` bằng Maven package với `-DskipTests`.
- `static-validation.json`: collection JSON hợp lệ; 78 request và 78 post-response script đã được kiểm tra cú pháp, 10 body JSON parse được sau khi thay biến bằng giá trị mẫu.
- Script Node và PowerShell đã qua kiểm tra cú pháp.

**Chưa chạy HTTP/Postman/Newman.** Không có kết quả PASS/FAIL tích hợp của KB1–KB8 trong các file này. Kết quả mong đợi nằm trong prompt và assertion của collection.

Các log `*.stdout.log`, `*.stderr.log` và file PID cục bộ không đưa vào Git. Những log H2 cũ do quá trình chuẩn bị tạo ra không phải bằng chứng của bản `saga-demo`.
