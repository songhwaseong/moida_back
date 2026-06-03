package com.moida.domain.settlement;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.AdminSettlementResponse;
import com.moida.common.response.AdminSettlementSummaryResponse;
import com.moida.domain.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 정산 관리 전용 서비스.
 *
 * 상태 전이:
 *   PENDING → PAID     (markAsPaid) : 정산 처리
 *   PENDING → CANCELED (cancel)     : 보류/취소
 *   CANCELED → PAID                  : 보류 해제 후 정산 처리
 *   그 외 전이는 거부한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final SettlementRepository settlementRepository;
    private final WalletService walletService;

    @Transactional(readOnly = true)
    public List<AdminSettlementResponse> getAll() {
        return settlementRepository.findAllForAdmin().stream()
                .map(AdminSettlementResponse::from)
                .toList();
    }

    /** 상단 요약 카드: CANCELED 는 매출 합계에서 제외한다. */
    @Transactional(readOnly = true)
    public AdminSettlementSummaryResponse getSummary() {
        List<Settlement> all = settlementRepository.findAllForAdmin();
        long totalSale = 0, totalFee = 0, totalNet = 0, pending = 0;
        for (Settlement s : all) {
            if (s.getStatus() == Settlement.SettlementStatus.CANCELED) continue;
            totalSale += s.getSalesAmount();
            totalFee += s.getFeeAmount();
            totalNet += s.getSettledAmount();
            if (s.getStatus() == Settlement.SettlementStatus.PENDING) pending++;
        }
        return new AdminSettlementSummaryResponse(totalSale, totalFee, totalNet, pending);
    }

    /** 상태 변경 — PAID(정산처리) / CANCELED(보류) 만 허용 */
    @Transactional
    public AdminSettlementResponse updateStatus(Long settlementId, Settlement.SettlementStatus next) {
        Settlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        switch (next) {
            case PAID -> {
                if (s.getStatus() == Settlement.SettlementStatus.PAID) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 정산 완료된 거래입니다.");
                }
                s.payToSeller();
                // 정산 지급액을 판매자 계좌이력(입금)으로 기록한다. (payToSeller 가 잔액을 이미 올렸으므로 내역만 추가)
                walletService.recordSettlementCredit(
                        s.getSeller(),
                        s.getSettledAmount(),
                        "판매 정산금 입금"
                );
            }
            case CANCELED -> {
                if (s.getStatus() == Settlement.SettlementStatus.PAID) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "정산 완료된 거래는 보류할 수 없습니다.");
                }
                s.cancel();
            }
            case PENDING -> throw new BusinessException(ErrorCode.INVALID_INPUT, "PENDING 상태로의 변경은 허용되지 않습니다.");
        }
        return AdminSettlementResponse.from(s);
    }
}
