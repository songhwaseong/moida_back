package com.moida.common.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {
    private String nickname;
    private String phone;
    private String avatar;
}
