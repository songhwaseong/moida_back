package com.moida.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductChatRoomRepository extends JpaRepository<ProductChatRoom, Long> {

    Optional<ProductChatRoom> findByProductId(Long productId);
}
