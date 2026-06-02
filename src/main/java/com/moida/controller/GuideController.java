package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.GuideResponse;
import com.moida.domain.guide.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideRepository guideRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<GuideResponse>>> getGuides() {
        List<GuideResponse> guides = guideRepository.findAllByOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(GuideResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(guides));
    }
}
