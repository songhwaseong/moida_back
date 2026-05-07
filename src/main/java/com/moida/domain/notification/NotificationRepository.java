package com.moida.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    long countByMemberIdAndIsReadFalse(Long memberId);
}
