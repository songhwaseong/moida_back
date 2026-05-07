package com.moida.domain.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findAllByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    long countByRoomIdAndIsReadFalseAndSenderIdNot(Long roomId, Long memberId);
}
