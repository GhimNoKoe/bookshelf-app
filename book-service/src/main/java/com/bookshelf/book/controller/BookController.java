package com.bookshelf.book.controller;

import com.bookshelf.book.dto.BookDto;
import com.bookshelf.book.dto.CreateBookRequest;
import com.bookshelf.book.dto.UpdateBookRequest;
import com.bookshelf.book.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookDto> listBooks() {
        return bookService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto createBook(@AuthenticationPrincipal String userId,
                              @Valid @RequestBody CreateBookRequest request) {
        return bookService.create(userId, request);
    }

    @GetMapping("/{id}")
    public BookDto getBook(@PathVariable String id) {
        return bookService.getById(id);
    }

    @PutMapping("/{id}")
    public BookDto updateBook(@PathVariable String id,
                              @AuthenticationPrincipal String userId,
                              @Valid @RequestBody UpdateBookRequest request) {
        return bookService.update(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String id,
                           @AuthenticationPrincipal String userId) {
        bookService.delete(id, userId);
    }
}
