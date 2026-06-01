package com.moida.domain.terms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermsDocumentRepository extends JpaRepository<TermsDocument, Long> {

    Optional<TermsDocument> findByTypeAndActiveTrue(TermsDocument.TermsType type);

    boolean existsByType(TermsDocument.TermsType type);
}
