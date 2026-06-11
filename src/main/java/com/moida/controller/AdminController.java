package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminRoleUpdateRequest;
import com.moida.common.response.AdminDeactivatedMemberResponse;
import com.moida.common.response.AdminMemberResponse;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.ProductSummaryResponse;
import com.moida.domain.product.ProductImageStorageService;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRole;
import com.moida.domain.member.MemberService;
import com.moida.domain.product.ProductService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;
    private final ProductService productService;
    private final ProductImageStorageService productImageStorageService;
    private final AdminActionLogService adminActionLogService;

    @PatchMapping("/members/{id}/role")
    public ResponseEntity<ApiResponse<String>> updateMemberRole(
            @PathVariable Long id,
            @RequestBody AdminRoleUpdateRequest request
    ) {
        try {
            String reason = requireReason(request == null ? null : request.reason());
            Member target = memberService.findById(id);
            if (target.getRole() == MemberRole.ADMIN) {
                adminActionLogService.recordFailure(
                        "MEMBER_ROLE_CHANGE_FAILED",
                        "MEMBER",
                        target.getId(),
                        target.getEmail(),
                        adminActionLogService.fields("requestedRole", request == null ? null : request.role()),
                        "ADMIN 역할 변경 시도 차단"
                );
                throw new BusinessException(ErrorCode.INVALID_INPUT, "ADMIN의 역할은 변경할 수 없습니다.");
            }

            MemberRole role = MemberRole.valueOf(request.role());
            memberService.updateMemberRole(id, role, reason);
        } catch (RuntimeException e) {
            adminActionLogService.recordFailure(
                    "MEMBER_ROLE_CHANGE_FAILED",
                    "MEMBER",
                    id,
                    String.valueOf(id),
                    adminActionLogService.fields("requestedRole", request == null ? null : request.role()),
                    e.getMessage()
            );
            throw e;
        }

        return ResponseEntity.ok(ApiResponse.success("역할이 변경되었습니다."));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<AdminMemberResponse>>> getMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminActionLogService.recordView(
                "ADMIN_MEMBER_LIST_VIEW",
                "MEMBER",
                adminActionLogService.fields("view", "activeMembers")
        );
        List<AdminMemberResponse> members = memberService.findAll().stream()
                .filter(m -> !m.getId().equals(userDetails.getMemberId()))
                .map(AdminMemberResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @GetMapping("/members/deactivated")
    public ResponseEntity<ApiResponse<List<AdminDeactivatedMemberResponse>>> getDeactivatedMembers() {
        adminActionLogService.recordView(
                "ADMIN_DEACTIVATED_MEMBER_VIEW",
                "MEMBER",
                adminActionLogService.fields("view", "deactivatedMembers")
        );
        return ResponseEntity.ok(ApiResponse.success(memberService.findDeactivatedMembers()));
    }

    @GetMapping("/products/pending")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getPendingProducts() {
        adminActionLogService.recordView(
                "ADMIN_PENDING_PRODUCT_VIEW",
                "PRODUCT",
                adminActionLogService.fields("view", "pendingProducts")
        );
        List<ProductSummaryResponse> products = productService.findPendingProducts().stream()
                .map(p -> ProductSummaryResponse.from(p, null, productImageStorageService::toPublicUrl))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 변경 사유를 입력해야 합니다.");
        }
        return reason.trim();
    }
}
