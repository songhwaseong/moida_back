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
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.auction.BidRepository;
import com.moida.domain.product.ProductLikeRepository;
import com.moida.domain.wallet.WalletTransaction;
import com.moida.domain.wallet.WalletTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String SOCIAL_ACCOUNT_CONFIRMATION_TEXT = "회원탈퇴";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductLikeRepository productLikeRepository;

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
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
    public void completeSocialProfile(String email, String nickname, String phone) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateNickname(nickname);
        member.updateProfile(null, phone, null, null);
    }

    @Transactional
    public void signup(SignupRequest dto) {
        // memberNo 자동 생성 (예: 2026050900001)
        String memberNo = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", memberRepository.count() + 1);


        Member member = Member.builder()
                .memberNo(memberNo)
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .location(dto.getLocation())
                .role(MemberRole.USER)
                .build();

        memberRepository.save(member);
    }

    public Member findById(Long id) {
        // id로 회원 조회, 없으면 예외
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public List<Member> findAll() {
        // 최신 가입순으로 정렬
        return memberRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void updateMemberRole(Long id, MemberRole role) {
        // 회원 조회 후 역할 변경 (더티 체킹으로 자동 저장)
        Member member = findById(id);
        member.updateRole(role);
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

        // 정산/출금 여지가 남은 계정은 탈퇴를 막는다.
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

        // 개인정보는 즉시 삭제하지 않고 탈퇴 상태로만 전환한다.
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

    public MemberProfileResponse getMemberProfile(Long memberId) {
        Member member = findById(memberId);
        int winCount  = (int) auctionRepository.countByWinnerId(memberId);
        int bidCount  = (int) bidRepository.countByBidderId(memberId);
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
