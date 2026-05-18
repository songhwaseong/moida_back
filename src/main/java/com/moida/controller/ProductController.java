package com.moida.controller;

import com.moida.common.request.ProductRequest;
import com.moida.common.response.ApiResponse;
import com.moida.domain.product.ProductService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 등록 (경매 / 중고거래 공통)
     * POST /api/products
     * Authorization: Bearer {JWT 토큰} 필요
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails  // JWT에서 로그인 유저 자동 추출
    ) {
        Long productId = productService.create(request, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(productId, "상품이 등록되었습니다."));
    }
}