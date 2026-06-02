package com.moida.domain.faq;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    Optional<Faq> findFirstByDisplayOrder(Integer displayOrder);

    List<Faq> findAllByOrderByDisplayOrderAscIdAsc();

    List<Faq> findAllByVisibleTrueOrderByDisplayOrderAscIdAsc();
}
