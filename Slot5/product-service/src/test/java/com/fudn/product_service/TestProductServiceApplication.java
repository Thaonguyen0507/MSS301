package com.fudn.product_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fudn.product_service.dto.ProductRequest;
import com.fudn.product_service.repository.IProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import com.fudn.product_service.model.Product;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class TestProductServiceApplication {

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	IProductRepository productRepository;

	@Autowired
	ObjectMapper objectMapper;

	@BeforeEach
	void cleanup() {
		productRepository.deleteAll();
	}

	@Test
	void shouldCreateProduct() throws Exception {
		ProductRequest productRequest = ProductRequest.builder()
				.name("Test Product")
				.description("This is a test product")
				.price(BigDecimal.valueOf(19.99))
				.build();

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(productRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.name").value("Test Product"))
				.andExpect(jsonPath("$.description").value("This is a test product"))
				.andExpect(jsonPath("$.price").value(19.99));

		assertThat(productRepository.findAll()).hasSize(1);
	}

	@Test
	void shouldUpdateProduct() throws Exception {

		Product product = Product.builder()
				.name("Old Product")
				.description("Old Description")
				.price(BigDecimal.valueOf(10))
				.build();

		Product savedProduct = productRepository.save(product);

		ProductRequest updateRequest = ProductRequest.builder()
				.name("Updated Product")
				.description("Updated Description")
				.price(BigDecimal.valueOf(20))
				.build();

		mockMvc.perform(put("/api/products/{id}", savedProduct.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updateRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(savedProduct.getId()))
				.andExpect(jsonPath("$.name").value("Updated Product"))
				.andExpect(jsonPath("$.description").value("Updated Description"))
				.andExpect(jsonPath("$.price").value(20));

		Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();

		assertThat(updatedProduct.getName()).isEqualTo("Updated Product");
	}

	@Test
	void shouldDeleteProduct() throws Exception {

		Product product = Product.builder()
				.name("Delete Product")
				.description("Delete Description")
				.price(BigDecimal.valueOf(50))
				.build();

		Product savedProduct = productRepository.save(product);

		mockMvc.perform(delete("/api/products/{id}", savedProduct.getId()))
				.andExpect(status().isNoContent());

		assertThat(productRepository.findById(savedProduct.getId())).isEmpty();
	}

	@Test
	void contextLoads() {
	}
}
