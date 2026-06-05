package com.moida.domain.chat;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.ProductChatMessageRequest;
import com.moida.common.response.ProductChatMessageResponse;
import com.moida.common.response.ProductChatMessagesResponse;
import com.moida.common.response.ProductChatRoomResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductRepository;
import com.moida.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ProductChatService {

    private static final int DEFAULT_MESSAGE_SIZE = 50;
    private static final int MAX_MESSAGE_SIZE = 100;
    private static final Duration MESSAGE_COOLDOWN = Duration.ofSeconds(2);
    private static final Duration RECENT_MESSAGES_CACHE_TTL = Duration.ofSeconds(5);

    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;
    private final MemberRepository memberRepository;
    private final ProductChatRoomRepository productChatRoomRepository;
    private final ProductChatMessageRepository productChatMessageRepository;
    private final AdminActionLogService adminActionLogService;
    private final Map<Long, CachedRoomMessages> recentMessagesCache = new ConcurrentHashMap<>();

    // 상품/경매 상세의 최초 렌더링과 REST fallback에서 사용한다.
    // 응답에 실제 방 상태를 포함해 종료된 상품은 읽기 전용으로 보여준다.
    @Transactional(readOnly = true)
    public ProductChatMessagesResponse getMessages(Long productId, Long currentMemberId, Integer size) {
        // 상세 조회 정책과 동일하게 본인 PENDING/HIDDEN 도 허용한다.
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        assertVisibleFor(product, currentMemberId);
        ProductChatRoomStatus derivedStatus = deriveRoomStatus(product, null);

        return productChatRoomRepository.findByProductId(productId)
                .map(room -> {
                    ProductChatRoomStatus roomStatus = deriveRoomStatus(product, room);
                    if (roomStatus == ProductChatRoomStatus.HIDDEN) {
                        return new ProductChatMessagesResponse(roomStatus, List.of());
                    }
                    int pageSize = normalizeSize(size);
                    // 캐시에는 공통 메시지로 저장하고, mine 여부는 요청자 기준으로 다시 채운다.
                    List<ProductChatMessageResponse> messages = getCachedRecentMessages(room, pageSize).stream()
                            .map(message -> message.withMine(currentMemberId))
                            .toList();
                    return new ProductChatMessagesResponse(roomStatus, messages);
                })
                .orElseGet(() -> new ProductChatMessagesResponse(derivedStatus, List.of()));
    }

    // REST와 STOMP가 함께 사용하는 메시지 저장 경로다.
    // 검증, 방 생성, 도배 방지, 저장, 캐시 무효화를 한곳에서 처리한다.
    @Transactional
    public ProductChatMessageResponse createMessage(Long productId, Long memberId, ProductChatMessageRequest request) {
        // 상세 조회 정책과 동일하게 본인 PENDING/HIDDEN 도 허용한다.
        Product product = productRepository.findOwnProductDetail(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        assertVisibleFor(product, memberId);
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ProductChatRoom room = productChatRoomRepository.findByProductId(productId)
                .orElseGet(() -> productChatRoomRepository.save(ProductChatRoom.builder()
                        .product(product)
                        .build()));

        if (room.getStatus() == ProductChatRoomStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "This chat room is hidden.");
        }
        if (deriveRoomStatus(product, room) == ProductChatRoomStatus.READ_ONLY) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "This chat room is read-only.");
        }
        enforceMessageCooldown(room.getId(), memberId);

        String content = request.getContent().trim();
        ProductChatMessage message = ProductChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .type(ChatMessage.MessageType.TEXT)
                .build();
        room.updateLastMessage(content);

        ProductChatMessage saved = productChatMessageRepository.save(message);
        evictRecentMessages(room.getId());
        return ProductChatMessageResponse.from(saved, memberId);
    }

    @Transactional(readOnly = true)
    public List<ProductChatRoomResponse> getAdminRooms() {
        return productChatRoomRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(room -> ProductChatRoomResponse.from(room, productChatMessageRepository.countByRoomId(room.getId())))
                .toList();
    }

    @Transactional
    public ProductChatRoomResponse changeRoomStatus(Long roomId, ProductChatRoomStatus status) {
        ProductChatRoom room = productChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        ProductChatRoomStatus previousStatus = room.getStatus();
        room.changeStatus(status);
        adminActionLogService.record(
                "CHAT_ROOM_STATUS_CHANGE",
                "CHAT_ROOM",
                room.getId(),
                room.getProduct().getName(),
                adminActionLogService.fields("status", previousStatus),
                adminActionLogService.fields("status", room.getStatus()),
                "채팅방 상태 변경"
        );
        // 방 상태 변경은 사용자가 다시 입장할 때 즉시 반영되어야 한다.
        evictRecentMessages(room.getId());
        return ProductChatRoomResponse.from(room, productChatMessageRepository.countByRoomId(room.getId()));
    }

    @Transactional
    public ProductChatMessageResponse hideMessage(Long messageId, Long adminId) {
        ProductChatMessage message = productChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        Object beforeValue = adminActionLogService.fields(
                "hidden", message.getIsDeleted(),
                "content", message.getContent(),
                "senderId", message.getSender().getId()
        );
        message.hide();
        adminActionLogService.record(
                "CHAT_MESSAGE_HIDE",
                "CHAT_MESSAGE",
                message.getId(),
                message.getRoom().getProduct().getName(),
                beforeValue,
                adminActionLogService.fields(
                        "hidden", message.getIsDeleted(),
                        "content", message.getContent(),
                        "senderId", message.getSender().getId()
                ),
                "채팅 메시지 숨김"
        );
        // 숨김 처리한 메시지는 최근 메시지 캐시에서도 즉시 사라져야 한다.
        evictRecentMessages(message.getRoom().getId());
        return ProductChatMessageResponse.from(message, adminId);
    }

    private int normalizeSize(Integer size) {
        if (size == null) return DEFAULT_MESSAGE_SIZE;
        return Math.max(1, Math.min(size, MAX_MESSAGE_SIZE));
    }

    // ProductService.getProduct 와 동일한 가시성 정책.
    // DELETED 는 모두 차단, PENDING/HIDDEN/환수 진행 상태는 본인만 허용한다.
    private void assertVisibleFor(Product product, Long memberId) {
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
    }

    private ProductChatRoomStatus deriveRoomStatus(Product product, ProductChatRoom room) {
        // 관리자가 지정한 상태를 우선 적용하고, 그 다음 상품/경매 종료 상태로 쓰기 가능 여부를 결정한다.
        if (room != null && room.getStatus() == ProductChatRoomStatus.HIDDEN) {
            return ProductChatRoomStatus.HIDDEN;
        }
        if (room != null && room.getStatus() == ProductChatRoomStatus.READ_ONLY) {
            return ProductChatRoomStatus.READ_ONLY;
        }
        if (isClosedProduct(product)) {
            return ProductChatRoomStatus.READ_ONLY;
        }
        if (auctionRepository.findByProductId(product.getId())
                .map(auction -> auction.isEnded())
                .orElse(false)) {
            return ProductChatRoomStatus.READ_ONLY;
        }
        return ProductChatRoomStatus.ACTIVE;
    }

    private boolean isClosedProduct(Product product) {
        return product.getStatus() == ProductStatus.SOLD
                || product.getStatus() == ProductStatus.FAILED
                || product.getStatus() == ProductStatus.RETURN_REQUESTED
                || product.getStatus() == ProductStatus.RETURN_SHIPPING
                || product.getStatus() == ProductStatus.RETURN_COMPLETED
                || product.getStatus() == ProductStatus.HIDDEN
                || product.getStatus() == ProductStatus.DELETED;
    }

    private void enforceMessageCooldown(Long roomId, Long memberId) {
        // 프론트 제한을 우회해도 서버에서 같은 사용자의 연속 전송을 막는다.
        productChatMessageRepository.findRecentByRoomIdAndSenderId(
                        roomId,
                        memberId,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .findFirst()
                .ifPresent(message -> {
                    LocalDateTime availableAt = message.getCreatedAt().plus(MESSAGE_COOLDOWN);
                    if (availableAt.isAfter(LocalDateTime.now())) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT, "Messages can be sent once every 2 seconds.");
                    }
                });
    }

    private List<ProductChatMessageResponse> getCachedRecentMessages(ProductChatRoom room, int pageSize) {
        // 활성 채팅방에서 상세 페이지 조회가 반복될 때 DB 조회를 줄이기 위한 짧은 캐시다.
        // 백엔드 인스턴스가 여러 대가 되면 Redis 같은 공유 캐시로 옮기는 것이 좋다.
        CachedRoomMessages cached = recentMessagesCache.get(room.getId());
        if (cached != null && cached.isFresh() && cached.messages().size() >= pageSize) {
            return cached.messages().subList(cached.messages().size() - pageSize, cached.messages().size());
        }

        List<ProductChatMessageResponse> messages = new ArrayList<>(productChatMessageRepository
                .findRecentByRoomId(
                        room.getId(),
                        PageRequest.of(0, MAX_MESSAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .map(message -> ProductChatMessageResponse.from(message, null))
                .toList());
        Collections.reverse(messages);

        recentMessagesCache.put(room.getId(), new CachedRoomMessages(
                List.copyOf(messages),
                LocalDateTime.now().plus(RECENT_MESSAGES_CACHE_TTL)
        ));

        if (messages.size() <= pageSize) {
            return messages;
        }
        return messages.subList(messages.size() - pageSize, messages.size());
    }

    private void evictRecentMessages(Long roomId) {
        recentMessagesCache.remove(roomId);
    }

    private record CachedRoomMessages(
            List<ProductChatMessageResponse> messages,
            LocalDateTime expiresAt
    ) {
        private boolean isFresh() {
            return expiresAt.isAfter(LocalDateTime.now());
        }
    }
}
