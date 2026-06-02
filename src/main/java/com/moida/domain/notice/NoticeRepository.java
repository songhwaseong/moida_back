package com.moida.domain.notice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByIsPinnedTrueOrderByCreatedAtDesc();

    List<Notice> findAllByStatusOrderByIsPinnedDescCreatedAtDesc(Notice.NoticeStatus status);

    List<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc();

    Page<Notice> findAll(Pageable pageable);
}
