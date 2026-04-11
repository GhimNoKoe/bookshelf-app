package com.bookshelf.ai.client.dto;

import lombok.Builder;

@Builder
public record BookResponse(String id, String title) {}
