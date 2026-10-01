package com.fudn.orderservice.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock stubs – giả lập Inventory Service trong integration test.
 * File này đã implement sẵn – sinh viên đọc để hiểu cách WireMock hoạt động.
 */
public class InventoryStubs {

    /**
     * Stub: khi có GET /api/inventory?skuCode={skuCode}&quantity={quantity}
     *       → trả về HTTP 200 body "true" (còn hàng)
     */
    public static void stubInventoryCall(String skuCode, Integer quantity) {
        stubFor(get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo(skuCode))
                .withQueryParam("quantity", equalTo(quantity.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
    }

    /**
     * Stub: giả lập Inventory Service trả về false (hết hàng)
     */
    public static void stubInventoryCallOutOfStock(String skuCode, Integer quantity) {
        stubFor(get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo(skuCode))
                .withQueryParam("quantity", equalTo(quantity.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));
    }
}
