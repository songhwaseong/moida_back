package com.moida.common.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 제재 등록 요청 DTO.
 *   - memberNo : 대상 회원번호 (id 대신 표시값을 그대로 받음)
 *   - type     : SanctionType enum 이름 (WARNING / SUSPEND_7 / SUSPEND_30 / PERMANENT)
 */
@Getter
@NoArgsConstructor
public class AdminSanctionRequest {
    private String memberNo;
    private String type;
    private String reason;
    private String adminNote;
}
