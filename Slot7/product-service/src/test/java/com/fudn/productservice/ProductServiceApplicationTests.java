package com.fudn.productservice;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration Test – Product Service
 *
 * Chạy test:
 *   cd product-service
 *   ./mvnw test
 *
 * Yêu cầu: Docker phải đang chạy (để Testcontainer kéo MongoDB image)
 * Keycloak KHÔNG cần thiết – test chạy độc lập.
 *
 * Test PASS khi tất cả TODO PS-1 đến PS-8 đã được implement đúng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

    static {
        mongoDBContainer.start();
    }

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void shouldCreateProduct() {
        String requestBody = """
                {
                    "name": "iPhone 15",
                    "description": "Apple smartphone",
                    "price": 1000
                }
                """;

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/api/product")
        .then()
            .log().all()
            .statusCode(201);
    }

    @Test
    void shouldGetAllProducts() {
        // Tạo 1 sản phẩm trước
        String requestBody = """
                {
                    "name": "Samsung Galaxy S24",
                    "description": "Android flagship",
                    "price": 900
                }
                """;

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/api/product");

        // Sau đó lấy danh sách
        given()
        .when()
            .get("/api/product")
        .then()
            .log().all()
            .statusCode(200)
            .body("$", not(empty()));
    }
}
