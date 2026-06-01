package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.AdminDeactivatedMemberResponse;
import com.moida.domain.product.ProductService;
import com.moida.common.response.ProductSummaryResponse;
import com.moida.domain.member.MemberRole;
import com.moida.domain.member.MemberService;
import com.moida.common.response.AdminMemberResponse;
import com.moida.domain.member.Member;
import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;
    private final ProductService productService;

    // ── 회원 관리 ──────────────────────────────────────────────────────────────

    @PatchMapping("/members/{id}/role")
    public ResponseEntity<ApiResponse<String>> updateMemberRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Member target = memberService.findById(id);
        if (target.getRole() == MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ADMIN의 역할은 변경할 수 없습니다.");
        }

        MemberRole role = MemberRole.valueOf(body.get("role"));
        memberService.updateMemberRole(id, role);

        return ResponseEntity.ok(ApiResponse.success("역할이 변경되었습니다."));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<AdminMemberResponse>>> getMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<AdminMemberResponse> members = memberService.findAll().stream()
                .filter(m -> !m.getId().equals(userDetails.getMemberId()))
                .map(AdminMemberResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @GetMapping("/members/deactivated")
    public ResponseEntity<ApiResponse<List<AdminDeactivatedMemberResponse>>> getDeactivatedMembers() {
        return ResponseEntity.ok(ApiResponse.success(memberService.findDeactivatedMembers()));
    }

    // ── 상품 관리 (레거시) ──────────────────────────────────────────────────────
    // 상품 CRUD 는 AdminProductController 에서 전담한다.
    // 아래는 기존 호환을 위해 남겨둔 엔드포인트.

    /** 승인 대기 목록 */
    @GetMapping("/products/pending")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getPendingProducts() {
        List<ProductSummaryResponse> products = productService.findPendingProducts().stream()
                .map(p -> ProductSummaryResponse.from(p, null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

}
