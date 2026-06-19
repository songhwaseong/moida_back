package com.moida.controller;

import com.moida.common.request.ChangePasswordRequest;
import com.moida.common.request.DeactivateAccountRequest;
import com.moida.common.response.AccountDeactivationInfoResponse;
import com.moida.common.response.ApiResponse;
import com.moida.common.request.UpdateProfileRequest;
import com.moida.common.response.MemberProfileResponse;
import com.moida.domain.member.MemberService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 로그인한 회원의 계정 관리 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberAccountController {

    private final MemberService memberService;

    @GetMapping("/me/deactivation-info")
    public ResponseEntity<ApiResponse<AccountDeactivationInfoResponse>> getAccountDeactivationInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getAccountDeactivationInfo(userDetails.getMemberId())));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivateMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) DeactivateAccountRequest request
    ) {
        memberService.deactivateMemberAccount(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMemberProfile(userDetails.getMemberId())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateProfileRequest request
    ) {
        memberService.updateMemberProfile(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        memberService.changePassword(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
