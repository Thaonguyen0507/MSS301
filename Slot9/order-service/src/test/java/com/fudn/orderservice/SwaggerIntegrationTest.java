package com.fudn.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "inventory.url"))
class SwaggerIntegrationTest {
    @ServiceConnection
    static MySQLContainer database = new MySQLContainer("mysql:8.3.0");
    static { database.start(); }

    @LocalServerPort
    private int port;

    @Test
    void swaggerUiShouldBeAccessible() {
        given().port(port).redirects().follow(false).get("/swagger-ui.html")
                .then().statusCode(302).header("Location", containsString("/swagger-ui/index.html"));
        given().port(port).get("/swagger-ui/index.html")
                .then().statusCode(200).body(containsString("Swagger UI"));
    }

    @Test
    void apiDocsShouldReturnMetadataAndEndpoint() {
        given().port(port).get("/api-docs")
                .then().statusCode(200)
                .body("info.title", equalTo("Order Service API"))
                .body("info.version", equalTo("v0.0.1"))
                .body("paths", hasKey("/api/order"))
                .body("servers[0].url", equalTo("/"))
                .body("components.securitySchemes.bearerAuth.scheme", equalTo("bearer"));
    }

    @Test
    void corsPreflightShouldAllowGatewayOrigin() {
        given().port(port).header("Origin", "http://localhost:9000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type")
                .options("/api/order")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:9000"));
    }
}
