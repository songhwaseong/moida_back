package com.moida.domain.auth;

public enum VerificationPurpose {
    RESET_PASSWORD(Channel.EMAIL),
    PASSWORDLESS_WITHDRAW(Channel.EMAIL),
    SIGNUP(Channel.PHONE),
    COMPLETE_SOCIAL_PROFILE(Channel.PHONE),
    FIND_ID(Channel.PHONE),
    SELLER_PHONE(Channel.PHONE);

    private final Channel channel;

    VerificationPurpose(Channel channel) {
        this.channel = channel;
    }

    public boolean supportsEmail() {
        return channel == Channel.EMAIL;
    }

    public boolean supportsPhone() {
        return channel == Channel.PHONE;
    }

    private enum Channel {
        EMAIL,
        PHONE
    }
}
