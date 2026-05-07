package com.moida.domain.product;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_images",
        indexes = @Index(name = "idx_pi_product", columnList = "product_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain;

    @Builder
    private ProductImage(String url, Integer displayOrder, Boolean isMain) {
        this.url = url;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.isMain = isMain != null && isMain;
    }

    void assignProduct(Product product) {
        this.product = product;
    }

    public void markAsMain() { this.isMain = true; }
    public void unmarkAsMain() { this.isMain = false; }
}
