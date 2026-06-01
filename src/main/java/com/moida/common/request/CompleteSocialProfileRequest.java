package com.moida.common.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteSocialProfileRequest {
    private String nickname;
    private String phone;
}