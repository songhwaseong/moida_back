package com.moida.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();
    Optional<Category> findByNameAndIsActiveTrue(String name);

    // 관리자 화면용 — 활성/비활성 모두 포함해 displayOrder 순으로 반환한다.
    List<Category> findAllByOrderByDisplayOrderAscIdAsc();
}
