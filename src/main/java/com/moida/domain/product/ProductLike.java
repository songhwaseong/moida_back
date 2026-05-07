package com.moida.domain.product;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_like",
                columnNames = {"product_id", "member_id"}),
        indexes = {
                @Index(name = "idx_like_member", columnList = "member_id"),
                @Index(name = "idx_like_product", columnList = "product_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    private ProductLike(Product product, Member member) {
        this.product = product;
        this.member = member;
    }
}
