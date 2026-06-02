package com.moida.domain.guide;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Table(name = "guides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "guide_type", nullable = false, unique = true, length = 30)
    private GuideType type;

    @Column(name = "tab_label", nullable = false, length = 30)
    private String tabLabel;

    @Column(name = "banner_label", nullable = false, length = 50)
    private String bannerLabel;

    @Column(name = "banner_title", nullable = false, length = 100)
    private String bannerTitle;

    @Column(name = "banner_description", nullable = false, length = 255)
    private String bannerDescription;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guide_steps", joinColumns = @JoinColumn(name = "guide_id"))
    @OrderColumn(name = "step_order")
    private List<GuideStep> steps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guide_tips", joinColumns = @JoinColumn(name = "guide_id"))
    @OrderColumn(name = "tip_order")
    private List<GuideTip> tips = new ArrayList<>();

    @Builder
    private Guide(GuideType type,
                  String tabLabel,
                  String bannerLabel,
                  String bannerTitle,
                  String bannerDescription,
                  Integer displayOrder,
                  List<GuideStep> steps,
                  List<GuideTip> tips) {
        this.type = type;
        this.tabLabel = tabLabel;
        this.bannerLabel = bannerLabel;
        this.bannerTitle = bannerTitle;
        this.bannerDescription = bannerDescription;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
        this.tips = tips == null ? new ArrayList<>() : new ArrayList<>(tips);
    }

    public void update(String tabLabel,
                       String bannerLabel,
                       String bannerTitle,
                       String bannerDescription,
                       Integer displayOrder,
                       List<GuideStep> steps,
                       List<GuideTip> tips) {
        if (tabLabel != null) this.tabLabel = tabLabel;
        if (bannerLabel != null) this.bannerLabel = bannerLabel;
        if (bannerTitle != null) this.bannerTitle = bannerTitle;
        if (bannerDescription != null) this.bannerDescription = bannerDescription;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (steps != null) {
            this.steps.clear();
            this.steps.addAll(steps);
        }
        if (tips != null) {
            this.tips.clear();
            this.tips.addAll(tips);
        }
    }

    public List<GuideStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public List<GuideTip> getTips() {
        return Collections.unmodifiableList(tips);
    }

    public enum GuideType {
        BUY,
        SELL,
        AUCTION
    }
}
