package com.bookshelf.ai.client.dto;

import lombok.Builder;

@Builder
public record AuthorResponse(String id, String name) {}
