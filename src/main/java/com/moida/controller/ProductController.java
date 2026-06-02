package com.moida.controller;

import com.moida.common.request.ProductRequest;
import com.moida.common.request.ProductUpdateRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.MyBidResponse;
import com.moida.common.response.ProductDetailResponse;
import com.moida.common.response.ProductSummaryResponse;
import com.moida.domain.auction.AuctionBidService;
import com.moida.domain.product.ProductService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final AuctionBidService auctionBidService;

    // 홈 화면 상품 목록 조회용 공개 API.
    // status=LIVE → 실시간 경매, status=SCHEDULED → 경매 예정 매물
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "60") int size
    ) {
        log.info("[ProductController] GET /api/products category={}, status={}, sort={}, size={}", category, status, sort, size);
        List<ProductSummaryResponse> products = productService.getProducts(category, status, sort, size);
        log.info("[ProductController] GET /api/products responseCount={}", products.size());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ProductController] GET /api/products/me memberId={}", userDetails.getMemberId());
        List<ProductSummaryResponse> products = productService.getMyProducts(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getSellerProducts(
            @PathVariable Long sellerId
    ) {
        log.info("[ProductController] GET /api/products/seller/{}", sellerId);
        List<ProductSummaryResponse> products = productService.getSellerProducts(sellerId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/bids/me")
    public ResponseEntity<ApiResponse<List<MyBidResponse>>> getMyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ProductController] GET /api/products/bids/me memberId={}", userDetails.getMemberId());
        List<MyBidResponse> bids = auctionBidService.getMyBids(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(bids));
    }

    // 상품 상세 화면에서 productId 기준으로 DB 데이터를 조회한다.
    // 비로그인도 조회 가능(GET permitAll)하지만, 토큰이 동봉되면 SecurityFilter 가
    // 인증 객체를 채워주므로 본인 좋아요 여부(liked) 까지 함께 응답한다.
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        log.info("[ProductController] GET /api/products/{} memberId={}", productId, memberId);
        ProductDetailResponse product = productService.getProduct(productId, memberId);
        log.info("[ProductController] GET /api/products/{} productName={}", productId, product.name());
        return ResponseEntity.ok(ApiResponse.success(product));
    }

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
        log.info("[ProductController] POST /api/products memberId={}, category={}, name={}",
                userDetails.getMemberId(), request.getCategory(), request.getName());
        Long productId = productService.create(request, userDetails.getMemberId());
        log.info("[ProductController] POST /api/products created productId={}", productId);
        return ResponseEntity.ok(ApiResponse.success(productId, "상품이 등록되었습니다."));
    }

    /**
     * 상품 수정 (판매자 본인 전용)
     * PUT /api/products/{productId}
     * Authorization: Bearer {JWT 토큰} 필요
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<Long>> update(
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ProductController] PUT /api/products/{} memberId={}", productId, userDetails.getMemberId());
        Long updatedId = productService.updateMyProduct(productId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedId, "상품이 수정되었습니다."));
    }
}
