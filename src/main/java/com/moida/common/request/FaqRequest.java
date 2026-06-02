package com.moida.common.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FaqRequest(
        @NotBlank(message = "Category is required.")
        @Size(max = 50, message = "Category must be 50 characters or less.")
        String category,

        @NotBlank(message = "Question is required.")
        @Size(max = 255, message = "Question must be 255 characters or less.")
        String question,

        @NotBlank(message = "Answer is required.")
        String answer,

        @JsonAlias("displayOrder")
        @NotNull(message = "Order is required.")
        @Positive(message = "Order must be positive.")
        Integer order,

        Boolean visible
) {
}
