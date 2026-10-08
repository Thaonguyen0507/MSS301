# Kết quả kiểm thử thực tế

Ngày 08/10/2026, múi giờ Asia/Saigon.

| Kiểm tra | Kết quả |
|---|---|
| Build 4 project | PASS |
| Spring context 4 project | 4/4 PASS |
| Postman collection chính | 108 request, 219 assertion, 0 lỗi |
| Postman BR14 khi Movie Service tắt | 2 request, 6 assertion, 0 lỗi |
| Hai request cùng ghế | 201 + 409, chỉ 1 vé được ghi |
| Hủy và đặt lại ghế | PASS |
| Doanh thu nhiều phim, thứ tự giảm dần | PASS |
| Restart movie-service | Số document/index không đổi |
| SQL Server | cinema_customer, Flyway V1/V2, NVARCHAR, Unicode đúng |
| MongoDB | 4 collection, unique index tên, ticketPrice decimal |
| MySQL | cinema_booking, Flyway V1/V2, unique ghế còn hiệu lực |

Collection chạy bằng Newman, dùng file Postman có thể import và chạy trong Collection Runner. Báo cáo HTML và ảnh là bản render từ kết quả thực, không phải ảnh Postman Desktop.

Hệ thống đã phục hồi đủ 4 ứng dụng sau kiểm tra 503. SQL Server Docker dùng host port 14330 để tránh SQL Server khác trên máy ở 1433.

## File bằng chứng

- [Báo cáo HTML](test-report.html), [ảnh báo cáo](test-report.png)
- [Newman JSON](newman-results.json), [JUnit XML](newman-results.xml)
- [BR14 JSON](outage-newman-results.json), [JUnit XML](outage-newman-results.xml)
- [Kiểm tra đồng thời](concurrency-results.json)
- [SQL Server](sqlserver-check.txt), [MongoDB](mongodb-check.txt), [MySQL](mysql-check.txt)
- [Docker](docker-status.txt), [restart seed](seed-restart.txt)
- Log build.log và maven-tests.log trong thư mục này.
