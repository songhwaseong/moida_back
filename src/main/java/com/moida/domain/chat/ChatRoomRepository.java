package com.moida.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(Long productId, Long buyerId, Long sellerId);

    List<ChatRoom> findAllByBuyerIdOrSellerIdOrderByUpdatedAtDesc(Long buyerId, Long sellerId);
}
