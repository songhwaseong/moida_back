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
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "M005", "주소를 찾을 수 없습니다."),
    ACCOUNT_DEACTIVATION_BLOCKED(HttpStatus.BAD_REQUEST, "M006", "탈퇴할 수 없는 계정 상태입니다."),
    MEMBER_ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "M007", "이용할 수 없는 계정입니다."),
    PASSWORDLESS_LOGIN_REQUIRED(HttpStatus.FORBIDDEN, "M008", "Passwordless가 등록된 계정입니다.\nPasswordless로 로그인해주세요."),
    LOGIN_TEMPORARILY_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "M009", "로그인 시도가 너무 많아 일시적으로 잠겼습니다. 잠시 후 다시 시도해주세요."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    NOT_PRODUCT_OWNER(HttpStatus.FORBIDDEN, "P002", "상품의 판매자가 아닙니다."),

    // Auction
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AC001", "경매를 찾을 수 없습니다."),
    AUCTION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "AC002", "이미 종료된 경매입니다."),
    INVALID_BID_AMOUNT(HttpStatus.BAD_REQUEST, "AC003", "입찰 금액이 유효하지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "AC004", "잔액이 부족합니다."),
    SELF_BID_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "AC005", "본인 상품에는 입찰할 수 없습니다."),

    // Review
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "R001", "후기를 작성할 수 없는 거래입니다."),
    REVIEW_RECEIPT_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "R002", "수령확인 후 후기를 작성할 수 있습니다."),
    REVIEW_ALREADY_WRITTEN(HttpStatus.CONFLICT, "R003", "이미 후기를 작성한 상품입니다."),

    // Wallet 관련 에러 코드
    /** 등록된 출금 계좌를 찾을 수 없는 경우 */
    WALLET_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "W001", "등록된 출금 계좌가 없습니다."),
    /** 입출금 금액이 유효하지 않은 경우 (예: 최소 금액 미만) */
    INVALID_WALLET_AMOUNT(HttpStatus.BAD_REQUEST, "W002", "금액이 유효하지 않습니다."),
    /** 지갑 잔액이 부족하여 출금할 수 없는 경우 */
    WALLET_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "W003", "잔액이 부족합니다."),
    /** 지갑 거래 내역을 찾을 수 없는 경우 */
    WALLET_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "W004", "지갑 거래 내역을 찾을 수 없습니다."),
    /** 지갑 거래 내역을 현재 상태에서 처리할 수 없는 경우 */
    INVALID_WALLET_TRANSACTION(HttpStatus.BAD_REQUEST, "W005", "처리할 수 없는 지갑 거래입니다."),

    // 배송 조회 관련 에러 코드
    /** 지원하지 않는 택배사 코드인 경우 */
    UNSUPPORTED_CARRIER(HttpStatus.BAD_REQUEST, "T001", "지원하지 않는 택배사입니다."),
    /** 택배사 API 조회에 실패하거나 송장 정보가 없는 경우 */
    TRACKING_LOOKUP_FAILED(HttpStatus.BAD_REQUEST, "T002", "배송 정보를 조회할 수 없습니다. 택배사와 송장번호를 확인해주세요."),

    // 휴대폰 인증(SMS) 관련 에러 코드
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS001", "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),
    VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "SMS002", "인증 요청 내역이 없습니다. 인증번호를 다시 요청해주세요."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "SMS003", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "SMS004", "인증번호가 일치하지 않습니다."),
    VERIFICATION_TOO_MANY_ATTEMPTS(HttpStatus.BAD_REQUEST, "SMS005", "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."),
    VERIFICATION_RESEND_COOLDOWN(HttpStatus.BAD_REQUEST, "SMS006", "잠시 후 다시 시도해주세요."),
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "SMS007", "휴대폰 인증을 완료해주세요."),
    VERIFICATION_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "SMS008", "오늘 인증번호 발송 한도를 초과했습니다. 내일 다시 시도해주세요."),

    // 이메일 인증 관련 에러 코드
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL001", "인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "EMAIL002", "인증 요청 내역이 없습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL003", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "EMAIL004", "인증번호가 일치하지 않습니다."),
    EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS(HttpStatus.BAD_REQUEST, "EMAIL005", "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_VERIFICATION_RESEND_COOLDOWN(HttpStatus.BAD_REQUEST, "EMAIL006", "잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
