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
public class GuideTip {

    @Column(name = "icon", nullable = false, length = 30)
    private String icon;

    @Column(name = "text", nullable = false, length = 255)
    private String text;

    @Column(name = "warning", nullable = false)
    private Boolean warning;

    @Builder
    private GuideTip(String icon, String text, Boolean warning) {
        this.icon = icon;
        this.text = text;
        this.warning = warning != null && warning;
    }
}
