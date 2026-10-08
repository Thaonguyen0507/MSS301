package com.fudn.orderservice;

import com.fudn.orderservice.stub.InventoryStubs;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration Test – Order Service
 *
 * Cách chạy:
 *   cd order-service
 *   ./mvnw test
 *
 * Yêu cầu:
 *   - Docker phải đang chạy
 *   - TODO OS-1 → OS-9 phải đã được implement
 *
 * WireMock (@AutoConfigureWireMock) tự động giả lập Inventory Service,
 * nên KHÔNG cần Inventory Service thật đang chạy khi test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "inventory.url"))
class OrderServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.0.36");

    static {
        mySQLContainer.start();
    }

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void shouldPlaceOrderWhenInStock() {
        // Giả lập Inventory Service: iphone_15 còn 1 cái
        InventoryStubs.stubInventoryCall("iphone_15", 1);

        String orderJson = """
                {
                    "skuCode": "iphone_15",
                    "price": 1000,
                    "quantity": 1
                }
                """;

        given()
            .contentType("application/json")
            .body(orderJson)
        .when()
            .post("/api/order")
        .then()
            .log().all()
            .statusCode(201)
            .body(is("Order Placed Successfully"));
    }

    @Test
    void shouldFailWhenOutOfStock() {
        // Giả lập Inventory Service: hết hàng
        InventoryStubs.stubInventoryCallOutOfStock("iphone_15", 500);

        String orderJson = """
                {
                    "skuCode": "iphone_15",
                    "price": 1000,
                    "quantity": 500
                }
                """;

        // Khi hết hàng, OrderService throw RuntimeException → Spring trả về 500
        given()
            .contentType("application/json")
            .body(orderJson)
        .when()
            .post("/api/order")
        .then()
            .log().all()
            .statusCode(500);
    }
}
