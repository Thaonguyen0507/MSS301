[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/b_oi_2YT)
# MSS301 — Microservices Project (Part 1 → Part 4)

Project thực hành tích hợp cho môn **MSS301 — Microservices with Spring Boot**.  
Sinh viên xây dựng hệ thống thương mại điện tử từ đầu, trải qua 4 giai đoạn từ REST API cơ bản đến bảo mật với Keycloak.

---

## Kiến trúc tổng thể

```
Client (Postman)
    │
    │  Authorization: Bearer <JWT>
    ▼
API Gateway (:9000)          ← Part 3 + 4: Routing + Bảo mật OAuth2/JWT
    │  Xác minh JWT với Keycloak
    ├──→  Product Service  (:8080)  ← Part 1: MongoDB, CRUD sản phẩm
    ├──→  Order Service    (:8081)  ← Part 1 + 2: MySQL, đặt hàng + OpenFeign
    └──→  Inventory Service(:8082)  ← Part 1: MySQL, kiểm tra tồn kho
                               ↑
                    Order Service gọi qua OpenFeign

Keycloak (:8181)             ← Authorization Server (cấp JWT)
```

---

## Khởi động hệ thống

```bash
# 1. Khởi động tất cả infrastructure (MongoDB, MySQL, Keycloak)
docker compose up -d

# 2. Khởi động từng service theo thứ tự (mỗi terminal riêng)
cd inventory-service && ./mvnw spring-boot:run   # port 8082
cd product-service   && ./mvnw spring-boot:run   # port 8080
cd order-service     && ./mvnw spring-boot:run   # port 8081
cd api-gateway       && ./mvnw spring-boot:run   # port 9000
```

---

## Danh sách TODO (17 TODO cần hoàn thành)

### PHẦN 1 – Product Service (`product-service/`)

#### TODO PS-1 · `model/Product.java`
Thêm Lombok annotations (`@Document`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`, `@Data`) và khai báo 4 fields: `id`, `name`, `description`, `price`.

#### TODO PS-2 · `repository/ProductRepository.java`
Kế thừa `MongoRepository<Product, String>`.

#### TODO PS-3 · `dto/ProductRequest.java`
Chuyển thành Java Record với 3 fields: `name`, `description`, `price`.

#### TODO PS-4 · `dto/ProductResponse.java`
Chuyển thành Java Record với 4 fields: `id`, `name`, `description`, `price`.

#### TODO PS-5 · `service/ProductService.java` — `createProduct()`
Chuyển `ProductRequest` → `Product` entity dùng builder, lưu vào MongoDB.

#### TODO PS-6 · `service/ProductService.java` — `getAllProducts()`
Lấy tất cả từ DB, chuyển `List<Product>` → `List<ProductResponse>` bằng stream.

#### TODO PS-7 · `controller/ProductController.java` — POST endpoint
`POST /api/product` → `201 Created`, gọi `productService.createProduct()`.

#### TODO PS-8 · `controller/ProductController.java` — GET endpoint
`GET /api/product` → `200 OK`, trả về `List<ProductResponse>`.

**Kiểm thử:**
```bash
cd product-service && ./mvnw test
```

---

### PHẦN 2 – Inventory Service (`inventory-service/`)

#### TODO IS-1 · `model/Inventory.java`
Thêm `@Entity`, `@Table(name = "t_inventory")`, Lombok annotations, 3 fields: `id`, `skuCode`, `quantity`.

#### TODO IS-2 · `repository/InventoryRepository.java`
Kế thừa `JpaRepository<Inventory, Long>`. Thêm derived query method:
```java
boolean existsBySkuCodeAndQuantityIsGreaterThanEqual(String skuCode, int quantity);
```

#### TODO IS-3 · `service/InventoryService.java` — `isInStock()`
Gọi repository method, trả về boolean.

#### TODO IS-4 · `controller/InventoryController.java` — GET endpoint
`GET /api/inventory?skuCode=xxx&quantity=yyy` → `200 OK`, trả về `boolean`.

**Kiểm thử:**
```bash
cd inventory-service && ./mvnw test
```

---

### PHẦN 3 – Order Service (`order-service/`)

#### TODO OS-1 · `model/Order.java`
`@Entity`, `@Table(name = "t_orders")`, 5 fields: `id`, `orderNumber`, `skuCode`, `price`, `quantity`.

#### TODO OS-2 · `repository/OrderRepository.java`
Kế thừa `JpaRepository<Order, Long>`.

#### TODO OS-3 · `dto/OrderRequest.java`
Java Record với 3 fields: `skuCode`, `price`, `quantity`.

#### TODO OS-4 · `service/OrderService.java` — `placeOrder()`
1. Gọi `inventoryClient.isInStock()` để kiểm tra tồn kho.
2. Nếu còn hàng → tạo Order với `orderNumber = UUID.randomUUID()` → lưu DB.
3. Nếu hết hàng → `throw new RuntimeException(...)`.

#### TODO OS-5 · `controller/OrderController.java` — POST endpoint
`POST /api/order` → `201 Created`, trả về `"Order Placed Successfully"`.

#### TODO OS-6 · `pom.xml` — Thêm OpenFeign dependency
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

#### TODO OS-7 · `client/InventoryClient.java` — FeignClient interface
Khai báo `@FeignClient(value = "inventory", url = "${inventory.url}")` và method `isInStock()`.

#### TODO OS-8 · `OrderServiceApplication.java` — `@EnableFeignClients`
Thêm annotation này vào class main để Spring scan và tạo Feign proxy.

#### TODO OS-9 · `application.properties` — `inventory.url`
```properties
inventory.url=http://localhost:8082
```

**Kiểm thử:**
```bash
cd order-service && ./mvnw test
# WireMock tự mock Inventory Service – không cần inventory-service thật khi test
```

---

### PHẦN 4 – API Gateway (`api-gateway/`)

#### TODO GW-1 · `routes/Routes.java` — 3 routing rules
Tạo 3 `@Bean RouterFunction<ServerResponse>`:
- `/api/product` → `http://localhost:8080`
- `/api/order` → `http://localhost:8081`
- `/api/inventory` → `http://localhost:8082`

#### TODO GW-2 · `pom.xml` — Thêm OAuth2 Resource Server dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

#### TODO GW-3 · `application.properties` — `issuer-uri`
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/spring-microservices-realm
```

#### TODO GW-4 · `config/SecurityConfig.java` — `SecurityFilterChain`
Yêu cầu xác thực mọi request, cấu hình JWT Resource Server.

**Kiểm thử:**
```bash
cd api-gateway && ./mvnw test
```

---

## Tổng quan thứ tự làm bài

```
1. Hoàn thành Product Service    (PS-1 → PS-8)  → chạy test
2. Hoàn thành Inventory Service  (IS-1 → IS-4)  → chạy test
3. Hoàn thành Order Service      (OS-1 → OS-9)  → chạy test
4. Cài đặt Keycloak              (docker compose up -d, tạo realm + client)
5. Hoàn thành API Gateway        (GW-1 → GW-4)  → chạy test
6. Test end-to-end bằng Postman
```

---

## Tài liệu tham khảo

| Phần | Tài liệu |
|------|----------|
| Part 1 | `microservices/part1.md` |
| Part 2 | `microservices/HUONG_DAN_OpenFeign_Part2.md` |
| Part 3 | `microservices/SpringBoot_Microservices_Part3.md` |
| Part 4 | `microservices/SpringBoot_Microservices_Part4.md` |
