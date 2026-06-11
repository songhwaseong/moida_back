package com.moida.domain.product;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.ProductImagePresignRequest;
import com.moida.common.response.ProductImagePresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageStorageService {

    private static final int MAX_IMAGE_COUNT = 10;
    private static final String TEMP_PRODUCTS_PREFIX = "temp/products/";
    private static final String PERMANENT_PRODUCTS_PREFIX = "products/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");

    private final ProductImageStorageProperties properties;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    public ProductImagePresignResponse createPresignedUploads(
            ProductImagePresignRequest request,
            Long memberId
    ) {
        ensureConfigured();
        if (request == null || request.files() == null || request.files().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드할 이미지 파일을 선택해주세요.");
        }
        if (request.files().size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "상품 이미지는 최대 10장까지 등록할 수 있습니다.");
        }

        Instant expiresAt = Instant.now().plusSeconds(properties.getPresignTtlSeconds());
        List<ProductImagePresignResponse.UploadResponse> uploads = request.files().stream()
                .map(file -> createPresignedUpload(file, memberId, expiresAt))
                .toList();

        return new ProductImagePresignResponse(uploads);
    }

    public List<String> normalizeProductImageReferences(
            List<String> images,
            String legacySingleImage,
            Long memberId
    ) {
        List<String> source = images == null ? List.of() : images;
        List<String> normalized = source.stream()
                .filter(image -> image != null && !image.isBlank())
                .map(this::toStorageReference)
                .limit(MAX_IMAGE_COUNT)
                .collect(Collectors.toCollection(ArrayList::new));

        if (normalized.isEmpty() && legacySingleImage != null && !legacySingleImage.isBlank()) {
            normalized.add(toStorageReference(legacySingleImage));
        }

        normalized.forEach(image -> validateStorageReference(image, memberId));
        return normalized;
    }

    public List<String> promoteTempImages(List<String> images, Long memberId) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        ensureConfigured();
        return images.stream()
                .map(image -> promoteTempImage(image, memberId))
                .toList();
    }

    public String toPublicUrl(String storageReference) {
        if (storageReference == null || storageReference.isBlank()) {
            return storageReference;
        }
        String value = storageReference.trim();
        if (isLegacyExternalReference(value)) {
            return value;
        }
        if (properties.getPublicBaseUrl().isBlank()) {
            return value;
        }
        return properties.getPublicBaseUrl() + "/" + value;
    }

    private ProductImagePresignResponse.UploadResponse createPresignedUpload(
            ProductImagePresignRequest.FileRequest file,
            Long memberId,
            Instant expiresAt
    ) {
        String contentType = normalizeContentType(file == null ? null : file.contentType());
        validateFile(file, contentType);

        String key = buildObjectKey(memberId, contentType);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(java.time.Duration.ofSeconds(properties.getPresignTtlSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new ProductImagePresignResponse.UploadResponse(
                key,
                presigned.url().toString(),
                toPublicUrl(key),
                expiresAt.toString(),
                Map.of("Content-Type", contentType)
        );
    }

    private void validateFile(ProductImagePresignRequest.FileRequest file, String contentType) {
        if (file == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일 정보가 올바르지 않습니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "jpg, png, webp 이미지만 업로드할 수 있습니다.");
        }
        if (file.sizeBytes() == null || file.sizeBytes() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일 크기가 올바르지 않습니다.");
        }
        if (file.sizeBytes() > properties.getMaxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일은 10MB 이하만 업로드할 수 있습니다.");
        }
    }

    private void validateStorageReference(String image, Long memberId) {
        if (isLegacyExternalReference(image)) {
            return;
        }
        if (isOwnedProductKey(image, memberId)) {
            return;
        }
        if (!isOwnedTempProductKey(image, memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "상품 이미지 경로가 올바르지 않습니다.");
        }
    }

    private String buildObjectKey(Long memberId, String contentType) {
        LocalDate today = LocalDate.now();
        return "temp/products/%d/%s/%s/%s.%s".formatted(
                memberId,
                YEAR.format(today),
                MONTH.format(today),
                UUID.randomUUID(),
                extensionFor(contentType)
        );
    }

    private String promoteTempImage(String image, Long memberId) {
        String storageReference = toStorageReference(image);
        validateStorageReference(storageReference, memberId);

        if (isLegacyExternalReference(storageReference) || isOwnedProductKey(storageReference, memberId)) {
            return storageReference;
        }

        String permanentKey = storageReference.substring("temp/".length());
        copyTempImage(storageReference, permanentKey);
        deleteTempImage(storageReference);
        return permanentKey;
    }

    private void copyTempImage(String tempKey, String permanentKey) {
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .copySource(properties.getBucket() + "/" + tempKey)
                    .destinationBucket(properties.getBucket())
                    .destinationKey(permanentKey)
                    .build());
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 저장에 실패했습니다.");
        }
    }

    private void deleteTempImage(String tempKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(tempKey)
                    .build());
        } catch (S3Exception e) {
            log.warn("Failed to delete temporary product image. key={}", tempKey, e);
        }
    }

    private boolean isOwnedProductKey(String image, Long memberId) {
        return image.startsWith(PERMANENT_PRODUCTS_PREFIX + memberId + "/");
    }

    private boolean isOwnedTempProductKey(String image, Long memberId) {
        return image.startsWith(TEMP_PRODUCTS_PREFIX + memberId + "/");
    }

    private String toStorageReference(String image) {
        String value = image.trim();
        String publicBaseUrl = properties.getPublicBaseUrl();
        if (!publicBaseUrl.isBlank() && value.startsWith(publicBaseUrl + "/")) {
            String key = value.substring(publicBaseUrl.length() + 1);
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        }
        return value;
    }

    private boolean isLegacyExternalReference(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("data:image/")
                || lower.startsWith("http://")
                || lower.startsWith("https://");
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private void ensureConfigured() {
        if (properties.getBucket().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "S3 버킷 설정이 필요합니다.");
        }
        if (properties.getPublicBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 공개 URL 설정이 필요합니다.");
        }
    }
}
