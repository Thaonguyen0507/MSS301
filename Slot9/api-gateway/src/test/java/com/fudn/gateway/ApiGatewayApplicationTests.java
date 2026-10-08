package com.fudn.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kiểm tra Spring context của API Gateway khởi động thành công.
 *
 * Cách chạy:
 *   cd api-gateway
 *   ./mvnw test
 *
 * Test PASS khi:
 *   - TODO GW-1: Routes.java đã khai báo đủ 3 routes
 *   - TODO GW-2: pom.xml đã có dependency oauth2-resource-server
 *   - TODO GW-3: application.properties đã có issuer-uri
 *   - TODO GW-4: SecurityConfig.java đã implement SecurityFilterChain
 *
 * Không cần Keycloak thật – application-test.properties override sang jwk-set-uri.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Context khởi động thành công → SecurityConfig và Routes đã đúng cú pháp
    }
}
