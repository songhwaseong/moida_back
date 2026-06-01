package com.moida.domain.settlement;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수수료 정책. 낙찰가가 minAmount 이상일 때 feeRate(%)가 적용된다.
 * 정산 생성 시점에 적용되며, 변경 이후 체결되는 거래부터 효과를 본다(기존 거래에는 영향 없음).
 */
@Entity
@Getter
@Table(name = "fee_rules",
        indexes = @Index(name = "idx_fee_rule_min_amount", columnList = "min_amount"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_rule_id")
    private Long id;

    @Column(name = "min_amount", nullable = false)
    private Long minAmount;       // 적용 시작 금액 (이상)

    @Column(name = "fee_rate", nullable = false)
    private Double feeRate;       // 수수료율(%)

    @Builder
    private FeeRule(Long minAmount, Double feeRate) {
        this.minAmount = minAmount;
        this.feeRate = feeRate;
    }

    public void update(Long minAmount, Double feeRate) {
        if (minAmount != null) this.minAmount = minAmount;
        if (feeRate != null)   this.feeRate = feeRate;
    }
}
