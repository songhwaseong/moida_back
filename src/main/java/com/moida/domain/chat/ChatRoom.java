package com.moida.domain.chat;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room",
                columnNames = {"product_id", "buyer_id", "seller_id"}),
        indexes = {
                @Index(name = "idx_chatroom_buyer", columnList = "buyer_id"),
                @Index(name = "idx_chatroom_seller", columnList = "seller_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed;

    @Builder
    private ChatRoom(Product product, Member buyer, Member seller) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
        this.isClosed = false;
    }

    public void updateLastMessage(String message) { this.lastMessage = message; }

    public void close() { this.isClosed = true; }
}
