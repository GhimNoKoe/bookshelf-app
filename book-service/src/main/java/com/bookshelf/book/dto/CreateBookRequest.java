package com.bookshelf.book.dto;

import com.bookshelf.book.model.BookType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotNull BookType bookType,
        String eshopUrl,
        String privateFileKey
) {}
