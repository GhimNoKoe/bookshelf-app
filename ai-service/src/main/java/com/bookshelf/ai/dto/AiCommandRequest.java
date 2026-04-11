package com.bookshelf.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AiCommandRequest(
        @NotBlank String prompt
) {}
