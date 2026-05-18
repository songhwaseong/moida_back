package com.moida.domain.product;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.domain.category.Category;
import com.moida.domain.category.CategoryRepository;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.common.request.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Long create(ProductRequest request, Long memberId) {

        // 1. 판매자 조회 (JWT 토큰에서 추출한 memberId 사용)
        Member seller = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 카테고리 조회 (프론트에서 넘어온 카테고리 이름으로 검색)
        Category category = categoryRepository.findByNameAndIsActiveTrue(request.getCategory())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 카테고리입니다."));

        // 3. 상품 번호 자동 생성 (예: 2026051600001)
        String productNo = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", productRepository.count() + 1);

        // 4. Product 엔티티 생성
        Product product = Product.builder()
                .productNo(productNo)
                .seller(seller)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .type(request.toProductType())           // "AUCTION" / "TRADE" → enum
                .condition(request.toProductCondition()) // "S급" → ProductCondition.S
                .price(request.getPrice())
                .location(request.getLocation())
                .mainImageUrl(request.getImage())        // Base64 문자열 (추후 S3 등으로 교체)
                .build();

        // 5. 이미지 등록 (대표 이미지)
        if (request.getImage() != null && !request.getImage().isBlank()) {
            ProductImage mainImage = ProductImage.builder()
                    .url(request.getImage())
                    .displayOrder(0)
                    .isMain(true)
                    .build();
            product.addImage(mainImage);
        }

        // 6. DB 저장
        Product saved = productRepository.save(product);

        return saved.getId(); // 저장된 상품 ID 반환
    }
}
