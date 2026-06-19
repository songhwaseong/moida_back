package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.domain.auth.WebSocketTicketService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/ws-ticket")
public class WebSocketTicketController {

    private final WebSocketTicketService ticketService;

    @PostMapping
    public ResponseEntity<ApiResponse<WebSocketTicketService.IssuedTicket>> issue(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.issue(userDetails.getMemberId())));
    }
}
