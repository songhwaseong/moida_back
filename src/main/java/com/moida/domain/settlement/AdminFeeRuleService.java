package com.moida.domain.settlement;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.FeeRuleUpdateRequest;
import com.moida.common.response.FeeRuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 수수료 정책 관리 서비스 (조회/수정).
 * 등록·삭제는 현재 화면에서 지원하지 않아 별도 메서드를 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AdminFeeRuleService {

    private final FeeRuleRepository feeRuleRepository;

    @Transactional(readOnly = true)
    public List<FeeRuleResponse> getAll() {
        return feeRuleRepository.findAllByOrderByMinAmountAsc().stream()
                .map(FeeRuleResponse::from)
                .toList();
    }

    @Transactional
    public FeeRuleResponse update(Long id, FeeRuleUpdateRequest req) {
        if (req.getMinAmount() != null && req.getMinAmount() < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "기준 금액은 0 이상이어야 합니다.");
        }
        if (req.getFeeRate() != null && (req.getFeeRate() < 0 || req.getFeeRate() > 100)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "수수료율은 0~100 사이여야 합니다.");
        }
        FeeRule rule = feeRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        rule.update(req.getMinAmount(), req.getFeeRate());
        return FeeRuleResponse.from(rule);
    }
}
