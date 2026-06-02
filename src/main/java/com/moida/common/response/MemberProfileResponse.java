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
    private Integer winCount;
    private Integer bidCount;
    private Integer wishCount;
    private String avatar;
    private String socialLogin;

    public MemberProfileResponse(Member member, int winCount, int bidCount, int wishCount) {
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.memberNo = member.getMemberNo();
        this.phone = member.getPhone();
        this.mannerTemp = member.getMannerTemp();
        this.winCount = winCount;
        this.bidCount = bidCount;
        this.wishCount = wishCount;
        this.avatar = member.getAvatar();
        this.socialLogin = member.getSocialLogin();
    }
}
