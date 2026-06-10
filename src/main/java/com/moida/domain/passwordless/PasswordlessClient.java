package com.moida.domain.passwordless;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
public class PasswordlessClient {

    private static final String IS_AP = "/ap/rest/auth/isAp";
    private static final String JOIN_AP = "/ap/rest/auth/joinAp";
    private static final String WITHDRAWAL_AP = "/ap/rest/auth/withdrawalAp";
    private static final String GET_TOKEN_FOR_ONE_TIME = "/ap/rest/auth/getTokenForOneTime";
    private static final String GET_SP = "/ap/rest/auth/getSp";
    private static final String RESULT = "/ap/rest/auth/result";
    private static final String CANCEL = "/ap/rest/auth/cancel";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String restCheckUrl;
    private final String simpleAutopasswordUrl;
    private final String pushConnectorUrl;
    private final String serverId;
    private final String serverKey;
    private final String corpId;

    public PasswordlessClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${passwordless.rest-check-url}") String restCheckUrl,
            @Value("${passwordless.simple-autopassword-url:}") String simpleAutopasswordUrl,
            @Value("${passwordless.push-connector-url}") String pushConnectorUrl,
            @Value("${passwordless.server-id}") String serverId,
            @Value("${passwordless.server-key}") String serverKey,
            @Value("${passwordless.corp-id:}") String corpId
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.restCheckUrl = trimTrailingSlash(restCheckUrl);
        this.simpleAutopasswordUrl = trimTrailingSlash(simpleAutopasswordUrl);
        this.pushConnectorUrl = pushConnectorUrl;
        this.serverId = serverId;
        this.serverKey = serverKey;
        this.corpId = corpId;
    }

    public boolean isRegistered(Long memberId) {
        PasswordlessRemoteResponse response = post(IS_AP, baseParams(memberId));
        return Optional.ofNullable(response.data())
                .map(data -> data.path("exist").asBoolean(false))
                .orElse(false);
    }

    public PasswordlessRegistrationData join(Long memberId) {
        PasswordlessRemoteResponse response = post(JOIN_AP, baseParams(memberId));
        JsonNode data = responseData(response);
        return new PasswordlessRegistrationData(
                fieldText(data, "qr"),
                fieldText(data, "corpId", corpId),
                fieldText(data, "registerKey"),
                data.path("terms").asInt(0),
                fieldText(data, "serverUrl", simpleAutopasswordUrl),
                fieldText(data, "userId", String.valueOf(memberId)),
                fieldText(data, "pushConnectorUrl", pushConnectorUrl),
                extractConnectorToken(response)
        );
    }

    public void withdraw(Long memberId) {
        post(WITHDRAWAL_AP, baseParams(memberId));
    }

    public String issueOneTimeToken(Long memberId) {
        PasswordlessRemoteResponse response = post(GET_TOKEN_FOR_ONE_TIME, baseParams(memberId));
        String encryptedToken = Optional.ofNullable(response.data())
                .map(data -> data.path("token").asText(""))
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 일회용 토큰이 없습니다."));
        return decryptOneTimeToken(encryptedToken);
    }

    public String requestAuthentication(Long memberId, String clientIp, String sessionId, String random) {
        MultiValueMap<String, String> params = baseParams(memberId);
        params.add("clientIp", clientIp);
        params.add("sessionId", sessionId);
        params.add("random", random);
        params.add("password", "");
        PasswordlessRemoteResponse response = post(GET_SP, params);
        return extractConnectorToken(response);
    }

    public String getResult(Long memberId, String sessionId, String random) {
        MultiValueMap<String, String> params = baseParams(memberId);
        params.add("sessionId", sessionId);
        params.add("random", random);
        PasswordlessRemoteResponse response = post(RESULT, params);
        return Optional.ofNullable(response.data())
                .map(data -> data.path("auth").asText(""))
                .orElse("");
    }

    public void cancel(Long memberId, String sessionId, String random) {
        MultiValueMap<String, String> params = baseParams(memberId);
        params.add("sessionId", sessionId);
        params.add("random", random);
        post(CANCEL, params);
    }

    public String getPushConnectorUrl() {
        return pushConnectorUrl;
    }

    private PasswordlessRemoteResponse post(String path, MultiValueMap<String, String> params) {
        validateConfiguration();

        String body;
        try {
            body = restClient.post()
                    .uri(restCheckUrl + path)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            log.warn("passwordless_remote_call_failed path={}", path, ex);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 서버 호출에 실패했습니다.");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            String result = root.path("result").asText("");
            String code = root.path("code").asText("");
            boolean success = result.equalsIgnoreCase("OK")
                    || root.path("success").asBoolean(false)
                    || code.equalsIgnoreCase("OK")
                    || (!StringUtils.hasText(result) && !StringUtils.hasText(code) && !root.path("data").isMissingNode());
            String message = root.path("message").asText(null);

            if (!success) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        StringUtils.hasText(message) ? message : "Passwordless 요청이 거절되었습니다.");
            }

            return new PasswordlessRemoteResponse(true, code, message, root.path("data"));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("passwordless_remote_response_parse_failed path={}", path, ex);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 서버 응답을 해석하지 못했습니다.");
        }
    }

    private MultiValueMap<String, String> baseParams(Long memberId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("userId", String.valueOf(memberId));
        params.add("serverId", serverId);
        params.add("serverKey", serverKey);
        if (StringUtils.hasText(corpId)) {
            params.add("corpId", corpId);
        }
        return params;
    }

    private String extractConnectorToken(PasswordlessRemoteResponse response) {
        JsonNode data = responseData(response);
        return fieldText(data, "pushConnectorToken",
                fieldText(data, "connectorToken", fieldText(data, "token")));
    }

    private JsonNode responseData(PasswordlessRemoteResponse response) {
        JsonNode data = response.data();
        if (data == null || data.isMissingNode() || data.isNull()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 응답 데이터가 비어 있습니다.");
        }

        if (!data.isTextual()) {
            return data;
        }

        try {
            return objectMapper.readTree(data.asText());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 응답 데이터 파싱에 실패했습니다.");
        }
    }

    private String fieldText(JsonNode data, String fieldName) {
        return fieldText(data, fieldName, "");
    }

    private String fieldText(JsonNode data, String fieldName, String fallback) {
        String value = data.path(fieldName).asText("");
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String decryptOneTimeToken(String encrypted) {
        byte[] key = serverKey.getBytes(StandardCharsets.UTF_8);
        try {
            SecretKey secureKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(key));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 일회용 토큰 복호화에 실패했습니다.");
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(restCheckUrl)
                || !StringUtils.hasText(pushConnectorUrl)
                || !StringUtils.hasText(serverId)
                || !StringUtils.hasText(serverKey)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 설정이 필요합니다.");
        }

        int keyLength = serverKey.getBytes(StandardCharsets.UTF_8).length;
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless serverKey 길이가 올바르지 않습니다.");
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
