package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.domain.member.MemberRole;
import com.moida.domain.member.MemberService;
import com.moida.common.response.AdminMemberResponse;
import com.moida.domain.member.Member;
import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;

    // ADMIN만 호출 가능
    @PatchMapping("/members/{id}/role")
    public ResponseEntity<ApiResponse<String>> updateMemberRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        // 대상 회원이 ADMIN이면 역할 변경 거부
        Member target = memberService.findById(id);
        if (target.getRole() == MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ADMIN의 역할은 변경할 수 없습니다.");
        }

        // body에서 "role" 값 추출 ("USER" 또는 "ADMIN")
        MemberRole role = MemberRole.valueOf(body.get("role"));
        memberService.updateMemberRole(id, role);

        return ResponseEntity.ok(ApiResponse.success("역할이 변경되었습니다."));
    }

    // 전체 회원 목록 조회 (관리자 전용)
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
}
