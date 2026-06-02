package com.moida.common.response;

import com.moida.domain.guide.Guide;
import com.moida.domain.guide.GuideStep;
import com.moida.domain.guide.GuideTip;

import java.util.List;

public record GuideResponse(
        String type,
        String tabLabel,
        String bannerLabel,
        String bannerTitle,
        String bannerDescription,
        List<GuideStepResponse> steps,
        List<GuideTipResponse> tips
) {
    public static GuideResponse from(Guide guide) {
        return new GuideResponse(
                guide.getType().name(),
                guide.getTabLabel(),
                guide.getBannerLabel(),
                guide.getBannerTitle(),
                guide.getBannerDescription(),
                guide.getSteps().stream().map(GuideStepResponse::from).toList(),
                guide.getTips().stream().map(GuideTipResponse::from).toList()
        );
    }

    public record GuideStepResponse(String icon, String title, String description) {
        public static GuideStepResponse from(GuideStep step) {
            return new GuideStepResponse(step.getIcon(), step.getTitle(), step.getDescription());
        }
    }

    public record GuideTipResponse(String icon, String text, Boolean warning) {
        public static GuideTipResponse from(GuideTip tip) {
            return new GuideTipResponse(tip.getIcon(), tip.getText(), tip.getWarning());
        }
    }
}
