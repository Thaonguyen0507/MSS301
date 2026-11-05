# Assignment 02 - Watch Management

Bài làm đến hết TODO5, mã sinh viên **DE190669**.

| TODO | Kết quả |
| --- | --- |
| 0 | Git ignore, env mẫu, commit template |
| 1 | 4 project Maven, Java 21, Boot 4.1.1, Cloud 2025.1.3 |
| 2 | DBAccount, DBBrand, DBWatch; datasource riêng cho từng service |
| 3 | 5 tài khoản BCrypt, login, access/refresh JWT, logout, xử lý lỗi |
| 4 | [Thiết kế 3 Saga](docs/SAGA_DESIGN.md), compensation, state machine, pivot |
| 5 | MSBrand: seed, truy vấn brand và gán/gỡ watch idempotent |

MSWatch và Gateway hiện là scaffold. Orchestrator/CRUD watch thuộc TODO6;
routing và kiểm tra JWT tại Gateway thuộc TODO7. Collection ở giai đoạn
này gọi trực tiếp MSAccount và MSBrand.

## Cấu hình

1. Dùng JDK 21 và SQL Server; chạy [script tạo database](sql/01-create-databases.sql)
   trong SSMS hoặc bằng `sqlcmd`.
2. Copy `.env.example` thành `.env`, điền thông tin SQL Server và JWT secret
   ngẫu nhiên dài ít nhất 32 byte. SQL Authentication là cấu hình mặc định;
   bật SQL Server Authentication và login `sa` nếu cần.
3. Nạp biến môi trường vào mỗi cửa sổ PowerShell trước khi chạy service.
   Spring Boot không tự đọc file `.env`.

```powershell
cd D:\MSS301\Slot18\Assignment2
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

Với Windows Authentication, đặt `DB_INTEGRATED_SECURITY=true` trong `.env`.
JDBC cần DLL `mssql-jdbc_auth-13.4.0.x64.dll` của Microsoft và biến
`JAVA_TOOL_OPTIONS=-Djava.library.path=<thư mục chứa DLL>`. Khi dùng chế độ
này, username/password SQL được bỏ qua. File `.env` cục bộ dùng để kiểm thử
đã cấu hình Windows Authentication; file này được Git ignore.

## Build và chạy

Build cả 4 project từ thư mục Assignment2:

```powershell
.\MSAccount_DE190669\mvnw.cmd -f pom.xml clean package
```

Mỗi lệnh dưới chạy trong một cửa sổ PowerShell đã nạp biến môi trường:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\MSAccount_DE190669\target\ms-account-1.0.0.jar
& "$env:JAVA_HOME\bin\java.exe" -jar .\MSBrand_DE190669\target\ms-brand-1.0.0.jar
& "$env:JAVA_HOME\bin\java.exe" -jar .\MSWatch_DE190669\target\ms-watch-1.0.0.jar
& "$env:JAVA_HOME\bin\java.exe" -jar .\APIGateway_DE190669\target\api-gateway-1.0.0.jar
```

| Service | Cổng | Database |
| --- | --- | --- |
| MSAccount | 8081 | DBAccount |
| MSBrand | 8082 | DBBrand |
| MSWatch | 8083 | DBWatch |
| APIGateway | 8080 | — |

Tài khoản seed: `adminsys`, `staff02`, `member01`, `member02`, `staff01`;
mật khẩu đều là `@1`. Hai tài khoản cuối inactive. Access token có hạn
15 phút; refresh token 7 ngày. Logout yêu cầu client xóa token đang giữ.

## Kiểm thử

Import [Postman collection](Assignment2.postman_collection.json), chạy theo
thứ tự với `accountUrl=http://localhost:8081`, `brandUrl=http://localhost:8082`.
Hoặc dùng Newman:

```powershell
npx --yes newman run Assignment2.postman_collection.json
```

Đã chạy trên SQL Server: **29 request, 51 assertion, 0 lỗi**. Collection
kiểm tra login/refresh, role, tài khoản inactive, dữ liệu thiếu, seed brand,
assign lặp, chuyển brand, brand không tồn tại và unassign lặp. WatchID 99
là dữ liệu kiểm thử, được gỡ khi kết thúc collection. Seed cuối cùng có
5 brand, 10 liên kết; watchCount lần lượt là Rolex 2, Omega 2, Seiko 2,
Casio 3, TAG Heuer 1.

## Git

Các bước dùng nhánh `feature/scaffold`, `feature/ms-account`,
`docs/saga-design`, `feature/ms-brand-participant`; merge bằng `--no-ff`.
Commit dùng Conventional Commits và footer `Refs: TODO-x.y`.
