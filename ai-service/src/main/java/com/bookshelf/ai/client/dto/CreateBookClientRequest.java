package com.bookshelf.ai.client.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CreateBookClientRequest(
        String title,
        String originalTitle,
        List<String> authorIds,
        String bookType,
        String eshopUrl,
        String seriesId,
        String subSeriesId,
        Integer seriesOrder,
        Integer subSeriesOrder
) {}
