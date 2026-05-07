package com.moida.domain.auction;

public enum AuctionStatus {
    READY,       // 시작 전
    LIVE,        // 진행 중
    SUCCESS,     // 낙찰
    FAILED,      // 유찰
    CANCELED     // 취소됨
}
