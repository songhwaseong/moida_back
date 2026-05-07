package com.moida.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "엔티티를 찾을 수 없습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "M003", "비밀번호가 일치하지 않습니다."),
    SUSPENDED_MEMBER(HttpStatus.FORBIDDEN, "M004", "정지된 계정입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    NOT_PRODUCT_OWNER(HttpStatus.FORBIDDEN, "P002", "상품의 판매자가 아닙니다."),

    // Auction
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AC001", "경매를 찾을 수 없습니다."),
    AUCTION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "AC002", "이미 종료된 경매입니다."),
    INVALID_BID_AMOUNT(HttpStatus.BAD_REQUEST, "AC003", "입찰 금액이 유효하지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "AC004", "잔액이 부족합니다."),
    SELF_BID_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "AC005", "본인 상품에는 입찰할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
