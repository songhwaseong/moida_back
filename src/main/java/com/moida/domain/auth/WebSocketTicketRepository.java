package com.moida.domain.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;

public interface WebSocketTicketRepository extends JpaRepository<WebSocketTicket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WebSocketTicket t where t.ticketHash = :ticketHash")
    Optional<WebSocketTicket> findByHashForUpdate(@Param("ticketHash") String ticketHash);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
