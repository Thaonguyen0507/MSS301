package com.fudn.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerIntegrationTest {
    @ServiceConnection
    static MongoDBContainer database = new MongoDBContainer("mongo:7.0.5");
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
                .body("info.title", equalTo("Product Service API"))
                .body("info.version", equalTo("v0.0.1"))
                .body("paths", hasKey("/api/product"))
                .body("servers[0].url", equalTo("/"))
                .body("components.securitySchemes.bearerAuth.scheme", equalTo("bearer"));
    }

    @Test
    void corsPreflightShouldAllowGatewayOrigin() {
        given().port(port).header("Origin", "http://localhost:9000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type")
                .options("/api/product")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:9000"));
    }
}
