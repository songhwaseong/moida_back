package com.moida.common.response;

import com.moida.domain.faq.Faq;

public record FaqResponse(
        Long id,
        String category,
        String question,
        String answer,
        Integer order,
        Boolean visible
) {
    public static FaqResponse from(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getCategory(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getDisplayOrder(),
                faq.getVisible()
        );
    }
}
