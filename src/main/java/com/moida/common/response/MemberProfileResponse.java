package com.moida.common.response;

import com.moida.domain.member.Member;
import lombok.Getter;

@Getter
public class MemberProfileResponse {
    private String nickname;
    private String email;
    private String memberNo;
    private String phone;
    private Double mannerTemp;
    private Integer salesCount;
    private Integer purchaseCount;
    private Integer bidCount;
    private String avatar;
    private String socialLogin;

    public MemberProfileResponse(Member member) {
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.memberNo = member.getMemberNo();
        this.phone = member.getPhone();
        this.mannerTemp = member.getMannerTemp();
        this.salesCount = member.getSalesCount();
        this.purchaseCount = member.getPurchaseCount();
        this.bidCount = member.getBidCount();
        this.avatar = member.getAvatar();
        this.socialLogin = member.getSocialLogin();
    }
}
