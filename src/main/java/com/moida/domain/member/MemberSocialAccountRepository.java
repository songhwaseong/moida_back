package com.moida.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    Optional<MemberSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    Optional<MemberSocialAccount> findByMemberIdAndProvider(Long memberId, SocialProvider provider);

    List<MemberSocialAccount> findAllByMemberId(Long memberId);
}
