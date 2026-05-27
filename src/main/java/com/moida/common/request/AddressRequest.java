package com.moida.common.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송지 주소 등록 및 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    /** 배송지 이름 */
    @NotBlank(message = "배송지 이름을 입력해주세요.")
    @Size(max = 50, message = "배송지 이름은 50자 이하여야 합니다.")
    private String name;

    /** 우편번호 */
    @NotBlank(message = "우편번호를 입력해주세요.")
    @Pattern(regexp = "\\d{5}", message = "우편번호는 숫자 5자리여야 합니다.")
    private String zonecode;

    /** 기본 주소 */
    @NotBlank(message = "주소를 입력해주세요.")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
    private String address;

    /** 상세 주소 */
    @Size(max = 100, message = "상세 주소는 100자 이하여야 합니다.")
    private String detail;

    /** 연락처 */
    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(regexp = "01[016789]-?\\d{3,4}-?\\d{4}", message = "올바른 휴대폰 번호를 입력해주세요.")
    private String phone;

    /** 기본 배송지로 설정할지 여부 */
    @JsonProperty("isDefault")
    private Boolean defaultAddress;

    public boolean isDefaultRequested() {
        return Boolean.TRUE.equals(defaultAddress);
    }
}
