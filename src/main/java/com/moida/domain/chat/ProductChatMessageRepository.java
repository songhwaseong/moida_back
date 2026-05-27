package com.moida.domain.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductChatMessageRepository extends JpaRepository<ProductChatMessage, Long> {

    @Query("""
            select m
            from ProductChatMessage m
            join fetch m.sender
            join fetch m.room r
            join fetch r.product p
            join fetch p.seller
            where r.id = :roomId
            order by m.createdAt desc
            """)
    List<ProductChatMessage> findRecentByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    @Query("""
            select m
            from ProductChatMessage m
            where m.room.id = :roomId
              and m.sender.id = :senderId
            order by m.createdAt desc
            """)
    List<ProductChatMessage> findRecentByRoomIdAndSenderId(
            @Param("roomId") Long roomId,
            @Param("senderId") Long senderId,
            Pageable pageable
    );

    long countByRoomId(Long roomId);
}
