package com.fudn.productservice.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// ==========================================================
// TODO PS-1 – Hoàn thiện class Product (MongoDB Document)
// ----------------------------------------------------------
// YÊU CẦU:
//   1. Thêm các annotation Lombok: @Document, @AllArgsConstructor,
//      @NoArgsConstructor, @Builder, @Data
//   2. @Document(value = "product") – ánh xạ vào collection "product"
//   3. Khai báo các field:
//      - id        : String  (@Id – primary key, MongoDB tự sinh)
//      - name      : String
//      - description : String
//      - price     : BigDecimal  (dùng BigDecimal thay double để chính xác)
//
// GỢI Ý IMPORT:
//   import lombok.*;
//   import org.springframework.data.annotation.Id;
//   import org.springframework.data.mongodb.core.mapping.Document;
// ==========================================================

@Document(value = "product")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
}
