package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.TrackingResponse;
import com.moida.domain.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping
    public ResponseEntity<ApiResponse<TrackingResponse>> getTracking(
            @RequestParam String carrier,
            @RequestParam String invoice
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trackingService.getTrackingInfo(carrier, invoice.replaceAll("[^0-9]", ""))
        ));
    }
}
