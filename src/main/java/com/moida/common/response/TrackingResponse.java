package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 배송 조회(스마트택배) 응답 DTO.
 * 프론트엔드 배송 추적 화면이 바로 렌더링할 수 있는 형태로 가공한다.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {

    private String carrier;        // 택배사명 (예: CJ대한통운)
    private String trackingNo;     // 송장번호
    private String product;        // 상품명 (스마트택배 itemName)
    private String currentStatus;  // 현재 배송 상태 텍스트
    private String estimatedDate;  // 도착 예정 정보
    private boolean complete;      // 배송 완료 여부
    private int level;             // 배송 단계 (1:상품인수 ~ 6:배송완료)
    private List<Step> steps;      // 배송 상세 단계 목록 (최신순)

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step {
        private String time;      // 처리 시각 (timeString)
        private String location;  // 처리 위치 (where)
        private String status;    // 처리 상태 (kind)
        private int level;        // 단계 레벨
    }
}
