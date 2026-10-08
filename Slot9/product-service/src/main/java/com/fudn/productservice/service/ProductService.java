package com.fudn.productservice.service;

import com.fudn.productservice.dto.ProductRequest;
import com.fudn.productservice.dto.ProductResponse;
import com.fudn.productservice.model.Product;
import com.fudn.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // ==========================================================
    // TODO PS-5 – Implement createProduct()
    // ----------------------------------------------------------
    // YÊU CẦU:
    //   1. Chuyển ProductRequest → Product entity (dùng builder pattern)
    //   2. Lưu vào MongoDB: productRepository.save(product)
    //   3. Ghi log: log.info("Product {} is saved", product.getId())
    // ==========================================================
    public void createProduct(ProductRequest productRequest) {
        Product product = Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .build();
        
        productRepository.save(product);
        log.info("Product {} is saved", product.getId());
    }

    // ==========================================================
    // TODO PS-6 – Implement getAllProducts()
    // ----------------------------------------------------------
    // YÊU CẦU:
    //   1. Lấy tất cả Product từ DB: productRepository.findAll()
    //   2. Chuyển List<Product> → List<ProductResponse> bằng stream + map
    //   3. Trả về list kết quả
    // ==========================================================
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice()
                ))
                .toList();
    }
}
