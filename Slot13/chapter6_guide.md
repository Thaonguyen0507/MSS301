# Chapter 06: Transactions in Microservices – Saga Pattern

> **Mục tiêu:** Hiểu vấn đề distributed transactions trong Microservices và triển khai Saga Orchestration Pattern với Spring Boot.
>
> **Môi trường:** Windows + IntelliJ IDEA Community Edition

---

## 1. Lý Thuyết Cơ Bản

### 1.1 Transaction là gì?

Transaction là một đơn vị công việc phải hoàn thành **toàn bộ hoặc không thực hiện gì** (all-or-nothing), đảm bảo tính toàn vẹn dữ liệu theo nguyên tắc **ACID**:

| Thuộc tính | Ý nghĩa |
|---|---|
| **Atomicity** | Tất cả bước thành công, hoặc tất cả thất bại |
| **Consistency** | Database luôn ở trạng thái hợp lệ |
| **Isolation** | Các transaction song song không ảnh hưởng nhau |
| **Durability** | Dữ liệu đã commit được lưu vĩnh viễn |

### 1.2 Monolith vs Microservices Transactions

| | Monolith | Microservices |
|---|---|---|
| Database | 1 database duy nhất | Mỗi service có DB riêng |
| Transaction | Dễ dùng `@Transactional` | Giao tiếp qua network |
| Rollback | Đơn giản | Phức tạp, dễ partial failure |
| ACID | Dễ đảm bảo | Rất khó đảm bảo đầy đủ |

### 1.3 Thách Thức Distributed Transaction

- **Network Failures**: Service có thể mất kết nối bất kỳ lúc nào
- **Partial Failures**: Một số bước thành công, các bước còn lại thất bại
- **Data Consistency**: Duy trì nhất quán dữ liệu giữa nhiều DB
- **CAP Theorem**: Hệ thống phân tán chỉ có thể đảm bảo được 2 trong 3 (Consistency, Availability, Partition Tolerance)

### 1.4 Các Pattern Giải Quyết Distributed Transactions

| Pattern | Mô tả | Nhược điểm |
|---|---|---|
| **2PC (Two-Phase Commit)** | Coordinator hỏi tất cả service "sẵn sàng chưa?" trước khi commit | Blocking, single point of failure, hiệu năng kém |
| **Saga Pattern** | Chuỗi local transactions, thất bại thì chạy compensating transaction | Eventual consistency, phức tạp hơn |
| **Event Sourcing** | Lưu events thay vì state hiện tại | Cần replay để rebuild state |
| **CQRS** | Tách riêng luồng đọc (Query) và ghi (Command) | Cần đồng bộ 2 model |

---

## 2. Saga Pattern – Chi Tiết

### 2.1 Khái Niệm

Saga Pattern quản lý distributed transaction bằng cách chia nhỏ thành **chuỗi local transactions**:
- Mỗi bước thực hiện một local transaction và **publish event** để kích hoạt bước tiếp
- Nếu một bước thất bại → chạy **compensating transactions** theo chiều ngược lại để rollback

### 2.2 Hai Cách Triển Khai

#### Choreography (Phi tập trung)
- Các service **tự lắng nghe events** và quyết định hành động
- Không có coordinator trung tâm
- Phù hợp: flow đơn giản, ít service

```
OrderService --[OrderCreated]--> InventoryService --[InventoryReserved]--> PaymentService
```

#### Orchestration (Tập trung) ← *Demo này dùng cách này*
- **Orchestrator trung tâm** điều phối toàn bộ workflow
- Gửi lệnh đến từng service, xử lý response
- Phù hợp: flow phức tạp, nhiều service, dễ debug

```
                  ┌──────────────────────┐
                  │  OrderSagaOrchestrator│
                  └───────┬──────────────┘
          ┌────────────────┼────────────────┐
    reserveInventory  processPayment   confirmOrder
          │                │                │
   InventoryService  PaymentService   OrderService
```

### 2.3 Compensating Transactions

Mỗi bước trong saga **phải có** một compensating transaction tương ứng:

| Bước thực hiện | Compensating transaction |
|---|---|
| Tạo đơn hàng (PENDING) | Hủy đơn hàng |
| Reserve tồn kho | Release tồn kho |
| Charge thanh toán | Refund thanh toán |

Yêu cầu của compensating transaction:
- **Idempotent**: Gọi nhiều lần kết quả vẫn như nhau
- **Reversible**: Hoàn tác được effect của bước gốc

### 2.4 Saga States

```
PENDING → EXECUTING → COMPLETED
                   ↘ FAILED → COMPENSATING → COMPENSATED
```

---

## 3. Cấu Hình Cần Thiết

### 3.1 Cấu Trúc Demo

**Kịch bản:** Khách hàng đặt hàng → hệ thống tự động:
1. Tạo Order (PENDING)
2. Reserve Inventory
3. Process Payment
4. Confirm Order (COMPLETED)

Nếu bất kỳ bước nào thất bại → rollback toàn bộ qua compensating transactions.

```
demo-chapter6/
├── order-service/          ← Quản lý đơn hàng + chứa Orchestrator
│   └── src/main/
│       ├── java/.../
│       │   ├── OrderSagaOrchestrator.java
│       │   ├── OrderService.java
│       │   └── OrderController.java
│       └── resources/
│           └── application.yml
├── inventory-service/      ← Quản lý tồn kho
│   └── src/main/
│       ├── java/.../
│       │   └── InventoryController.java
│       └── resources/
│           └── application.yml
└── payment-service/        ← Xử lý thanh toán
    └── src/main/
        ├── java/.../
        │   └── PaymentController.java
        └── resources/
            └── application.yml
```

> **Demo này dùng HTTP (RestTemplate)** để giao tiếp giữa services — đơn giản, không cần cài thêm message broker. Trong production thường dùng Kafka/RabbitMQ.

### 3.2 Dependencies cho Mỗi Service

Tất cả 3 service dùng chung dependencies (`pom.xml`):

```xml
<dependencies>
    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- H2 Database (in-memory, tiện cho demo) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

### 3.3 Cấu Hình application.yml

**order-service** (port 8080):
```yaml
server:
  port: 8080
spring:
  application:
    name: order-service
  datasource:
    url: jdbc:h2:mem:orderdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console

# URL của các service khác
inventory:
  service:
    url: http://localhost:8081
payment:
  service:
    url: http://localhost:8082
```

**inventory-service** (port 8081):
```yaml
server:
  port: 8081
spring:
  application:
    name: inventory-service
  datasource:
    url: jdbc:h2:mem:inventorydb
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

**payment-service** (port 8082):
```yaml
server:
  port: 8082
spring:
  application:
    name: payment-service
  datasource:
    url: jdbc:h2:mem:paymentdb
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

---

## 4. Hướng Dẫn Step-by-Step

> **Lưu ý:** Tạo 3 project riêng tại [start.spring.io](https://start.spring.io), mở mỗi project trong **một cửa sổ IntelliJ riêng**.

---

### Step 1: Tạo Inventory Service

**Tạo project:** Artifact = `inventory-service`, Dependencies: **Spring Web, Spring Data JPA, H2**

**`Inventory.java`** (Entity):
```java
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;
    private int quantity;
    private String reservedFor;  // orderId đang giữ chỗ

    // getters, setters
}
```

**`InventoryRepository.java`:**
```java
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(String productId);
}
```

**`InventoryController.java`:**
```java
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryRepository inventoryRepository;

    // Khởi tạo dữ liệu test
    @PostConstruct
    public void init() {
        Inventory inv = new Inventory();
        inv.setProductId("PROD-001");
        inv.setQuantity(10);
        inventoryRepository.save(inv);
    }

    // Reserve tồn kho
    @PostMapping("/reserve")
    public ResponseEntity<String> reserve(@RequestParam String productId,
                                          @RequestParam int quantity,
                                          @RequestParam String orderId) {
        Optional<Inventory> opt = inventoryRepository.findByProductId(productId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Product not found");

        Inventory inv = opt.get();
        if (inv.getQuantity() < quantity) {
            return ResponseEntity.badRequest().body("Insufficient stock");
        }

        inv.setQuantity(inv.getQuantity() - quantity);
        inv.setReservedFor(orderId);
        inventoryRepository.save(inv);
        return ResponseEntity.ok("Reserved successfully");
    }

    // Compensating: release tồn kho (rollback)
    @PostMapping("/release")
    public ResponseEntity<String> release(@RequestParam String productId,
                                          @RequestParam int quantity,
                                          @RequestParam String orderId) {
        Optional<Inventory> opt = inventoryRepository.findByProductId(productId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Product not found");

        Inventory inv = opt.get();
        inv.setQuantity(inv.getQuantity() + quantity);
        inv.setReservedFor(null);
        inventoryRepository.save(inv);
        return ResponseEntity.ok("Released successfully");
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Inventory> check(@PathVariable String productId) {
        return inventoryRepository.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

#### ✅ Checklist Step 1
- [ ] Chạy inventory-service → console hiển thị `Started InventoryServiceApplication on port 8081`
- [ ] Mở `http://localhost:8081/inventory/check/PROD-001` → trả về JSON với `quantity: 10`
- [ ] Mở `http://localhost:8081/h2-console` → đăng nhập với URL `jdbc:h2:mem:inventorydb` → thấy bảng INVENTORY

---

### Step 2: Tạo Payment Service

**Tạo project:** Artifact = `payment-service`, Dependencies: **Spring Web, Spring Data JPA, H2**

**`Payment.java`** (Entity):
```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private double amount;
    private String status;  // SUCCESS, REFUNDED
    private LocalDateTime createdAt;

    // getters, setters
}
```

**`PaymentRepository.java`:**
```java
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
}
```

**`PaymentController.java`:**
```java
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    // Process payment
    @PostMapping("/process")
    public ResponseEntity<String> process(@RequestParam String orderId,
                                          @RequestParam double amount) {
        // Giả lập: amount > 1000 thì từ chối (để test compensation)
        if (amount > 1000) {
            return ResponseEntity.badRequest().body("Payment declined: amount exceeds limit");
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return ResponseEntity.ok("Payment processed successfully");
    }

    // Compensating: refund (rollback)
    @PostMapping("/refund")
    public ResponseEntity<String> refund(@RequestParam String orderId) {
        Optional<Payment> opt = paymentRepository.findByOrderId(orderId);
        if (opt.isEmpty()) return ResponseEntity.ok("No payment to refund");

        Payment payment = opt.get();
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
        return ResponseEntity.ok("Refunded successfully");
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Payment> status(@PathVariable String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

#### ✅ Checklist Step 2
- [ ] Chạy payment-service → console hiển thị `Started PaymentServiceApplication on port 8082`
- [ ] Gọi `POST http://localhost:8082/payment/process?orderId=TEST-001&amount=100` → trả về `Payment processed successfully`
- [ ] Gọi `POST http://localhost:8082/payment/process?orderId=TEST-002&amount=9999` → trả về `Payment declined: amount exceeds limit`

---

### Step 3: Tạo Order Service + Saga Orchestrator

**Tạo project:** Artifact = `order-service`, Dependencies: **Spring Web, Spring Data JPA, H2**

**`Order.java`** (Entity):
```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;  // dùng UUID

    private String productId;
    private int quantity;
    private double amount;
    private String status;   // PENDING, COMPLETED, FAILED, COMPENSATED

    // getters, setters
}
```

**`OrderRepository.java`:**
```java
public interface OrderRepository extends JpaRepository<Order, String> {}
```

**`OrderSagaOrchestrator.java`** ← *Đây là trái tim của Saga Pattern*:
```java
@Service
public class OrderSagaOrchestrator {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryUrl;

    @Value("${payment.service.url}")
    private String paymentUrl;

    public Order executeSaga(Order order) {
        // === BƯỚC 1: Tạo Order với trạng thái PENDING ===
        order.setId(UUID.randomUUID().toString());
        order.setStatus("PENDING");
        orderRepository.save(order);
        System.out.println("[SAGA] Order created: " + order.getId() + " - Status: PENDING");

        // === BƯỚC 2: Reserve Inventory ===
        boolean inventoryReserved = false;
        try {
            String url = inventoryUrl + "/inventory/reserve?productId=" + order.getProductId()
                    + "&quantity=" + order.getQuantity()
                    + "&orderId=" + order.getId();
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            inventoryReserved = response.getStatusCode().is2xxSuccessful();
            System.out.println("[SAGA] Inventory reserve: " + response.getBody());
        } catch (Exception e) {
            System.out.println("[SAGA] Inventory reserve FAILED: " + e.getMessage());
        }

        if (!inventoryReserved) {
            order.setStatus("FAILED");
            orderRepository.save(order);
            System.out.println("[SAGA] Saga FAILED at inventory step - no compensation needed");
            return order;
        }

        // === BƯỚC 3: Process Payment ===
        boolean paymentSuccess = false;
        try {
            String url = paymentUrl + "/payment/process?orderId=" + order.getId()
                    + "&amount=" + order.getAmount();
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            paymentSuccess = response.getStatusCode().is2xxSuccessful();
            System.out.println("[SAGA] Payment: " + response.getBody());
        } catch (Exception e) {
            System.out.println("[SAGA] Payment FAILED: " + e.getMessage());
        }

        if (!paymentSuccess) {
            // === COMPENSATION: Payment thất bại → Release Inventory ===
            System.out.println("[SAGA] Payment failed! Starting compensation...");
            order.setStatus("COMPENSATING");
            orderRepository.save(order);

            try {
                String url = inventoryUrl + "/inventory/release?productId=" + order.getProductId()
                        + "&quantity=" + order.getQuantity()
                        + "&orderId=" + order.getId();
                restTemplate.postForEntity(url, null, String.class);
                System.out.println("[SAGA] Inventory released (compensation done)");
            } catch (Exception e) {
                System.out.println("[SAGA] Compensation FAILED: " + e.getMessage());
            }

            order.setStatus("COMPENSATED");
            orderRepository.save(order);
            System.out.println("[SAGA] Saga COMPENSATED");
            return order;
        }

        // === BƯỚC 4: Hoàn tất ===
        order.setStatus("COMPLETED");
        orderRepository.save(order);
        System.out.println("[SAGA] Saga COMPLETED successfully!");
        return order;
    }
}
```

**`OrderController.java`:**
```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderSagaOrchestrator sagaOrchestrator;

    @Autowired
    private OrderRepository orderRepository;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Đặt hàng → kích hoạt Saga
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order result = sagaOrchestrator.executeSaga(order);
        return ResponseEntity.ok(result);
    }

    // Xem trạng thái đơn hàng
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
```

#### ✅ Checklist Step 3
- [ ] Chạy order-service → console hiển thị `Started OrderServiceApplication on port 8080`
- [ ] Cả 3 service đang chạy cùng lúc (3 cửa sổ IntelliJ)
- [ ] Mở `http://localhost:8080/orders` → trả về `[]` (danh sách rỗng)

---

### Step 4: Chạy Demo Saga – Happy Path (Thành Công)

Dùng **PowerShell** hoặc công cụ test API (Postman, cài tại [postman.com](https://www.postman.com)):

```powershell
# Đặt hàng hợp lệ (tồn kho đủ, số tiền < 1000)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/orders" `
  -ContentType "application/json" `
  -Body '{"productId":"PROD-001","quantity":2,"amount":200}'
```

Hoặc dùng **curl** trong CMD:
```cmd
curl -X POST http://localhost:8080/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"productId\":\"PROD-001\",\"quantity\":2,\"amount\":200}"
```

**Quan sát console của order-service:**
```
[SAGA] Order created: abc-123 - Status: PENDING
[SAGA] Inventory reserve: Reserved successfully
[SAGA] Payment: Payment processed successfully
[SAGA] Saga COMPLETED successfully!
```

**Kiểm tra kết quả:**
```cmd
curl http://localhost:8080/orders
curl http://localhost:8081/inventory/check/PROD-001
curl http://localhost:8082/payment/status/abc-123
```

#### ✅ Checklist Step 4 – Happy Path
- [ ] Response trả về Order với `"status": "COMPLETED"`
- [ ] Console order-service hiển thị đủ 4 dòng `[SAGA]` theo thứ tự: PENDING → reserve → payment → COMPLETED
- [ ] Inventory `PROD-001` giảm quantity đi 2 (từ 10 xuống 8)
- [ ] Payment service có record với `status: SUCCESS`

---

### Step 5: Chạy Demo Saga – Compensation (Payment Thất Bại)

```powershell
# Đặt hàng với số tiền > 1000 → payment sẽ bị từ chối → compensation chạy
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/orders" `
  -ContentType "application/json" `
  -Body '{"productId":"PROD-001","quantity":1,"amount":9999}'
```

**Quan sát console của order-service:**
```
[SAGA] Order created: xyz-456 - Status: PENDING
[SAGA] Inventory reserve: Reserved successfully
[SAGA] Payment: Payment declined: amount exceeds limit
[SAGA] Payment failed! Starting compensation...
[SAGA] Inventory released (compensation done)
[SAGA] Saga COMPENSATED
```

#### ✅ Checklist Step 5 – Compensation
- [ ] Response trả về Order với `"status": "COMPENSATED"`
- [ ] Console hiển thị đủ luồng: reserve → payment fail → compensation → COMPENSATED
- [ ] Inventory `PROD-001` quantity **không thay đổi** (đã release sau khi compensation)
- [ ] Không có payment record trong payment-service (hoặc có record DECLINED)

---

### Step 6: Chạy Demo Saga – Inventory Thất Bại

```powershell
# Đặt hàng số lượng nhiều hơn tồn kho (còn 8, đặt 100)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/orders" `
  -ContentType "application/json" `
  -Body '{"productId":"PROD-001","quantity":100,"amount":500}'
```

**Quan sát console:**
```
[SAGA] Order created: ... - Status: PENDING
[SAGA] Inventory reserve: Insufficient stock
[SAGA] Saga FAILED at inventory step - no compensation needed
```

#### ✅ Checklist Step 6 – Inventory Fail
- [ ] Response trả về Order với `"status": "FAILED"`
- [ ] Không có bước compensation vì payment chưa chạy
- [ ] Inventory quantity không thay đổi
- [ ] Không có payment record nào được tạo

---

### Step 7: Thứ Tự Khởi Động

```
1. inventory-service  (port 8081)
2. payment-service    (port 8082)
3. order-service      (port 8080)  ← phải chạy sau (cần gọi đến 2 service trên)
```

Dùng **Services window** trong IntelliJ để quản lý: View → Tool Windows → Services

---

## 5. Tóm Tắt Saga Pattern

### Luồng Happy Path
```
Client → POST /orders
  → Orchestrator: tạo Order [PENDING]
  → Inventory: reserve [OK]
  → Payment: process [OK]
  → Order [COMPLETED]
```

### Luồng Compensation
```
Client → POST /orders
  → Orchestrator: tạo Order [PENDING]
  → Inventory: reserve [OK]
  → Payment: process [FAILED]
  → Order [COMPENSATING]
  → Inventory: release (undo bước reserve)
  → Order [COMPENSATED]
```

### So Sánh Choreography vs Orchestration

| | Choreography | Orchestration |
|---|---|---|
| Điều phối | Phi tập trung (events) | Tập trung (Orchestrator) |
| Debug | Khó theo dõi flow | Dễ debug, log tập trung |
| Coupling | Loose coupling | Service phụ thuộc Orchestrator |
| Phù hợp | Flow đơn giản | Flow phức tạp, nhiều bước |

### Annotation / Class quan trọng

| Thành phần | Vai trò |
|---|---|
| `OrderSagaOrchestrator` | Điều phối toàn bộ saga workflow |
| `RestTemplate` | Gọi HTTP đến các service |
| `@Transactional` | Bảo vệ local transaction trong từng service |
| `@PostConstruct` | Khởi tạo dữ liệu test khi service start |
| Compensating method | Undo effect của bước gốc khi thất bại |

---

## 6. TODOs – Tính Năng Nâng Cao

> Sau khi hoàn thành demo cơ bản, thực hiện 5 TODO dưới đây để nâng cao chất lượng hệ thống.

---

### TODO 1: Saga State Persistence – Lưu Trạng Thái Saga vào Database

**Vấn đề hiện tại:** Nếu order-service restart giữa chừng, toàn bộ trạng thái saga mất → không biết đang ở bước nào, không thể tiếp tục hay compensation.

**Giải pháp:** Tạo bảng `saga_state` lưu từng bước saga, cập nhật sau mỗi action.

**Thêm Entity `SagaState.java` vào order-service:**
```java
@Entity
@Table(name = "saga_state")
public class SagaState {
    @Id
    private String sagaId;        // = orderId

    private String currentStep;   // INIT, INVENTORY_RESERVED, PAYMENT_DONE
    private String status;        // EXECUTING, COMPLETED, COMPENSATING, COMPENSATED
    private String failureReason;
    private LocalDateTime updatedAt;

    // getters, setters
}
```

**`SagaStateRepository.java`:**
```java
public interface SagaStateRepository extends JpaRepository<SagaState, String> {}
```

**Cập nhật `OrderSagaOrchestrator.java`** — inject `SagaStateRepository` và ghi trạng thái sau mỗi bước:
```java
@Autowired
private SagaStateRepository sagaStateRepository;

// Trong executeSaga(), sau khi tạo order:
SagaState state = new SagaState();
state.setSagaId(order.getId());
state.setCurrentStep("INIT");
state.setStatus("EXECUTING");
state.setUpdatedAt(LocalDateTime.now());
sagaStateRepository.save(state);

// Sau khi reserve inventory thành công:
state.setCurrentStep("INVENTORY_RESERVED");
state.setUpdatedAt(LocalDateTime.now());
sagaStateRepository.save(state);

// Sau khi payment thành công:
state.setCurrentStep("PAYMENT_DONE");
state.setStatus("COMPLETED");
state.setUpdatedAt(LocalDateTime.now());
sagaStateRepository.save(state);

// Khi compensation:
state.setStatus("COMPENSATING");
state.setFailureReason("Payment declined");
sagaStateRepository.save(state);
// ... sau khi compensation xong:
state.setStatus("COMPENSATED");
sagaStateRepository.save(state);
```

**Thêm endpoint xem saga state:**
```java
// Trong OrderController.java
@Autowired
private SagaStateRepository sagaStateRepository;

@GetMapping("/{id}/saga-state")
public ResponseEntity<SagaState> getSagaState(@PathVariable String id) {
    return sagaStateRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

#### ✅ Checklist TODO 1
- [ ] Bảng `SAGA_STATE` xuất hiện trong H2 Console (`http://localhost:8080/h2-console`)
- [ ] Sau khi đặt hàng thành công: `GET /orders/{id}/saga-state` → `currentStep: PAYMENT_DONE`, `status: COMPLETED`
- [ ] Sau khi saga compensation: `status: COMPENSATED`, `failureReason` có nội dung
- [ ] Mỗi bước thay đổi thì `updatedAt` cũng được cập nhật

---

### TODO 2: Retry Tự Động với Spring Retry

**Vấn đề hiện tại:** Nếu inventory-service hoặc payment-service tạm thời không phản hồi (network glitch), saga lập tức thất bại — dù chỉ là lỗi thoáng qua.

**Giải pháp:** Dùng **Spring Retry** để tự động thử lại 3 lần trước khi kết luận thất bại.

**Thêm dependency vào `pom.xml` của order-service:**
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

**Bật Spring Retry trong main class:**
```java
@SpringBootApplication
@EnableRetry   // ← thêm annotation này
public class OrderServiceApplication { ... }
```

**Tạo `InventoryClient.java`** — tách việc gọi HTTP ra một class riêng để gắn `@Retryable`:
```java
@Service
public class InventoryClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryUrl;

    @Retryable(
        retryFor = { ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)  // 1s, 2s, 4s
    )
    public boolean reserve(String productId, int quantity, String orderId) {
        String url = inventoryUrl + "/inventory/reserve?productId=" + productId
                + "&quantity=" + quantity + "&orderId=" + orderId;
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    @Recover  // gọi khi hết retry vẫn lỗi
    public boolean recoverReserve(ResourceAccessException e, String productId,
                                   int quantity, String orderId) {
        System.out.println("[RETRY] Inventory reserve failed after 3 attempts: " + e.getMessage());
        return false;
    }

    @Retryable(retryFor = { ResourceAccessException.class }, maxAttempts = 3,
               backoff = @Backoff(delay = 1000))
    public boolean release(String productId, int quantity, String orderId) {
        String url = inventoryUrl + "/inventory/release?productId=" + productId
                + "&quantity=" + quantity + "&orderId=" + orderId;
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }
}
```

**Cập nhật `OrderSagaOrchestrator`** để dùng `InventoryClient` thay vì gọi RestTemplate trực tiếp:
```java
@Autowired
private InventoryClient inventoryClient;

// Thay đoạn gọi inventory bằng:
boolean inventoryReserved = inventoryClient.reserve(
    order.getProductId(), order.getQuantity(), order.getId()
);
```

#### ✅ Checklist TODO 2
- [ ] Tắt inventory-service, đặt hàng → console hiển thị `[RETRY]` và thử lại 3 lần trước khi fail
- [ ] Tắt inventory-service rồi bật lại trong lúc retry đang chờ → saga tiếp tục thành công
- [ ] Khoảng cách giữa các lần retry tăng dần (1s → 2s → 4s) theo cấu hình backoff
- [ ] Sau 3 lần thất bại → `@Recover` được gọi, log `[RETRY] ... failed after 3 attempts`

---

### TODO 3: Idempotency – Chống Gọi Trùng Lặp

**Vấn đề hiện tại:** Client có thể gửi request đặt hàng 2 lần (do network timeout, user double-click) → tạo ra 2 đơn hàng trùng nhau, trừ tiền 2 lần.

**Giải pháp:** Dùng **Idempotency Key** — client gửi kèm một key duy nhất trong header, server kiểm tra nếu đã xử lý key đó thì trả về kết quả cũ.

**Thêm Entity `IdempotencyRecord.java`** vào order-service:
```java
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {
    @Id
    private String idempotencyKey;

    private String orderId;
    private String responseStatus;
    private LocalDateTime createdAt;

    // getters, setters
}
```

**`IdempotencyRepository.java`:**
```java
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {}
```

**Cập nhật `OrderController.java`:**
```java
@Autowired
private IdempotencyRepository idempotencyRepository;

@PostMapping
public ResponseEntity<?> createOrder(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody Order order) {

    // Kiểm tra nếu key đã được xử lý
    if (idempotencyKey != null) {
        Optional<IdempotencyRecord> existing = idempotencyRepository.findById(idempotencyKey);
        if (existing.isPresent()) {
            System.out.println("[IDEMPOTENCY] Duplicate request detected, returning cached result");
            return orderRepository.findById(existing.get().getOrderId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    // Xử lý bình thường
    Order result = sagaOrchestrator.executeSaga(order);

    // Lưu idempotency record
    if (idempotencyKey != null) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setOrderId(result.getId());
        record.setResponseStatus(result.getStatus());
        record.setCreatedAt(LocalDateTime.now());
        idempotencyRepository.save(record);
    }

    return ResponseEntity.ok(result);
}
```

**Test với PowerShell:**
```powershell
# Gửi lần 1
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/orders" `
  -Headers @{"Idempotency-Key"="unique-key-abc-001"} `
  -ContentType "application/json" `
  -Body '{"productId":"PROD-001","quantity":1,"amount":100}'

# Gửi lần 2 với cùng key → phải trả về đúng order cũ, không tạo mới
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/orders" `
  -Headers @{"Idempotency-Key"="unique-key-abc-001"} `
  -ContentType "application/json" `
  -Body '{"productId":"PROD-001","quantity":1,"amount":100}'
```

#### ✅ Checklist TODO 3
- [ ] Gửi 2 request với cùng `Idempotency-Key` → chỉ có **1 order** được tạo trong DB
- [ ] Request thứ 2 trả về **cùng orderId** với request đầu
- [ ] Console hiển thị `[IDEMPOTENCY] Duplicate request detected` ở lần gọi thứ 2
- [ ] Inventory chỉ bị trừ **1 lần** dù gọi 2 lần
- [ ] Bảng `IDEMPOTENCY_RECORDS` trong H2 Console có 1 record với key tương ứng

---

### TODO 4: Circuit Breaker với Resilience4j

**Vấn đề hiện tại:** Nếu payment-service bị down liên tục, mỗi request vẫn chờ timeout rồi mới fail → lãng phí tài nguyên, làm chậm toàn hệ thống (cascade failure).

**Giải pháp:** **Circuit Breaker** — sau N lần thất bại liên tiếp, tự động "mở mạch" (OPEN state), không gọi service nữa, trả về fallback ngay lập tức. Sau một thời gian thử lại (HALF-OPEN).

**Thêm dependency vào `pom.xml` của order-service:**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**Cấu hình Circuit Breaker trong `application.yml`:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 5          # Xét 5 request gần nhất
        failure-rate-threshold: 60      # Mở mạch nếu >60% thất bại
        wait-duration-in-open-state: 10s  # Chờ 10s trước khi thử lại
        permitted-number-of-calls-in-half-open-state: 2
        register-health-indicator: true
```

**Tạo `PaymentClient.java`:**
```java
@Service
public class PaymentClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${payment.service.url}")
    private String paymentUrl;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public boolean processPayment(String orderId, double amount) {
        String url = paymentUrl + "/payment/process?orderId=" + orderId + "&amount=" + amount;
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    // Fallback khi circuit OPEN hoặc exception
    public boolean paymentFallback(String orderId, double amount, Exception e) {
        System.out.println("[CIRCUIT BREAKER] Payment service unavailable, fallback triggered: "
                + e.getMessage());
        return false;  // Saga sẽ chạy compensation
    }
}
```

**Cập nhật `OrderSagaOrchestrator`** dùng `PaymentClient`.

**Xem trạng thái Circuit Breaker** qua Actuator:
```
http://localhost:8080/actuator/circuitbreakers
http://localhost:8080/actuator/health
```

> Thêm vào `application.yml` để bật endpoint:
> ```yaml
> management:
>   endpoints:
>     web:
>       exposure:
>         include: health, circuitbreakers
>   health:
>     circuitbreakers:
>       enabled: true
> ```

#### ✅ Checklist TODO 4
- [ ] Tắt payment-service, gọi 5 request liên tiếp → Circuit Breaker chuyển sang **OPEN**
- [ ] Khi OPEN: request mới trả về ngay lập tức (không chờ timeout), log hiển thị `[CIRCUIT BREAKER] fallback triggered`
- [ ] `GET http://localhost:8080/actuator/health` → thấy `paymentService: DOWN` hoặc `OPEN`
- [ ] Bật lại payment-service, chờ 10s → Circuit Breaker chuyển sang **HALF-OPEN** rồi **CLOSED**
- [ ] Sau khi CLOSED: request xử lý bình thường trở lại

---

### TODO 5: Distributed Tracing với Spring Boot Actuator + Logging Correlation ID

**Vấn đề hiện tại:** Khi saga chạy qua 3 service, log nằm rải rác ở 3 console khác nhau, không biết request nào tương ứng với nhau → rất khó debug.

**Giải pháp:** Gắn **Correlation ID** (= orderId hoặc một UUID) vào header của mọi HTTP call giữa các service, và log ID này ở mỗi service để có thể trace toàn bộ flow.

**Thêm dependency vào `pom.xml` của **cả 3 service**:**
```xml
<!-- Micrometer Tracing (Spring Boot 3.x) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Tạo `CorrelationIdFilter.java`** trong **inventory-service và payment-service** (nhận và log Correlation ID):
```java
@Component
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) correlationId = UUID.randomUUID().toString();

        // Gắn vào MDC để tự động xuất hiện trong mọi log
        MDC.put("correlationId", correlationId);
        System.out.println("[TRACE] Request received - CorrelationId: " + correlationId
                + " | URI: " + httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

**Cập nhật `OrderSagaOrchestrator`** — truyền Correlation ID trong mọi HTTP call:
```java
private ResponseEntity<String> callWithCorrelation(String url, String correlationId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Correlation-ID", correlationId);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
}

// Trong executeSaga(), dùng orderId làm correlationId:
String correlationId = order.getId();

// Thay vì:
// restTemplate.postForEntity(url, null, String.class);
// Dùng:
// callWithCorrelation(url + "?...", correlationId);
```

**Cấu hình log pattern** trong `application.yml` của cả 3 service:
```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss} [%X{correlationId}] %-5level %logger{36} - %msg%n"
```

**Test và quan sát:**
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/orders" `
  -ContentType "application/json" `
  -Body '{"productId":"PROD-001","quantity":1,"amount":150}'
```

Tìm orderId trong response, sau đó **grep** cùng ID đó trong log của cả 3 service để thấy toàn bộ flow:

```
# order-service console:
14:05:01 [abc-123] INFO  OrderSagaOrchestrator - [SAGA] Order created: abc-123 - Status: PENDING

# inventory-service console:
14:05:01 [abc-123] INFO  CorrelationIdFilter  - [TRACE] Request received - CorrelationId: abc-123

# payment-service console:
14:05:02 [abc-123] INFO  CorrelationIdFilter  - [TRACE] Request received - CorrelationId: abc-123
```

#### ✅ Checklist TODO 5
- [ ] Log của cả 3 service đều hiển thị cùng một **correlationId** cho một request đặt hàng
- [ ] Format log có `[correlationId]` ở giữa timestamp và level
- [ ] Khi saga compensation xảy ra, correlationId vẫn xuất hiện nhất quán trong log release/refund
- [ ] Có thể tìm toàn bộ flow của 1 order chỉ bằng cách search correlationId trong log

---

## 7. Lộ Trình Thực Hiện TODOs

| Thứ tự | TODO | Độ khó | Thời gian ước tính |
|---|---|---|---|
| 1 | Saga State Persistence | ⭐⭐ | 30 phút |
| 2 | Retry với Spring Retry | ⭐⭐ | 45 phút |
| 3 | Idempotency Key | ⭐⭐ | 30 phút |
| 4 | Circuit Breaker Resilience4j | ⭐⭐⭐ | 60 phút |
| 5 | Distributed Tracing / Correlation ID | ⭐⭐⭐ | 60 phút |

> **Gợi ý:** Làm theo thứ tự trên — TODO 1, 2, 3 độc lập nhau, có thể làm riêng lẻ. TODO 4 và 5 nên làm sau khi đã ổn định phần cơ bản.
