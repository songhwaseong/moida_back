package com.moida.domain.review;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review",
                columnNames = {"product_id", "reviewer_id"}),
        indexes = {
                @Index(name = "idx_review_target", columnList = "target_member_id"),
                @Index(name = "idx_review_reviewer", columnList = "reviewer_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Member reviewer;             // 후기 작성자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_member_id", nullable = false)
    private Member targetMember;         // 후기 대상자 (판매자/구매자)

    @Column(nullable = false)
    private Integer rating;              // 1~5

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "manner_temp_change", nullable = false)
    private Double mannerTempChange;     // 매너 온도 변화량

    @Builder
    private Review(Product product, Member reviewer, Member targetMember,
                   Integer rating, String content, Double mannerTempChange) {
        this.product = product;
        this.reviewer = reviewer;
        this.targetMember = targetMember;
        this.rating = rating;
        this.content = content;
        this.mannerTempChange = mannerTempChange == null ? 0.0 : mannerTempChange;
    }
}
