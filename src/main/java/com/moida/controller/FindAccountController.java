package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.FindIdCodeSendRequest;
import com.moida.common.request.FindIdRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.FindIdResponse;
import com.moida.domain.auth.PhoneVerificationService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/find-id")
@RequiredArgsConstructor
public class FindAccountController {

    private final MemberService memberService;
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody FindIdCodeSendRequest request) {
        memberService.findActiveByNameAndPhone(request.getName(), request.getPhone());
        phoneVerificationService.sendCode(request.getPhone());
        return ResponseEntity.ok(ApiResponse.success(null, "인증번호를 전송했습니다."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@Valid @RequestBody FindIdRequest request) {
        if (!phoneVerificationService.isVerified(request.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }
        Member member = memberService.findActiveByNameAndPhone(request.getName(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.success(FindIdResponse.from(member)));
    }
}
