package com.moida.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByIsActiveTrueOrderByDisplayOrderAsc();
    Optional<Category> findByNameAndIsActiveTrue(String name);
}
