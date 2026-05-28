package com.moida.common.response;

import com.moida.domain.member.Member;

public record AccountDeactivationInfoResponse(
        String authenticationMethod,
        String socialLogin,
        String confirmationText
) {
    public static AccountDeactivationInfoResponse from(Member member, String socialAccountConfirmationText) {
        boolean socialAccount = member.getSocialLogin() != null && !member.getSocialLogin().isBlank();

        return new AccountDeactivationInfoResponse(
                socialAccount ? "SOCIAL_CONFIRMATION" : "PASSWORD",
                member.getSocialLogin(),
                socialAccount ? socialAccountConfirmationText : null
        );
    }
}
