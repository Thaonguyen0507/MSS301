# Bài kiểm thử Saga Slot14

- [Prompt step-by-step KB1–KB8](TEST_PROMPT_KB1_KB8.md)
- [Hướng dẫn sử dụng và trạng thái kiểm thử](TEST_GUIDE.md)
- [Collection Postman](postman/Saga_KB1_KB8.postman_collection.json)
- [Kết quả thực thi KB1 và KB2](TEST_RESULTS_KB1_KB2.md)
- [Ảnh báo cáo để chèn Word](test-evidence/images/)
- [Mã nguồn theo guide](saga-demo/)
- [Kết quả build đã xác minh](test-evidence/build-verification.json)
- [Danh sách file dự định push](PUSH_MANIFEST.md)

Chọn **KB1 và KB2** cho báo cáo: một luồng thành công và một luồng hoàn tác khi payment trả 402. Quy trình, body request, assertion và kết quả mong đợi đã có trong prompt/collection. Bạn tự viết Word.

Ba service trong `saga-demo` đã được build bằng Spring Boot 4.1.1. KB1 và KB2 đã chạy thật trên ba database Docker: **20 request và 47 assertion đều thành công**. Lần chạy dùng Newman 6.2.3 và cổng service 18081–18083; ảnh là báo cáo xuất từ dữ liệu thật, không phải giao diện Postman Desktop. KB3–KB8 chưa được kiểm thử.

Nhánh làm việc: `feat/slot14-saga-test-report`. Draft PR về `main` được giữ để bạn bổ sung Word và tài liệu trước khi merge.

Các service H2 cũ nằm trực tiếp trong Slot14 và tài liệu review cũ không thuộc bộ mã nguồn mới này.
