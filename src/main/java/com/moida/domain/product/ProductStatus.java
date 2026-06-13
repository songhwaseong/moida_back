package com.moida.domain.product;

public enum ProductStatus {
    SCHEDULED,    // 경매예정
    PENDING,      // 승인요청중
    NEEDS_REVISION, // 보완요청
    LIVE,         // 경매중
    SOLD,         // 낙찰
    FAILED,       // 유찰
    RETURN_REQUESTED,  // 환수요청
    RETURN_SHIPPING,   // 반송중
    RETURN_COMPLETED,  // 환수완료
    HIDDEN,       // 숨김
    DELETED       // 삭제
}
