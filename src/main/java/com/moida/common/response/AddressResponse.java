package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moida.domain.address.MemberAddress;

/**
 * 회원 배송지 주소 정보 응답 DTO
 */
public record AddressResponse(
        Long id,
        String name,
        String zonecode,
        String address,
        String detail,
        String phone,
        @JsonProperty("isDefault") boolean defaultAddress
) {

    /**
     * MemberAddress 엔티티로부터 AddressResponse DTO를 생성합니다.
     */
    public static AddressResponse from(MemberAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getName(),
                address.getZonecode(),
                address.getAddress(),
                address.getDetail(),
                address.getPhone(),
                address.isDefaultAddress()
        );
    }
}
