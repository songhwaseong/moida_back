package com.moida.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "websocket_tickets", indexes = {
        @Index(name = "uk_websocket_ticket_hash", columnList = "ticket_hash", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebSocketTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "websocket_ticket_id")
    private Long id;

    @Column(name = "ticket_hash", nullable = false, length = 64)
    private String ticketHash;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    private WebSocketTicket(String ticketHash, Long memberId, LocalDateTime expiresAt) {
        this.ticketHash = ticketHash;
        this.memberId = memberId;
        this.expiresAt = expiresAt;
    }

    public static WebSocketTicket create(String ticketHash, Long memberId, LocalDateTime expiresAt) {
        return new WebSocketTicket(ticketHash, memberId, expiresAt);
    }

    public boolean canConsume(LocalDateTime now) {
        return !consumed && expiresAt.isAfter(now);
    }

    public void consume() {
        this.consumed = true;
    }
}
