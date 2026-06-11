package com.moida.domain.inquiry;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.InquiryRequest;
import com.moida.common.response.InquiryResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductImageStorageService;
import com.moida.domain.product.ProductRepository;
import com.moida.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final ProductImageStorageService productImageStorageService;

    @Transactional(readOnly = true)
    public List<InquiryResponse> getProductInquiries(Long productId, Long memberId) {
        // 상세 조회 정책과 동일하게 본인 PENDING/HIDDEN/환수 진행 상품도 허용한다.
        // (findVisibleProductDetail 만 쓰면 본인 비공개 상품의 문의 목록이 항상 404 가 된다.)
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductStatus status = product.getStatus();
        boolean isOwner = memberId != null && product.isOwnedBy(memberId);
        if (status == ProductStatus.DELETED
                || ((status == ProductStatus.PENDING
                || status == ProductStatus.HIDDEN
                || status == ProductStatus.RETURN_REQUESTED
                || status == ProductStatus.RETURN_SHIPPING
                || status == ProductStatus.RETURN_COMPLETED) && !isOwner)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return inquiryRepository.findAllByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getMyInquiries(Long memberId) {
        return inquiryRepository.findMyInquiries(memberId, PageRequest.of(0, 100)).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @Transactional
    public InquiryResponse createProductInquiry(Long productId, Long memberId, InquiryRequest request) {
        Product product = productRepository.findVisibleProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        Member user = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // Sellers can answer inquiries elsewhere, but should not create one on their own product.
        if (product.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Cannot create an inquiry on your own product.");
        }

        Inquiry inquiry = Inquiry.builder()
                .product(product)
                .user(user)
                .seller(product.getSeller())
                .question(request.getQuestion().trim())
                .isSecret(request.getIsSecret())
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        notificationService.createAndPush(
                product.getSeller(),
                Notification.NotificationType.INQUIRY_NEW,
                "새 상품 문의가 도착했어요",
                String.format("'%s' 상품에 새 문의가 등록되었습니다.", product.getName()),
                "/products/" + product.getId()
        );

        return InquiryResponse.from(saved, productImageStorageService::toPublicUrl);
    }
}
