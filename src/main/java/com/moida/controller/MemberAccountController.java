package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.domain.member.MemberService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 회원의 계정 관리 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberAccountController {

    private final MemberService memberService;

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        memberService.withdrawCurrentMember(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
