package com.moida.domain.category;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_name", columnNames = "name"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;          // 디지털/가전, 패션/의류 등

    @Column(length = 10)
    private String emoji;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private Category(String name, String emoji, Integer displayOrder) {
        this.name = name;
        this.emoji = emoji;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.isActive = true;
    }

    public void update(String name, String emoji, Integer displayOrder) {
        if (name != null) this.name = name;
        if (emoji != null) this.emoji = emoji;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    public void deactivate() { this.isActive = false; }
    public void activate() { this.isActive = true; }
}
