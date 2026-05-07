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
@Table(name = "chat_messages",
        indexes = @Index(name = "idx_message_room", columnList = "room_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Builder
    private ChatMessage(ChatRoom room, Member sender, String content, MessageType type) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.type = type == null ? MessageType.TEXT : type;
        this.isRead = false;
    }

    public void markAsRead() { this.isRead = true; }

    public enum MessageType { TEXT, IMAGE, SYSTEM }
}
