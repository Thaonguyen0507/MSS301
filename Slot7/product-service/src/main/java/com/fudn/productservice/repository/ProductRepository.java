package com.fudn.productservice.repository;

import com.fudn.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

// ==========================================================
// TODO PS-2 – Khai báo ProductRepository
// ----------------------------------------------------------
// YÊU CẦU:
//   Kế thừa MongoRepository<Product, String>
//   Spring Data tự cung cấp: save(), findAll(), findById(), delete()...
//
// GỢI Ý:
//   import org.springframework.data.mongodb.repository.MongoRepository;
//   public interface ProductRepository extends MongoRepository<Product, String> { }
// ==========================================================

public interface ProductRepository extends MongoRepository<Product, String> {

}
