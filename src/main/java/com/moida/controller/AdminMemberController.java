package com.moida.controller;

import com.moida.common.response.AdminWithdrawnMemberResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 회원 관리 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping("/withdrawn")
    public ResponseEntity<ApiResponse<List<AdminWithdrawnMemberResponse>>> getWithdrawnMembers() {
        return ResponseEntity.ok(ApiResponse.success(memberService.getWithdrawnMembers()));
    }
}
