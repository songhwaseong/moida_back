package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryRequest {

    @NotBlank(message = "Question is required.")
    @Size(max = 2000, message = "Question must be 2000 characters or less.")
    private String question;

    private Boolean isSecret;
}
