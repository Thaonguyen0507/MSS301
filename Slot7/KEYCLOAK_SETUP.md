# Keycloak Setup Guide - Issue #8

## 📋 Tổng quan

Hướng dẫn cài đặt và cấu hình Keycloak cho Spring Microservices Platform.

## 🎯 Mục tiêu

- ✅ Khởi động Keycloak container
- ✅ Tạo realm: `spring-microservices-realm`
- ✅ Tạo client: `spring-cloud-gateway-client`
- ✅ Tạo test users: `user` và `admin`
- ✅ Lấy JWT token qua Postman
- ✅ Verify token hoạt động với Gateway

## 🚀 Bước 1: Khởi động Keycloak

```bash
# Khởi động tất cả services (bao gồm Keycloak)
docker compose up -d

# Kiểm tra Keycloak đã chạy chưa
docker ps | grep keycloak

# Xem logs của Keycloak
docker logs keycloak -f
```

**Chờ khoảng 30-60 giây** để Keycloak khởi động hoàn toàn.

## 🌐 Bước 2: Truy cập Keycloak Admin Console

1. Mở trình duyệt: http://localhost:8181
2. Click **Administration Console**
3. Đăng nhập:
   - **Username**: `admin`
   - **Password**: `admin`

## ✅ Bước 3: Verify Realm đã được import

Realm `spring-microservices-realm` đã được tự động import từ file:
```
api-gateway/docker/keycloak/realms/spring-microservices-realm.json
```

### Kiểm tra Realm:

1. Ở góc trên bên trái, click dropdown realm
2. Bạn sẽ thấy: `spring-microservices-realm`
3. Chọn realm này để làm việc

### Nếu realm chưa có (manual import):

1. Click **Create Realm**
2. Click **Browse** và chọn file `spring-microservices-realm.json`
3. Click **Create**

## 👥 Bước 4: Verify Users

Realm đã có sẵn 2 test users:

| Username | Password | Role  | Email              |
|----------|----------|-------|--------------------|
| `user`   | `password` | user  | user@example.com   |
| `admin`  | `admin`    | admin | admin@example.com  |

### Kiểm tra Users:

1. Trong realm `spring-microservices-realm`
2. Click **Users** (menu bên trái)
3. Click **View all users**
4. Bạn sẽ thấy 2 users: `user` và `admin`

## 🔑 Bước 5: Verify Client Configuration

Client `spring-cloud-gateway-client` đã được tạo sẵn.

### Kiểm tra Client:

1. Click **Clients** (menu bên trái)
2. Tìm client: `spring-cloud-gateway-client`
3. Click vào client để xem chi tiết

### Thông tin Client quan trọng:

- **Client ID**: `spring-cloud-gateway-client`
- **Client Secret**: `qwerty12345`
- **Access Type**: `confidential`
- **Direct Access Grants**: `Enabled` ✅
- **Service Accounts**: `Enabled` ✅

## 🧪 Bước 6: Test lấy JWT Token

### 6.1. Sử dụng PowerShell

```powershell
$body = @{
    grant_type='password'
    client_id='spring-cloud-gateway-client'
    client_secret='qwerty12345'
    username='user'
    password='password'
}
$response = Invoke-RestMethod -Uri 'http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token' -Method POST -Body $body -ContentType 'application/x-www-form-urlencoded'
Write-Host "Access Token: $($response.access_token)"
```

### 6.2. Sử dụng curl

```bash
curl -X POST "http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-gateway-client" \
  -d "client_secret=qwerty12345" \
  -d "username=user" \
  -d "password=password"
```

### 6.3. Sử dụng Postman

1. Method: `POST`
2. URL: `http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token`
3. Headers:
   - `Content-Type`: `application/x-www-form-urlencoded`
4. Body (x-www-form-urlencoded):
   - `grant_type`: `password`
   - `client_id`: `spring-cloud-gateway-client`
   - `client_secret`: `qwerty12345`
   - `username`: `user`
   - `password`: `password`

**Response mẫu:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "profile email"
}
```

### 6.4. Lấy Token cho Admin

Thay `username` và `password`:
- `username`: `admin`
- `password`: `admin`

### 6.5. Decode và Verify Token

Copy `access_token` và paste vào: https://jwt.io

**Token sẽ chứa:**
```json
{
  "exp": 1234567890,
  "iat": 1234567590,
  "jti": "...",
  "iss": "http://localhost:8181/realms/spring-microservices-realm",
  "aud": "account",
  "sub": "...",
  "typ": "Bearer",
  "azp": "spring-cloud-gateway-client",
  "session_state": "...",
  "realm_access": {
    "roles": [
      "user"
    ]
  },
  "resource_access": {
    "spring-cloud-gateway-client": {
      "roles": [
        "user"
      ]
    }
  },
  "scope": "profile email",
  "email_verified": true,
  "name": "Regular User",
  "preferred_username": "user",
  "given_name": "Regular",
  "family_name": "User",
  "email": "user@example.com"
}
```

## 🔐 Bước 7: Test Token với API Gateway (sau khi implement Issue #7)

```bash
# Test với token
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:9000/api/product

# Hoặc dùng Postman:
# GET http://localhost:9000/api/product
# Headers:
#   Authorization: Bearer YOUR_ACCESS_TOKEN
```

## 📝 Bước 8: Export Realm Configuration (Optional)

Nếu bạn thay đổi cấu hình và muốn export:

1. Trong Keycloak Admin Console
2. Chọn realm `spring-microservices-realm`
3. Click **Realm settings** → **Action** → **Partial export**
4. Chọn các options cần export
5. Click **Export**

## 🐛 Troubleshooting

### Keycloak không khởi động

```bash
# Xem logs
docker logs keycloak

# Restart Keycloak
docker compose restart keycloak

# Nếu vẫn lỗi, xóa và tạo lại
docker compose down
docker volume rm progress-test-1-kirenz_keycloak-mysql-data
docker compose up -d
```

### Không lấy được token

**Lỗi: "Invalid client credentials"**
- Kiểm tra `client_secret` đúng: `qwerty12345`
- Kiểm tra `client_id` đúng: `spring-cloud-gateway-client`

**Lỗi: "Invalid user credentials"**
- Kiểm tra username/password
- User: `user` / `password`
- Admin: `admin` / `admin`

**Lỗi: "Client not found"**
- Kiểm tra realm đúng: `spring-microservices-realm`
- URL phải có `/realms/spring-microservices-realm/`

### Realm không được import tự động

```bash
# Stop Keycloak
docker compose stop keycloak

# Xóa data cũ
docker volume rm progress-test-1-kirenz_keycloak-mysql-data

# Start lại
docker compose up -d keycloak

# Hoặc import manual qua Admin Console
```

## 📊 Kiểm tra hoàn thành

- [ ] Keycloak accessible tại http://localhost:8181
- [ ] Đăng nhập Admin Console thành công
- [ ] Realm `spring-microservices-realm` tồn tại
- [ ] Client `spring-cloud-gateway-client` được cấu hình đúng
- [ ] 2 users (`user`, `admin`) tồn tại
- [ ] Lấy được JWT token cho `user` qua Postman
- [ ] Lấy được JWT token cho `admin` qua Postman
- [ ] Token decode được trên jwt.io
- [ ] Token chứa đúng roles và claims

## 🔗 Thông tin quan trọng

### Endpoints

- **Keycloak Admin Console**: http://localhost:8181
- **Token Endpoint**: http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token
- **JWKS Endpoint**: http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/certs
- **Issuer**: http://localhost:8181/realms/spring-microservices-realm

### Credentials

**Keycloak Admin:**
- Username: `admin`
- Password: `admin`

**Test Users:**
- User: `user` / `password` (role: user)
- Admin: `admin` / `admin` (role: admin)

**Client:**
- Client ID: `spring-cloud-gateway-client`
- Client Secret: `qwerty12345`

## 📚 Tài liệu tham khảo

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

## ✅ Next Steps

Sau khi hoàn thành Issue #8, tiếp tục với:
- **Issue #7**: API Gateway - Security Integration với Keycloak
