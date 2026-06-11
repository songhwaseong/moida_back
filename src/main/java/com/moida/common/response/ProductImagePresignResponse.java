package com.moida.common.response;

import java.util.List;
import java.util.Map;

public record ProductImagePresignResponse(
        List<UploadResponse> uploads
) {
    public record UploadResponse(
            String key,
            String uploadUrl,
            String publicUrl,
            String expiresAt,
            Map<String, String> headers
    ) {
    }
}
