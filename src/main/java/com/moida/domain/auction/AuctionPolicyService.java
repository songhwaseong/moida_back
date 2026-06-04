package com.moida.domain.auction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AuctionPolicyUpdateRequest;
import com.moida.common.response.AuctionPolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경매 정책(경매 기본 진행 기간) 조회/수정 서비스.
 * 정책 행이 없을 때를 대비해 기본값(7일)으로 폴백한다.
 */
@Service
@RequiredArgsConstructor
public class AuctionPolicyService {

    /** 정책 행이 없을 때의 기본 경매 기간(분) = 7일. */
    public static final int DEFAULT_DURATION_MINUTES = 7 * 24 * 60;

    /** 허용 범위: 최소 1분 ~ 최대 365일. */
    private static final int MIN_DURATION_MINUTES = 1;
    private static final int MAX_DURATION_MINUTES = 365 * 24 * 60;

    private final AuctionPolicyRepository auctionPolicyRepository;

    /** 경매 생성 시 사용할 현재 진행 기간(분). 정책이 없으면 기본값. */
    @Transactional(readOnly = true)
    public int getDurationMinutes() {
        return auctionPolicyRepository.findTopByOrderByIdAsc()
                .map(AuctionPolicy::getDurationMinutes)
                .orElse(DEFAULT_DURATION_MINUTES);
    }

    @Transactional(readOnly = true)
    public AuctionPolicyResponse get() {
        return AuctionPolicyResponse.of(getDurationMinutes());
    }

    @Transactional
    public AuctionPolicyResponse update(AuctionPolicyUpdateRequest req) {
        int total = req.toTotalMinutes();
        if (total < MIN_DURATION_MINUTES || total > MAX_DURATION_MINUTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "경매 기간은 최소 1분 이상, 최대 365일 이하여야 합니다.");
        }
        AuctionPolicy policy = auctionPolicyRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> AuctionPolicy.builder()
                        .durationMinutes(DEFAULT_DURATION_MINUTES)
                        .build());
        policy.updateDurationMinutes(total);
        auctionPolicyRepository.save(policy);
        return AuctionPolicyResponse.of(total);
    }
}
