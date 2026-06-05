package com.moida.domain.sanction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminSanctionRequest;
import com.moida.common.response.AdminSanctionResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 제재 관리 서비스.
 * 등록 시 Member 상태(정지/영구정지) 및 sanctionCount 도 함께 갱신한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSanctionService {

    private final SanctionRepository sanctionRepository;
    private final MemberRepository memberRepository;
    private final AdminActionLogService adminActionLogService;

    @Transactional(readOnly = true)
    public List<AdminSanctionResponse> getAll() {
        return sanctionRepository.findAllForAdmin().stream()
                .map(AdminSanctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return sanctionRepository.count();
    }

    @Transactional
    public AdminSanctionResponse create(AdminSanctionRequest request,
                                         @AuthenticationPrincipal CustomUserDetails adminDetails) {
        if (request.getMemberNo() == null || request.getMemberNo().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "회원번호를 입력해주세요.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "제재 유형을 선택해주세요.");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사유를 입력해주세요.");
        }

        Sanction.SanctionType type;
        try {
            type = Sanction.SanctionType.valueOf(request.getType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 제재 유형입니다.");
        }

        Member target = memberRepository.findByMemberNo(request.getMemberNo().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member admin = memberRepository.findById(adminDetails.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Object beforeValue = adminActionLogService.fields(
                "status", target.getStatus(),
                "sanctionCount", target.getSanctionCount(),
                "suspendedUntil", target.getSuspendedUntil()
        );

        // 1) Member 도메인 메서드로 상태/카운트 갱신 (정지일이 있는 유형은 expiresAt 도 함께 계산)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = null;
        switch (type) {
            case WARNING -> target.warn();
            case SUSPEND_7 -> {
                expiresAt = now.plusDays(7);
                target.suspend(expiresAt);
            }
            case SUSPEND_30 -> {
                expiresAt = now.plusDays(30);
                target.suspend(expiresAt);
            }
            case PERMANENT -> target.permanentBan();
        }

        // 2) Sanction row 저장 — 이력 보관용
        Sanction sanction = Sanction.builder()
                .member(target)
                .admin(admin)
                .type(type)
                .reason(request.getReason().trim())
                .adminNote(request.getAdminNote())
                .expiresAt(expiresAt)
                .build();
        Sanction saved = sanctionRepository.save(sanction);
        adminActionLogService.record(
                "SANCTION_CREATE",
                "MEMBER",
                target.getId(),
                target.getMemberNo(),
                beforeValue,
                adminActionLogService.fields(
                        "status", target.getStatus(),
                        "sanctionCount", target.getSanctionCount(),
                        "suspendedUntil", target.getSuspendedUntil(),
                        "sanctionId", saved.getId(),
                        "type", saved.getType(),
                        "reason", saved.getReason()
                ),
                "회원 제재 등록"
        );
        return AdminSanctionResponse.from(saved);
    }
}
