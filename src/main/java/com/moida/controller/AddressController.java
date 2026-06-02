package com.moida.controller;

import com.moida.common.request.AddressRequest;
import com.moida.common.response.AddressResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.address.AddressService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 회원 배송지 주소 관련 API를 제공하는 컨트롤러
 *
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 회원의 주소 목록을 조회합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddresses(userDetails.getMemberId())));
    }

    /**
     * 회원의 주소를 등록합니다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(addressService.createAddress(userDetails.getMemberId(), request)));
    }

    /**
     * 회원의 주소를 수정합니다.
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(addressService.updateAddress(
                userDetails.getMemberId(),
                addressId,
                request
        )));
    }

    /**
     * 회원의 주소를 삭제합니다.
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId
    ) {
        addressService.deleteAddress(userDetails.getMemberId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 특정 주소를 기본 배송지로 설정합니다.
     */
    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId
    ) {
        return ResponseEntity.ok(ApiResponse.success(addressService.setDefaultAddress(
                userDetails.getMemberId(),
                addressId
        )));
    }
}
