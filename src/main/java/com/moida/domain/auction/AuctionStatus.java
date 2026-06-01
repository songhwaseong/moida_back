package com.moida.domain.auction;

public enum AuctionStatus {
    READY,              // 시작 전
    LIVE,               // 진행 중
    AWAITING_PAYMENT,   // 낙찰 후 결제 대기 (최고가 입찰자 잔액 부족 시, paymentDeadline 까지 결제 필요)
    SUCCESS,            // 낙찰 + 결제 완료
    FAILED,             // 유찰 (입찰 없음 / 결제 기한 만료)
    CANCELED            // 취소됨
}
