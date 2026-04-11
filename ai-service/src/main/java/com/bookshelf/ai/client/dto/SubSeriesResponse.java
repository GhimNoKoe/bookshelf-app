package com.bookshelf.ai.client.dto;

import lombok.Builder;

@Builder
public record SubSeriesResponse(String id, String name, String seriesId) {}
