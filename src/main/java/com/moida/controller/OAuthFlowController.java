package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.OAuthPrepareResponse;
import com.moida.domain.auth.OAuthFlowService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/oauth")
public class OAuthFlowController {

    private final OAuthFlowService oauthFlowService;

    @GetMapping("/{provider}/prepare")
    public ResponseEntity<ApiResponse<OAuthPrepareResponse>> prepare(
            @PathVariable String provider,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(ApiResponse.success(oauthFlowService.prepare(provider, response)));
    }
}
