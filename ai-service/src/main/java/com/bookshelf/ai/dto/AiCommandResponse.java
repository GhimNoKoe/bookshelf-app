package com.bookshelf.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AiCommandResponse(
        String result,
        List<String> actionsPerformed
) {}
