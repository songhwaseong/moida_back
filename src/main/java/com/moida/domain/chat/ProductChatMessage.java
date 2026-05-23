package com.moida.domain.chat;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_chat_messages",
        indexes = {
                @Index(name = "idx_product_chat_message_room", columnList = "room_id"),
                @Index(name = "idx_product_chat_message_created", columnList = "created_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ProductChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessage.MessageType type;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount;

    @Builder
    private ProductChatMessage(ProductChatRoom room, Member sender, String content, ChatMessage.MessageType type) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.type = type == null ? ChatMessage.MessageType.TEXT : type;
        this.isDeleted = false;
        this.reportCount = 0;
    }

    public void hide() {
        this.isDeleted = true;
    }
}
