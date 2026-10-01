package com.fudn.gateway.routes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration(proxyBeanMethods = false)
public class Routes {

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${services.order.url}")
    private String orderServiceUrl;

    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;

    // ==========================================================
    // TODO GW-1 – Định nghĩa 3 routing rules
    // ----------------------------------------------------------
    // YÊU CẦU:
    //   Tạo 3 @Bean RouterFunction<ServerResponse>, mỗi bean route
    //   một đường dẫn tới đúng microservice:
    //
    //   Route 1 – Product Service (port 8080)
    //     ID     : "product_service"
    //     Path   : /api/product
    //     Target : http://localhost:8080
    //
    //   Route 2 – Order Service (port 8081)
    //     ID     : "order_service"
    //     Path   : /api/order
    //     Target : http://localhost:8081
    //
    //   Route 3 – Inventory Service (port 8082)
    //     ID     : "inventory_service"
    //     Path   : /api/inventory
    //     Target : http://localhost:8082
    //
    // GỢI Ý CÚ PHÁP:
    //   @Bean
    //   public RouterFunction<ServerResponse> productServiceRoute() {
    //       return route("product_service")
    //               .route(path("/api/product"), http("http://localhost:8080"))
    //               .build();
    //   }
    // ==========================================================

    @Bean
    public RouterFunction<ServerResponse> productServiceRoute() {
        return route("product_service")
                .route(path("/api/product/**"), http())
                .before(uri(productServiceUrl))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoute() {
        return route("order_service")
                .route(path("/api/order/**"), http())
                .before(uri(orderServiceUrl))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceRoute() {
        return route("inventory_service")
                .route(path("/api/inventory/**"), http())
                .before(uri(inventoryServiceUrl))
                .build();
    }
}
