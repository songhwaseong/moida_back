package com.moida.domain.member;

public enum MemberRole {
    USER,    // 일반 회원
    MANAGER,  // 매니저 (관리자 화면 접근 가능, 역할 변경 불가)
    ADMIN    // 관리자 (역할 변경 가능)
}
