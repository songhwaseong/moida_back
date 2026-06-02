package com.moida.domain.guide;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideStep {

    @Column(name = "icon", nullable = false, length = 30)
    private String icon;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Builder
    private GuideStep(String icon, String title, String description) {
        this.icon = icon;
        this.title = title;
        this.description = description;
    }
}
