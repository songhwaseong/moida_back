package com.moida.domain.banner;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "banners",
        indexes = @Index(name = "idx_banner_active", columnList = "is_active"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Builder
    private Banner(String title, String imageUrl, String linkUrl, Integer displayOrder,
                   LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.isActive = true;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void update(String title, String imageUrl, String linkUrl,
                       Integer displayOrder, LocalDateTime startAt, LocalDateTime endAt) {
        if (title != null) this.title = title;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (linkUrl != null) this.linkUrl = linkUrl;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (startAt != null) this.startAt = startAt;
        if (endAt != null) this.endAt = endAt;
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }
}
