package com.moida.domain.auction;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경매 정책(전역 설정). 현재는 경매 기본 진행 기간만 보관한다.
 * 단일 행으로 운영하며, 관리자 화면에서 일/시간/분으로 수정한다(내부 저장은 총 분 단위).
 * 변경은 이후 새로 시작되는 경매부터 적용되고, 진행 중인 경매에는 영향을 주지 않는다.
 */
@Entity
@Getter
@Table(name = "auction_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_policy_id")
    private Long id;

    /** 경매 진행 기간(분). 시작 시각 + 이 값 = 종료 시각. */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Builder
    private AuctionPolicy(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void updateDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
