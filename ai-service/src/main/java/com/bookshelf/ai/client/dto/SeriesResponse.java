package com.bookshelf.ai.client.dto;

import lombok.Builder;

@Builder
public record SeriesResponse(String id, String name) {}
