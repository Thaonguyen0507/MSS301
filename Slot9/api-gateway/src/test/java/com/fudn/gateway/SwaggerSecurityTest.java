package com.fudn.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.product.url", "services.order.url", "services.inventory.url"}))
class SwaggerSecurityTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private JwtDecoder decoder;

    @Test
    void swaggerUiShouldBeAccessibleWithoutToken() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void swaggerConfigShouldListThreeServices() throws Exception {
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls.length()").value(3));
    }

    @Test
    void aggregateDocsShouldBePublicAndRewritePath() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api-docs"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Mock service\",\"version\":\"v0.0.1\"},\"paths\":{}}")));
        for (String service : new String[]{"product-service", "order-service", "inventory-service"}) {
            mvc.perform(get("/aggregate/" + service + "/v3/api-docs"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Mock service"));
        }
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api-docs")));
    }

    @Test
    void protectedApiShouldRequireToken() throws Exception {
        mvc.perform(get("/api/product")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/order").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightShouldAllowAuthorizationHeader() throws Exception {
        mvc.perform(options("/api/product").header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}
