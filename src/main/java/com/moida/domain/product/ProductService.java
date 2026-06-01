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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductLikeRepository productLikeRepository;

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

        List<String> requestImages = resolveRequestImages(request);
        int mainImageIndex = resolveMainImageIndex(request.getMainImageIndex(), requestImages.size());
        // mainImageUrl drives list thumbnails; product_images keeps the full detail gallery.
        String mainImageUrl = requestImages.isEmpty() ? null : requestImages.get(mainImageIndex);

        // 4. Product 엔티티 생성 (프로젝트가 경매-only로 피벗되어 type은 AUCTION 고정)
        Product product = Product.builder()
                .productNo(productNo)
                .seller(seller)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .type(ProductType.AUCTION)
                .condition(request.toProductCondition()) // "S급" → ProductCondition.S
                .price(request.getPrice())
                .location(request.getLocation())
                .mainImageUrl(mainImageUrl)        // Base64 문자열 (추후 S3 등으로 교체)
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
                .map(product -> ProductSummaryResponse.from(product, auctionsByProductId.get(product.getId())))
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
                .map(product -> ProductSummaryResponse.from(product, auctionsByProductId.get(product.getId())))
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
        // 판매자 본인은 자신의 PENDING/HIDDEN 상품도 볼 수 있어야 하므로,
        // 상태 필터 없는 쿼리로 먼저 가져와 본인 여부를 확인한 뒤 가시성 정책을 적용한다.
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductStatus status = product.getStatus();
        boolean isOwner = memberId != null && product.isOwnedBy(memberId);
        // DELETED 는 본인도 조회 불가. PENDING/HIDDEN 은 본인만 조회 가능.
        if (status == ProductStatus.DELETED
                || ((status == ProductStatus.PENDING || status == ProductStatus.HIDDEN) && !isOwner)) {
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
        return ProductDetailResponse.from(product, auction, bids, liked, memberId);
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

    private List<String> resolveRequestImages(ProductRequest request) {
        List<String> images = request.getImages() == null ? Collections.emptyList() : request.getImages();
        // Support both the new images array and the legacy single image field.
        List<String> normalized = images.stream()
                .filter(image -> image != null && !image.isBlank())
                .limit(10)
                .collect(Collectors.toCollection(ArrayList::new));

        if (normalized.isEmpty() && request.getImage() != null && !request.getImage().isBlank()) {
            normalized.add(request.getImage());
        }

        return normalized;
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
