package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title must be 200 characters or less.")
        String title,

        @NotBlank(message = "Content is required.")
        String content,

        @NotBlank(message = "Category is required.")
        String category,

        @NotBlank(message = "Status is required.")
        String status,

        Boolean isPinned
) {
}
