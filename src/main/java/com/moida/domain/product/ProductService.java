package com.moida.domain.product;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.ProductDetailResponse;
import com.moida.common.response.ProductSummaryResponse;
import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.auction.AuctionStatus;
import com.moida.domain.auction.Bid;
import com.moida.domain.auction.BidRepository;
import com.moida.domain.category.Category;
import com.moida.domain.category.CategoryRepository;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.common.request.ProductRequest;
import com.moida.common.request.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final Set<String> SUPPORTED_CARRIER_CODES = Set.of("01", "04", "05", "06", "08", "11", "12", "23");

    // 판매자가 직접 수정할 수 있는 상품 상태(경매예정/보완요청/유찰/숨김).
    private static final Set<ProductStatus> USER_EDITABLE_STATUSES =
            Set.of(ProductStatus.SCHEDULED, ProductStatus.NEEDS_REVISION, ProductStatus.FAILED, ProductStatus.HIDDEN);
    private static final Set<ProductStatus> USER_RETURN_REQUESTABLE_STATUSES =
            Set.of(ProductStatus.PENDING, ProductStatus.SCHEDULED, ProductStatus.FAILED);
    // 수정 시 사용자가 지정할 수 있는 상태(승인요청=PENDING / 숨김=HIDDEN). 경매예정·진행중·낙찰 등은 관리자·시스템이 관리한다.
    // 상품을 수정하면 다시 관리자 승인을 받도록 PENDING 으로 되돌릴 수 있다.
    private static final Set<ProductStatus> USER_SETTABLE_STATUSES =
            Set.of(ProductStatus.PENDING, ProductStatus.HIDDEN);
    private static final String DEFAULT_LOCATION = "전국 배송";

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductLikeRepository productLikeRepository;
    private final ProductImageStorageService productImageStorageService;

    @Transactional
    public Long create(ProductRequest request, Long memberId) {
        log.info("[ProductService] create start memberId={}, category={}, name={}",
                memberId, request.getCategory(), request.getName());

        // 1. 판매자 조회 (JWT 토큰에서 추출한 memberId 사용)
        Member seller = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        log.debug("[ProductService] create seller loaded sellerId={}, sellerName={}",
                seller.getId(), seller.getName());

        // 2. 카테고리 조회 (프론트에서 넘어온 카테고리 이름으로 검색)
        Category category = categoryRepository.findByNameAndIsActiveTrue(request.getCategory())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 카테고리입니다."));
        log.debug("[ProductService] create category loaded categoryId={}, categoryName={}",
                category.getId(), category.getName());

        // 3. 상품 번호 자동 생성 (예: 2026051600001)
        String productNo = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", productRepository.count() + 1);

        List<String> requestImages = resolveRequestImages(request.getImages(), request.getImage(), memberId);
        requestImages = productImageStorageService.promoteTempImages(requestImages, memberId);
        int mainImageIndex = resolveMainImageIndex(request.getMainImageIndex(), requestImages.size());
        // mainImageUrl drives list thumbnails; product_images keeps the full detail gallery.
        String mainImageUrl = requestImages.isEmpty() ? null : requestImages.get(mainImageIndex);
        String carrierCode = normalizeShipmentValue(request.getCarrierCode());
        String trackingNo = normalizeTrackingNo(request.getTrackingNo());
        validateShipment(carrierCode, trackingNo);
        validateAuctionPrices(request.getPrice(), request.getMinBidUnit(), request.getBuyNowPrice());

        // 4. Product 엔티티 생성 (프로젝트가 경매-only로 피벗되어 type은 AUCTION 고정)
        // immediatePrice / minBidUnit 은 등록 단계에서 입력받아 Product 에 함께 보관한다.
        // 관리자가 SCHEDULED → LIVE 로 전환할 때 AdminProductService 가 Auction 으로 옮겨 사용한다.
        Product product = Product.builder()
                .productNo(productNo)
                .seller(seller)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .type(ProductType.AUCTION)
                .condition(request.toProductCondition()) // "S급" → ProductCondition.S
                .price(request.getPrice())
                .location(normalizeLocation(request.getLocation()))
                .carrierCode(carrierCode)
                .trackingNo(trackingNo)
                .mainImageUrl(mainImageUrl)        // Base64 문자열 (추후 S3 등으로 교체)
                .immediatePrice(request.getBuyNowPrice())
                .minBidUnit(request.getMinBidUnit())
                .build();

        // 5. 이미지 등록 (대표 이미지)
        // Store every submitted image in display order and mark the selected main image.
        for (int i = 0; i < requestImages.size(); i++) {
            ProductImage productImage = ProductImage.builder()
                    .url(requestImages.get(i))
                    .displayOrder(i)
                    .isMain(i == mainImageIndex)
                    .build();
            product.addImage(productImage);
        }

        // 6. DB 저장
        Product saved = productRepository.save(product);
        log.info("[ProductService] create saved productId={}, productNo={}",
                saved.getId(), saved.getProductNo());

        return saved.getId(); // 저장된 상품 ID 반환
    }
    // 판매자 본인이 자신의 상품을 수정한다.
    // 수정 가능한 상태(경매예정/보완요청/유찰/숨김)만 허용하고, 상태 변경은 승인요청/숨김(PENDING/HIDDEN)으로 제한한다.
    // (LIVE/SOLD/PENDING/DELETED 는 진행 중이거나 검수/종료 상태라 사용자 임의 수정을 막는다.)
    @Transactional
    public Long updateMyProduct(Long productId, Long memberId, ProductUpdateRequest request) {
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PRODUCT_OWNER);
        }
        if (!USER_EDITABLE_STATUSES.contains(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "경매예정·보완요청·유찰·숨김 상태의 상품만 수정할 수 있습니다.");
        }

        Category category = null;
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            category = categoryRepository.findByNameAndIsActiveTrue(request.getCategory())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 카테고리입니다."));
        }

        Long nextPrice = request.getPrice() != null ? request.getPrice() : product.getPrice();
        Long nextMinBidUnit = request.getMinBidUnit() != null ? request.getMinBidUnit() : product.getMinBidUnit();
        validateAuctionPrices(nextPrice, nextMinBidUnit, request.getImmediatePrice());

        // null 필드는 Product.update 에서 건너뛰므로 전달된 값만 반영된다.
        product.update(
                request.getName(),
                request.getDescription(),
                category,
                request.toProductCondition(),
                request.getPrice(),
                request.getLocation()
        );
        product.changeMinBidUnit(request.getMinBidUnit());
        product.changeImmediatePrice(request.getImmediatePrice());

        // 이미지가 전달된 경우에만 갤러리 전체를 교체한다. (빈 배열이면 기존 이미지 유지)
        List<String> requestImages = resolveRequestImages(request.getImages(), null, memberId);
        requestImages = productImageStorageService.promoteTempImages(requestImages, memberId);
        if (!requestImages.isEmpty()) {
            int mainImageIndex = resolveMainImageIndex(request.getMainImageIndex(), requestImages.size());
            product.clearImages();
            for (int i = 0; i < requestImages.size(); i++) {
                product.addImage(ProductImage.builder()
                        .url(requestImages.get(i))
                        .displayOrder(i)
                        .isMain(i == mainImageIndex)
                        .build());
            }
            product.changeMainImage(requestImages.get(mainImageIndex));
        }

        ProductStatus targetStatus = parseUserSettableStatus(request.getStatus());
        if (targetStatus != null) {
            product.changeStatus(targetStatus);
            if (targetStatus == ProductStatus.PENDING) {
                product.clearRevisionRequest();
            }
        }

        log.info("[ProductService] updateMyProduct productId={}, memberId={}, status={}",
                product.getId(), memberId, product.getStatus());
        return product.getId();
    }

    @Transactional
    public Long requestReturn(Long productId, Long memberId, String reason) {
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PRODUCT_OWNER);
        }
        if (!USER_RETURN_REQUESTABLE_STATUSES.contains(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "승인요청중·경매예정·유찰 상태의 상품만 환수요청할 수 있습니다.");
        }

        product.requestReturn(normalizeReturnReason(reason));
        log.info("[ProductService] requestReturn productId={}, memberId={}, status={}",
                product.getId(), memberId, product.getStatus());
        return product.getId();
    }

    private ProductStatus parseUserSettableStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        ProductStatus parsed;
        try {
            parsed = ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 상품 상태입니다.");
        }
        if (!USER_SETTABLE_STATUSES.contains(parsed)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "변경할 수 없는 상품 상태입니다.");
        }
        return parsed;
    }

    @Transactional
    public void approveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "승인 대기 상태의 상품만 승인할 수 있습니다.");
        }
        product.changeStatus(ProductStatus.SCHEDULED);
    }

    @Transactional(readOnly = true)
    public List<Product> findPendingProducts() {
        return productRepository.findAllByStatus(ProductStatus.PENDING, Pageable.unpaged()).getContent();
    }


    private String normalizeShipmentValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeTrackingNo(String value) {
        String normalized = normalizeShipmentValue(value);
        return normalized == null ? null : normalized.replaceAll("[^0-9]", "");
    }

    private String normalizeLocation(String value) {
        return value == null || value.isBlank() ? DEFAULT_LOCATION : value.trim();
    }

    private void validateShipment(String carrierCode, String trackingNo) {
        if (carrierCode == null || trackingNo == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "택배사와 송장번호를 입력해주세요.");
        }
        if (trackingNo != null && !trackingNo.matches("\\d{10,14}")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "송장번호는 10~14자리 숫자로 입력해주세요.");
        }
        if (carrierCode != null && !SUPPORTED_CARRIER_CODES.contains(carrierCode)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 택배사입니다.");
        }
    }

    private void validateAuctionPrices(Long startPrice, Long minBidUnit, Long immediatePrice) {
        if (startPrice == null || startPrice <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "경매 시작가는 1원 이상이어야 합니다.");
        }
        if (minBidUnit == null || minBidUnit <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "최소 호가 단위는 1원 이상이어야 합니다.");
        }
        if (immediatePrice != null && immediatePrice < startPrice + minBidUnit) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "즉시낙찰가는 경매 시작가와 최소 호가 단위를 더한 금액 이상이어야 합니다.");
        }
    }

    // 홈/인기 화면 상품 카드 리스트 조회.
    // status 파라미터로 두 섹션을 구분한다.
    //   - LIVE      → "🔴 실시간 경매" 섹션
    //   - SCHEDULED → "경매 예정 매물" 섹션
    // sort=popular 면 조회수(viewCount) 우선 정렬, 기본은 최신 등록순(createdAt DESC).
    // category 가 들어오면 해당 카테고리만 필터링하며, size 는 한 페이지 최대 100건으로 클램프한다.
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getProducts(String category, String status, String sort, int size) {
        log.info("[ProductService] getProducts start category={}, status={}, sort={}, size={}", category, status, sort, size);
        ProductStatus productStatus = parseProductStatus(status);
        String categoryName = normalizeBlank(category);
        int pageSize = Math.max(1, Math.min(size, 100));
        Sort sortSpec = resolveSort(sort);
        log.debug("[ProductService] getProducts normalized category={}, status={}, sort={}, pageSize={}",
                categoryName, productStatus, sortSpec, pageSize);

        List<Product> products = productRepository.findVisibleProducts(
                null,
                categoryName,
                productStatus,
                PageRequest.of(0, pageSize, sortSpec)
        );
        log.info("[ProductService] getProducts productCount={}", products.size());

        // 상품 목록은 Product 중심으로 조회하고, 경매 부가 정보는 product_id 기준으로 한 번에 붙인다.
        Map<Long, Auction> auctionsByProductId = findAuctionsByProductId(products);
        log.debug("[ProductService] getProducts auctionInfoCount={}", auctionsByProductId.size());

        // "실시간 경매" 섹션은 product.status=LIVE 뿐 아니라 auction.status=LIVE 도 함께 만족해야 한다.
        // 상태 전이가 한쪽만 진행된 불일치 데이터(예: 스케줄러 누락)가 노출되지 않도록 2차 검증한다.
        boolean liveOnly = productStatus == ProductStatus.LIVE;

        List<ProductSummaryResponse> responses = products.stream()
                .filter(product -> {
                    if (!liveOnly) return true;
                    Auction auction = auctionsByProductId.get(product.getId());
                    return auction != null && auction.getStatus() == AuctionStatus.LIVE;
                })
                .map(product -> ProductSummaryResponse.from(
                        product,
                        auctionsByProductId.get(product.getId()),
                        productImageStorageService::toPublicUrl
                ))
                .toList();

        // 가격 정렬은 currentPrice 가 채워진 뒤(=DTO 변환 이후) 적용해야 정확하다.
        return applyPriceSortIfNeeded(responses, sort);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getMyProducts(Long memberId) {
        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(memberId, PageRequest.of(0, 100));
        Map<Long, Auction> auctionsByProductId = findAuctionsByProductId(products);

        return products.stream()
                .filter(product -> product.getStatus() != ProductStatus.DELETED)
                .map(product -> ProductSummaryResponse.from(
                        product,
                        auctionsByProductId.get(product.getId()),
                        productImageStorageService::toPublicUrl
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getSellerProducts(Long sellerId) {
        if (!memberRepository.existsById(sellerId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId, PageRequest.of(0, 100));
        Map<Long, Auction> auctionsByProductId = findAuctionsByProductId(products);

        return products.stream()
                .filter(product -> product.getStatus() != ProductStatus.DELETED)
                .filter(product -> product.getStatus() != ProductStatus.HIDDEN)
                .filter(product -> product.getStatus() != ProductStatus.PENDING)
                .filter(product -> product.getStatus() != ProductStatus.NEEDS_REVISION)
                .filter(product -> product.getStatus() != ProductStatus.RETURN_REQUESTED)
                .filter(product -> product.getStatus() != ProductStatus.RETURN_SHIPPING)
                .filter(product -> product.getStatus() != ProductStatus.RETURN_COMPLETED)
                .map(product -> ProductSummaryResponse.from(
                        product,
                        auctionsByProductId.get(product.getId()),
                        productImageStorageService::toPublicUrl
                ))
                .toList();
    }

    // 상품 상세 조회.
    // memberId 가 주어지면(=로그인 상태) 해당 사용자의 좋아요 여부를 함께 채워
    // 상세 화면 진입 시 하트 활성 상태가 즉시 정확하게 보이도록 한다.
    // memberId 가 null 이면(=비로그인) liked 는 항상 false 로 응답한다.
    // 또한 매 진입 시 viewCount 를 1 증가시켜 인기 정렬에 반영한다.
    // 판매자 본인의 조회는 카운트하지 않아 자기 상품을 새로고침해 수치를 부풀리는 것을 막는다.
    @Transactional
    public ProductDetailResponse getProduct(Long productId, Long memberId) {
        log.info("[ProductService] getProduct start productId={}, memberId={}", productId, memberId);
        // 상세 화면은 판매자, 카테고리, 이미지 목록까지 함께 필요하므로 전용 fetch query를 사용한다.
        // 판매자 본인은 자신의 PENDING/NEEDS_REVISION/HIDDEN 상품도 볼 수 있어야 하므로,
        // 상태 필터 없는 쿼리로 먼저 가져와 본인 여부를 확인한 뒤 가시성 정책을 적용한다.
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductStatus status = product.getStatus();
        boolean isOwner = memberId != null && product.isOwnedBy(memberId);
        // DELETED 는 본인도 조회 불가. PENDING/NEEDS_REVISION/HIDDEN 은 본인만 조회 가능.
        if (status == ProductStatus.DELETED
                || ((status == ProductStatus.PENDING
                || status == ProductStatus.NEEDS_REVISION
                || status == ProductStatus.HIDDEN
                || status == ProductStatus.RETURN_REQUESTED
                || status == ProductStatus.RETURN_SHIPPING
                || status == ProductStatus.RETURN_COMPLETED) && !isOwner)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Auction auction = auctionRepository.findByProductId(product.getId()).orElse(null);
        log.info("[ProductService] getProduct loaded productId={}, productNo={}, hasAuction={}",
                product.getId(), product.getProductNo(), auction != null);
        List<Bid> bids = auction == null ? Collections.emptyList() : bidRepository.findHistoryByAuctionId(auction.getId());
        log.debug("[ProductService] getProduct bidHistoryCount={}", bids.size());

        // 판매자 본인이 아니면 조회수 증가. JPA dirty checking 으로 트랜잭션 커밋 시 자동 flush.
        if (memberId == null || !product.isOwnedBy(memberId)) {
            product.increaseViewCount();
        }

        boolean liked = memberId != null
                && productLikeRepository.existsByProductIdAndMemberId(product.getId(), memberId);
        return ProductDetailResponse.from(product, auction, bids, liked, memberId, productImageStorageService::toPublicUrl);
    }

    // 인기 정렬은 viewCount 우선 + 같은 값일 때 id 역순(최근 등록 우선)으로 결정적 정렬을 보장.
    // 가격 정렬(price_asc / price_desc)은 경매의 현재 입찰가(currentPrice) 기준이어야 하므로
    // DB 정렬은 기본값(createdAt DESC)으로 두고, 결과를 만든 뒤 Java 단계에서 재정렬한다.
    // 그 외(또는 null)는 최신 등록순(createdAt DESC) 기본값.
    private Sort resolveSort(String sort) {
        String normalized = normalizeBlank(sort);
        if (normalized == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        if ("popular".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.DESC, "viewCount")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
        }
        if (isPriceSort(normalized)) {
            // price 정렬은 후처리에서 적용하므로 DB 단계에서는 기본 정렬을 쓴다.
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid sort key.");
    }

    // 가격 정렬 키 여부 판별 (대소문자 무시).
    private boolean isPriceSort(String sort) {
        return "price_asc".equalsIgnoreCase(sort) || "price_desc".equalsIgnoreCase(sort);
    }

    // 카드에 표시되는 가격(=auction.currentPrice, 없으면 product.price)을 기준으로 정렬한다.
    // size <= 100 클램프 덕분에 인메모리 정렬 비용은 무시 가능하다.
    private List<ProductSummaryResponse> applyPriceSortIfNeeded(
            List<ProductSummaryResponse> items, String sort
    ) {
        if (!isPriceSort(sort)) return items;
        boolean asc = "price_asc".equalsIgnoreCase(sort);
        Comparator<ProductSummaryResponse> byDisplayedPrice = Comparator.comparingLong(item -> {
            Long current = item.currentPrice();
            return current != null ? current : (item.price() != null ? item.price() : 0L);
        });
        if (!asc) byDisplayedPrice = byDisplayedPrice.reversed();
        // 같은 가격일 때는 id 역순(최근 등록 우선)으로 결정적 순서를 보장한다.
        Comparator<ProductSummaryResponse> tiebreaker = Comparator
                .comparingLong(ProductSummaryResponse::id)
                .reversed();
        return items.stream()
                .sorted(byDisplayedPrice.thenComparing(tiebreaker))
                .toList();
    }

    private ProductStatus parseProductStatus(String status) {
        String normalized = normalizeBlank(status);
        if (normalized == null) return null;

        try {
            return ProductStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid product status.");
        }
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeReturnReason(String reason) {
        String normalized = normalizeBlank(reason);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "환수요청 사유를 입력해주세요.");
        }
        if (normalized.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "환수요청 사유는 500자 이내로 입력해주세요.");
        }
        return normalized;
    }

    private List<String> resolveRequestImages(List<String> images, String legacySingleImage, Long memberId) {
        return productImageStorageService.normalizeProductImageReferences(images, legacySingleImage, memberId);
    }

    private int resolveMainImageIndex(Integer requestedIndex, int imageCount) {
        if (imageCount <= 0) return 0;
        if (requestedIndex == null) return 0;
        return Math.max(0, Math.min(requestedIndex, imageCount - 1));
    }

    // 상품 묶음에 매핑되는 경매 정보를 한 번의 IN 쿼리로 모아 productId 키 맵으로 반환한다.
    // 카드 렌더링 시 상품마다 별도 쿼리가 나가는 N+1 을 방지한다.
    private Map<Long, Auction> findAuctionsByProductId(List<Product> products) {
        if (products.isEmpty()) return Collections.emptyMap();

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        return auctionRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(auction -> auction.getProduct().getId(), auction -> auction));
    }
}
