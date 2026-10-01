package com.fudn.inventoryservice;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test – Inventory Service
 *
 * Chạy test:
 *   cd inventory-service
 *   ./mvnw test
 *
 * Test PASS khi tất cả TODO IS-1 đến IS-4 đã được implement đúng.
 * Flyway migration V2 sẽ seed dữ liệu mẫu (100 iphone_15) vào DB test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

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
    void shouldReturnTrueWhenInStock() {
        // iphone_15 có 100 trong kho → yêu cầu 1 → phải trả true
        Boolean result = given()
            .when()
                .get("/api/inventory?skuCode=iphone_15&quantity=1")
            .then()
                .log().all()
                .statusCode(200)
                .extract().as(Boolean.class);

        assertTrue(result, "Phải trả true khi còn đủ hàng");
    }

    @Test
    void shouldReturnFalseWhenNotEnoughStock() {
        // iphone_15 chỉ có 100 → yêu cầu 1000 → phải trả false
        Boolean result = given()
            .when()
                .get("/api/inventory?skuCode=iphone_15&quantity=1000")
            .then()
                .log().all()
                .statusCode(200)
                .extract().as(Boolean.class);

        assertFalse(result, "Phải trả false khi không đủ hàng");
    }

    @Test
    void shouldReturnFalseForUnknownSku() {
        // SKU không tồn tại → phải trả false
        Boolean result = given()
            .when()
                .get("/api/inventory?skuCode=unknown_product&quantity=1")
            .then()
                .statusCode(200)
                .extract().as(Boolean.class);

        assertFalse(result, "Phải trả false khi SKU không tồn tại");
    }
}
