package com.fudn.productservice.dto;

import java.math.BigDecimal;

// ==========================================================
// TODO PS-4 – Khai báo ProductResponse (Java Record)
// ----------------------------------------------------------
// YÊU CẦU:
//   Tạo record chứa 4 field: id (String), name (String),
//   description (String), price (BigDecimal)
//   (Có thêm field "id" so với Request vì sau khi lưu MongoDB đã sinh id)
// ==========================================================

public record ProductResponse(String id, String name, String description, BigDecimal price) {

}
