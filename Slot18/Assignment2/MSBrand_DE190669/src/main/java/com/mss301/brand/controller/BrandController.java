package com.mss301.brand.controller;

import com.mss301.brand.dto.ApiResponse;
import com.mss301.brand.dto.BrandResponse;
import com.mss301.brand.dto.BrandWatchesResponse;
import com.mss301.brand.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping
    public ApiResponse<List<BrandResponse>> findAll() {
        return ApiResponse.success(brandService.findAll(), "Success");
    }

    @GetMapping("/{id}")
    public ApiResponse<BrandResponse> findById(@PathVariable Integer id) {
        return ApiResponse.success(brandService.findById(id), "Success");
    }

    @GetMapping("/{id}/watches")
    public ApiResponse<BrandWatchesResponse> watches(@PathVariable Integer id) {
        return ApiResponse.success(brandService.watchesOf(id), "Success");
    }
}
