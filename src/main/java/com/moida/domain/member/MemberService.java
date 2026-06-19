package com.moida.domain.member;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.ChangePasswordRequest;
import com.moida.common.request.DeactivateAccountRequest;
import com.moida.common.request.SignupRequest;
import com.moida.common.request.UpdateProfileRequest;
import com.moida.common.response.AccountDeactivationInfoResponse;
import com.moida.common.response.AdminDeactivatedMemberResponse;
import com.moida.common.response.MemberProfileResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.auction.BidRepository;
import com.moida.domain.product.ProductLikeRepository;
import com.moida.domain.wallet.WalletTransaction;
import com.moida.domain.wallet.WalletTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String SOCIAL_ACCOUNT_CONFIRMATION_TEXT = "회원탈퇴";

    // 로그인 연속 실패 허용 횟수와, 초과 시 적용할 잠금 시간.
    // 트레이드오프: 계정(이메일) 단위 잠금이라, 공격자가 피해자 이메일로 일부러 실패시켜
    // 일시적으로 로그인을 방해하는 계정 잠금형 DoS 가 이론상 가능하다. 그럼에도
    //   - 잠금은 10분으로 짧고 자동 해제되며(영구 잠금 아님),
    //   - 정상 사용자는 올바른 비밀번호 입력 시 실패 카운트가 즉시 리셋되고,
    //   - 근본 차단(IP 단위 분리)은 다중 인스턴스 환경에서 IP 집계 저장소(Redis 등)가
    //     필요해 현 규모에는 과한 인프라라 도입하지 않는다.
    // 위 이유로 표준적인 계정 단위 일시 잠금을 채택한다.
    private static final int LOGIN_MAX_ATTEMPTS = 10;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(10);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductLikeRepository productLikeRepository;
    private final MemberSocialAccountRepository socialAccountRepository;
    private final AdminActionLogService adminActionLogService;
    private final MemberNoGenerator memberNoGenerator;

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    // ===== 로그인 brute-force 방어 =====

    /** 로그인 인증 전에 호출. 반복 실패로 잠긴 계정이면 예외를 던진다. (미존재 이메일은 통과 — 어차피 인증이 실패한다) */
    public void assertLoginNotLocked(String email) {
        memberRepository.findByEmail(email).ifPresent(member -> {
            if (member.isLoginLocked(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.LOGIN_TEMPORARILY_LOCKED);
            }
        });
    }

    /** 인증 실패 시 호출. 회원이 존재하면 실패 횟수를 올리고 임계치 도달 시 잠근다. */
    @Transactional
    public void recordLoginFailure(String email) {
        memberRepository.findByEmail(email)
                .ifPresent(member -> member.recordLoginFailure(LOGIN_MAX_ATTEMPTS, LOGIN_LOCK_DURATION, LocalDateTime.now()));
    }

    /** 인증 성공 시 호출. 실패 횟수와 잠금 상태를 초기화한다. */
    @Transactional
    public void recordLoginSuccess(String email) {
        memberRepository.findByEmail(email).ifPresent(Member::resetLoginFailure);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public long countByNickname(String nickname) {
        return memberRepository.countByNickname(nickname);
    }

    public Member findActiveByNameAndPhone(String name, String phone) {
        String normalizedName = normalizeRequiredText(name);
        String normalizedPhone = normalizePhone(phone);
        if (normalizedName == null || normalizedPhone.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이름과 휴대폰 번호를 입력해주세요.");
        }
        return memberRepository.findAllByNameAndStatus(normalizedName, MemberStatus.ACTIVE).stream()
                .filter(member -> normalizedPhone.equals(normalizePhone(member.getPhone())))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "일치하는 회원 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public Member completeSocialProfile(String email, String nickname, String phone) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "휴대폰 번호를 입력해주세요.");
        }

        Member existing = findActiveMemberByPhone(normalizedPhone, member.getId()).orElse(null);
        if (existing != null) {
            relinkSocialAccounts(member, existing);
            existing.updateNickname(nickname);
            if (existing.getPhone() == null || existing.getPhone().isBlank()) {
                existing.updateProfile(null, normalizedPhone, null, null);
            }
            memberRepository.delete(member);
            return existing;
        }

        member.updateNickname(nickname);
        member.updateProfile(null, normalizedPhone, null, null);
        return member;
    }

    @Transactional
    public void signup(SignupRequest dto) {
        String email = normalizeRequiredText(dto.getEmail());
        String normalizedPhone = normalizePhone(dto.getPhone());
        Optional<Member> emailOwner = memberRepository.findByEmail(email);
        Optional<Member> phoneOwner = findActiveMemberByPhone(normalizedPhone, null);

        if (emailOwner.isPresent()) {
            boolean sameVerifiedPhone = phoneOwner
                    .map(member -> member.getId().equals(emailOwner.get().getId()))
                    .orElse(false);
            if (!sameVerifiedPhone) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            registerLocalLogin(emailOwner.get(), dto, email, normalizedPhone);
            return;
        }

        if (phoneOwner.isPresent()) {
            Member existing = phoneOwner.get();
            if (existing.getSocialLogin() == null || existing.getSocialLogin().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 가입된 휴대폰 번호입니다. 기존 계정으로 로그인해주세요.");
            }
            registerLocalLogin(existing, dto, email, normalizedPhone);
            return;
        }

        Member member = Member.builder()
                .memberNo(memberNoGenerator.generate())
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phone(normalizedPhone)
                .location(dto.getLocation())
                .role(MemberRole.USER)
                .build();

        memberRepository.save(member);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public List<Member> findAll() {
        return memberRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void updateMemberRole(Long id, MemberRole role, String reason) {
        Member member = findById(id);
        MemberRole previousRole = member.getRole();
        member.updateRole(role);
        adminActionLogService.record(
                "MEMBER_ROLE_CHANGE",
                "MEMBER",
                member.getId(),
                member.getEmail(),
                adminActionLogService.fields("role", previousRole),
                adminActionLogService.fields("role", member.getRole()),
                reason
        );
    }

    public AccountDeactivationInfoResponse getAccountDeactivationInfo(Long memberId) {
        return AccountDeactivationInfoResponse.from(findById(memberId), SOCIAL_ACCOUNT_CONFIRMATION_TEXT);
    }

    @Transactional
    public void deactivateMemberAccount(Long memberId, DeactivateAccountRequest request) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        DeactivateAccountRequest safeRequest = request != null
                ? request
                : new DeactivateAccountRequest(null, null, null, null);

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DEACTIVATION_BLOCKED, "이미 탈퇴했거나 이용할 수 없는 계정입니다.");
        }
        if (member.getBalance() != null && member.getBalance() > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DEACTIVATION_BLOCKED, "예치금 잔액이 남아 있어 탈퇴할 수 없습니다.");
        }
        if (walletTransactionRepository.existsByMemberIdAndStatus(memberId, WalletTransaction.TransactionStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ACCOUNT_DEACTIVATION_BLOCKED, "처리 대기 중인 지갑 요청이 있어 탈퇴할 수 없습니다.");
        }
        validateAccountDeactivationAuth(member, safeRequest);
        String reasonCode = requireReasonCode(safeRequest.reasonCode());
        String reasonDetail = normalizeOptionalText(safeRequest.reasonDetail(), 500);

        member.deactivateAccount(reasonCode, reasonDetail);
    }

    public List<AdminDeactivatedMemberResponse> findDeactivatedMembers() {
        return memberRepository.findAllByStatusOrderByWithdrawnAtDesc(MemberStatus.WITHDRAWN).stream()
                .map(AdminDeactivatedMemberResponse::from)
                .toList();
    }

    private void validateAccountDeactivationAuth(Member member, DeactivateAccountRequest request) {
        if (member.getSocialLogin() == null || member.getSocialLogin().isBlank()) {
            String password = normalizeOptionalText(request.password(), 255);
            if (password == null) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD, "현재 비밀번호를 입력해주세요.");
            }
            if (!passwordEncoder.matches(password, member.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }
            return;
        }

        String confirmationText = normalizeOptionalText(request.confirmationText(), 50);
        if (!SOCIAL_ACCOUNT_CONFIRMATION_TEXT.equals(confirmationText)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "확인 문구를 정확히 입력해주세요.");
        }
    }

    private String requireReasonCode(String value) {
        String reasonCode = normalizeOptionalText(value, 50);
        if (reasonCode == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "탈퇴 사유를 선택해주세요.");
        }
        return reasonCode;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String normalizeRequiredText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private Optional<Member> findActiveMemberByPhone(String normalizedPhone, Long excludedMemberId) {
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return Optional.empty();
        }
        return memberRepository.findAllByStatus(MemberStatus.ACTIVE).stream()
                .filter(member -> excludedMemberId == null || !member.getId().equals(excludedMemberId))
                .filter(member -> normalizedPhone.equals(normalizePhone(member.getPhone())))
                .findFirst();
    }

    private void relinkSocialAccounts(Member source, Member target) {
        List<MemberSocialAccount> sourceAccounts = socialAccountRepository.findAllByMemberId(source.getId());
        for (MemberSocialAccount sourceAccount : sourceAccounts) {
            Optional<MemberSocialAccount> targetAccount =
                    socialAccountRepository.findByMemberIdAndProvider(target.getId(), sourceAccount.getProvider());
            if (targetAccount.isPresent()) {
                boolean sameProviderUser = targetAccount.get().getProviderUserId().equals(sourceAccount.getProviderUserId());
                if (!sameProviderUser) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 연결된 같은 소셜 로그인 계정이 있습니다.");
                }
                continue;
            }
            sourceAccount.relink(target);
        }
    }

    private void registerLocalLogin(Member member, SignupRequest dto, String email, String normalizedPhone) {
        member.registerLocalCredentials(
                email,
                passwordEncoder.encode(dto.getPassword()),
                dto.getName(),
                dto.getNickname(),
                normalizedPhone,
                dto.getLocation()
        );
    }

    public MemberProfileResponse getMemberProfile(Long memberId) {
        Member member = findById(memberId);
        int winCount = (int) auctionRepository.countByWinnerId(memberId);
        int bidCount = (int) bidRepository.countByBidderId(memberId);
        int wishCount = (int) productLikeRepository.countByMemberId(memberId);
        return new MemberProfileResponse(member, winCount, bidCount, wishCount);
    }

    @Transactional
    public void updateMemberProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findById(memberId);
        member.updateNickname(request.getNickname());
        member.updateProfile(null, request.getPhone(), null, null);
        member.updateAvatar(request.getAvatar());
    }

    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = findById(memberId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        member.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.changePassword(passwordEncoder.encode(newPassword));
    }
}
