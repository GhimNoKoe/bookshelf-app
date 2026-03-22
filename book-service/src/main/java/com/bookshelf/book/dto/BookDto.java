package com.bookshelf.book.dto;

import com.bookshelf.book.model.BookType;

import java.time.LocalDateTime;

public record BookDto(
        String id,
        String ownerId,
        String title,
        String author,
        BookType bookType,
        String eshopUrl,
        String privateFileKey,
        LocalDateTime createdAt
) {}
