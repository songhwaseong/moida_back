package com.moida.domain.terms;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "terms_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_document_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, unique = true, length = 30)
    private TermsType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "intro", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Builder
    private TermsDocument(TermsType type, String title, String content, LocalDate effectiveDate, Boolean active) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.effectiveDate = effectiveDate == null ? LocalDate.of(2026, 6, 1) : effectiveDate;
        this.active = active == null || active;
    }

    public void update(String title, String content, LocalDate effectiveDate, Boolean active) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (effectiveDate != null) this.effectiveDate = effectiveDate;
        if (active != null) this.active = active;
    }

    public enum TermsType {
        TERMS,
        PRIVACY
    }
}