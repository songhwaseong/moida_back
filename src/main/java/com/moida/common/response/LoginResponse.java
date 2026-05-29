package com.moida.common.response;

import com.moida.domain.member.Member;
import lombok.Getter;

@Getter
public class LoginResponse {
    private String accessToken;
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean isNewUser;

    public LoginResponse(String token, Member member, boolean isNewUser) {
        this.accessToken = token;
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.role = member.getRole().name();
        this.isNewUser = isNewUser;
    }
}
