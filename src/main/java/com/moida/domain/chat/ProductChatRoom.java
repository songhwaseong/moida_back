package com.moida.domain.chat;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_chat_room_product",
                columnNames = "product_id"),
        indexes = {
                @Index(name = "idx_product_chat_room_product", columnList = "product_id"),
                @Index(name = "idx_product_chat_room_status", columnList = "status")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductChatRoomStatus status;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Builder
    private ProductChatRoom(Product product) {
        this.product = product;
        this.status = ProductChatRoomStatus.ACTIVE;
    }

    public void updateLastMessage(String message) {
        this.lastMessage = message;
    }

    public void changeStatus(ProductChatRoomStatus status) {
        this.status = status;
    }
}
