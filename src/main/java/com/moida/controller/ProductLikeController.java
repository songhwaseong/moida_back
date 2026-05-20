package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.ProductSummaryResponse;
import com.moida.domain.product.ProductLikeService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductLikeController {

    private final ProductLikeService productLikeService;

    // 상세 화면의 하트 버튼이 호출하는 엔드포인트.
    // 이미 좋아요한 상품이면 해제, 아니면 추가하며 결과(liked, likeCount)를 반환해
    // 프론트가 별도 재조회 없이 화면을 즉시 갱신할 수 있게 한다. JWT 인증 필요.
    @PostMapping("/{productId}/like")
    public ResponseEntity<ApiResponse<ProductLikeService.ToggleResult>> toggleLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ProductLikeController] POST /api/products/{}/like memberId={}",
                productId, userDetails.getMemberId());
        ProductLikeService.ToggleResult result = productLikeService.toggle(productId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 관심 목록(위시리스트) 화면에서 호출. 본인이 좋아요한 상품만 응답하므로 JWT 인증 필요.
    // SecurityConfig 에서 이 경로만 명시적으로 authenticated() 처리되어 있다.
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getMyLikes(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ProductLikeController] GET /api/products/likes memberId={}", userDetails.getMemberId());
        List<ProductSummaryResponse> likes = productLikeService.getMyLikes(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(likes));
    }
}
