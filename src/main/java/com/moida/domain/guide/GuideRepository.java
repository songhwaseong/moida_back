package com.moida.domain.guide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<Guide> findByType(Guide.GuideType type);

    boolean existsByType(Guide.GuideType type);
}
