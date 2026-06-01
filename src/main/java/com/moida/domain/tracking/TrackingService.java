package com.moida.domain.tracking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.TrackingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 스마트택배(sweettracker) 배송 조회 API 연동 서비스.
 */
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final RestTemplate restTemplate = new RestTemplate(); // 스마트택배 API HTTP 클라이언트
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tracking.sweettracker.api-key}")
    private String apiKey;          // application.yml → 환경변수 SWEETTRACKER_API_KEY

    @Value("${tracking.sweettracker.base-url}")
    private String baseUrl;         // 스마트택배 API 기본 URL

    // 스마트택배 택배사 코드 → 택배사명
    private static final Map<String, String> CARRIER_NAMES = Map.of(
            "04", "CJ대한통운",
            "06", "로젠택배",
            "05", "한진택배",
            "01", "우체국택배",
            "08", "롯데택배",
            "23", "경동택배",
            "11", "일양로지스",
            "12", "EMS"
    );

    public TrackingResponse getTrackingInfo(String carrierCode, String invoiceNo) {
        String carrierName = CARRIER_NAMES.get(carrierCode);
        if (carrierName == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_CARRIER);
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/trackingInfo")
                .queryParam("t_code", carrierCode)
                .queryParam("t_invoice", invoiceNo)
                .queryParam("t_key", apiKey)
                .toUriString();

        JsonNode root;
        try {
            String body = restTemplate.getForObject(url, String.class);
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TRACKING_LOOKUP_FAILED);
        }

        // trackingInfo 성공 응답은 result="Y" 로 표시되고 배송단계(trackingDetails)를 포함한다.
        // 조회 실패 시에는 result != "Y" 또는 status=false + msg 형태로 내려온다.
        boolean success = "Y".equalsIgnoreCase(root.path("result").asText(""))
                || (root.path("trackingDetails").isArray() && root.path("trackingDetails").size() > 0);
        if (!success) {
            throw new BusinessException(ErrorCode.TRACKING_LOOKUP_FAILED);
        }

        List<TrackingResponse.Step> steps = new ArrayList<>();
        JsonNode details = root.path("trackingDetails");
        if (details.isArray()) {
            for (JsonNode d : details) {
                steps.add(TrackingResponse.Step.builder()
                        .time(text(d, "timeString"))
                        .location(text(d, "where"))
                        .status(text(d, "kind"))
                        .level(d.path("level").asInt(0))
                        .build());
            }
        }
        // 스마트택배는 과거→최신 순으로 내려주므로, 화면 표시를 위해 최신순으로 뒤집는다.
        java.util.Collections.reverse(steps);

        String currentStatus = text(root.path("lastStateDetail"), "kind");
        if (currentStatus == null) {
            currentStatus = steps.isEmpty() ? null : steps.get(0).getStatus();
        }

        return TrackingResponse.builder()
                .carrier(carrierName)
                .trackingNo(text(root, "invoiceNo") != null ? text(root, "invoiceNo") : invoiceNo)
                .product(text(root, "itemName"))
                .currentStatus(currentStatus)
                .estimatedDate(text(root, "estimate"))
                .complete(root.path("complete").asBoolean(false))
                .level(root.path("level").asInt(0))
                .steps(steps)
                .build();
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return (s == null || s.isBlank()) ? null : s;
    }
}
