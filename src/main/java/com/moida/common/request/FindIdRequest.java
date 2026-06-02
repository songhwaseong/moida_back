package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FindIdRequest {

    @NotBlank(message = "이름은 필수 입력 사항입니다.")
    private String name;

    @NotBlank(message = "휴대폰 번호는 필수 입력 사항입니다.")
    private String phone;
}
