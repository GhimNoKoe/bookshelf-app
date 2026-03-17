package com.bookshelf.shelf.dto;

import com.bookshelf.shelf.model.ShelfType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ShelfDto(
        String id,
        String userId,
        String name,
        ShelfType shelfType,
        List<String> bookIds,
        LocalDateTime createdAt
) {}
