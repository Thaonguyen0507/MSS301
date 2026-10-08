package com.fudn.productservice.controller;

import com.fudn.productservice.dto.ProductRequest;
import com.fudn.productservice.dto.ProductResponse;
import com.fudn.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==========================================================
    // TODO PS-7 – Endpoint tạo sản phẩm
    // ----------------------------------------------------------
    // YÊU CẦU:
    //   - HTTP method : POST
    //   - URL         : /api/product  (kế thừa từ @RequestMapping)
    //   - Body        : ProductRequest (@RequestBody)
    //   - Status      : 201 CREATED   (@ResponseStatus)
    //   - Gọi         : productService.createProduct(productRequest)
    // ==========================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@RequestBody ProductRequest productRequest) {
        productService.createProduct(productRequest);
    }


    // ==========================================================
    // TODO PS-8 – Endpoint lấy tất cả sản phẩm
    // ----------------------------------------------------------
    // YÊU CẦU:
    //   - HTTP method : GET
    //   - URL         : /api/product
    //   - Return      : List<ProductResponse>
    //   - Status      : 200 OK
    //   - Gọi         : productService.getAllProducts()
    // ==========================================================
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }
}
