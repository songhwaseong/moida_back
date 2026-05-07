package com.moida.domain.product;

public enum ProductStatus {
    SCHEDULED,    // 경매예정
    PENDING,      // 승인요청중
    LIVE,         // 경매중
    SOLD,         // 낙찰
    FAILED,       // 유찰
    HIDDEN,       // 숨김
    DELETED       // 삭제
}
