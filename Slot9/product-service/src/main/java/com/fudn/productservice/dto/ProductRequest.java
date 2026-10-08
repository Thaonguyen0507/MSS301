package com.fudn.productservice.dto;

import java.math.BigDecimal;

// ==========================================================
// TODO PS-3 – Khai báo ProductRequest (Java Record)
// ----------------------------------------------------------
// YÊU CẦU:
//   Tạo record chứa 3 field: name (String), description (String), price (BigDecimal)
//   Record tự sinh constructor, getter, equals, hashCode – không cần Lombok.
//
// GỢI Ý:
//   public record ProductRequest(String name, String description, BigDecimal price) { }
// ==========================================================

public record ProductRequest(String name, String description, BigDecimal price) {

}
