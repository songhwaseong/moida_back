package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ProductChatMessageRequest {

    @NotBlank
    @Size(max = 500)
    private String content;
}
