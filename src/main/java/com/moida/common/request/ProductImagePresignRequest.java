package com.moida.common.request;

import java.util.List;

public record ProductImagePresignRequest(
        List<FileRequest> files
) {
    public record FileRequest(
            String fileName,
            String contentType,
            Long sizeBytes
    ) {
    }
}
