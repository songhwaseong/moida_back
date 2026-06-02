package com.moida.domain.faq;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "faqs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "display_order", nullable = false, unique = true)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean visible;

    @Builder
    private Faq(String category, String question, String answer, Integer displayOrder, Boolean visible) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.displayOrder = displayOrder;
        this.visible = visible != null && visible;
    }

    public void update(String category, String question, String answer, Boolean visible) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.visible = visible != null && visible;
    }

    public void update(String category, String question, String answer, Integer displayOrder, Boolean visible) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.displayOrder = displayOrder;
        this.visible = visible != null && visible;
    }
}
