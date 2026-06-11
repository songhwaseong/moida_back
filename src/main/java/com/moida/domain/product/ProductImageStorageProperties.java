package com.moida.domain.product;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "moida.images")
public class ProductImageStorageProperties {

    private String bucket = "";
    private String region = "ap-northeast-2";
    private String publicBaseUrl = "";
    private long presignTtlSeconds = 300;
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket == null ? "" : bucket.trim();
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region == null || region.isBlank() ? "ap-northeast-2" : region.trim();
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : trimTrailingSlash(publicBaseUrl.trim());
    }

    public long getPresignTtlSeconds() {
        return presignTtlSeconds;
    }

    public void setPresignTtlSeconds(long presignTtlSeconds) {
        this.presignTtlSeconds = presignTtlSeconds;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
