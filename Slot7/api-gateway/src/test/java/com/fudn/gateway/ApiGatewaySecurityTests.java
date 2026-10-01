package com.fudn.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.product.url", "services.order.url", "services.inventory.url"}))
class ApiGatewaySecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/product"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtRoutesProductRequest() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/product"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"iPhone 15\"}]")));

        mockMvc.perform(get("/api/product").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"name\":\"iPhone 15\"}]"));

        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api/product")));
    }

    @Test
    void validJwtRoutesOrderRequest() throws Exception {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/order"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withBody("Order Placed Successfully")));

        mockMvc.perform(post("/api/order")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"iphone_15\",\"price\":1000,\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));
    }
}
