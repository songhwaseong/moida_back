package com.moida.domain.inquiry;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.InquiryRequest;
import com.moida.common.response.InquiryResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<InquiryResponse> getProductInquiries(Long productId) {
        // Validate that the product is visible before exposing its inquiry list.
        productRepository.findVisibleProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return inquiryRepository.findAllByProductIdOrderByCreatedAtDesc(productId).stream()
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

        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }
}
