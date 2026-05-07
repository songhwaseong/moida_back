package com.moida.domain.inquiry;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "inquiries",
        indexes = {
                @Index(name = "idx_inquiry_product", columnList = "product_id"),
                @Index(name = "idx_inquiry_user", columnList = "user_id"),
                @Index(name = "idx_inquiry_seller", columnList = "seller_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member user;                 // 문의 작성자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;               // 답변할 판매자

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "is_secret", nullable = false)
    private Boolean isSecret;

    @Builder
    private Inquiry(Product product, Member user, Member seller, String question, Boolean isSecret) {
        this.product = product;
        this.user = user;
        this.seller = seller;
        this.question = question;
        this.isSecret = isSecret != null && isSecret;
    }

    public void answer(String answer) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
    }

    public void removeAnswer() {
        this.answer = null;
        this.answeredAt = null;
    }
}
