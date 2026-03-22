package com.bookshelf.book.service;

import com.bookshelf.book.dto.BookDto;
import com.bookshelf.book.dto.CreateBookRequest;
import com.bookshelf.book.dto.UpdateBookRequest;
import com.bookshelf.book.model.Book;
import com.bookshelf.book.model.BookType;
import com.bookshelf.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<BookDto> listAll() {
        return bookRepository.findAll().stream().map(this::toDto).toList();
    }

    public BookDto getById(String id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public BookDto create(String ownerId, CreateBookRequest request) {
        validateEshopUrl(request.bookType(), request.eshopUrl());
        Book book = Book.builder()
                .ownerId(ownerId)
                .title(request.title())
                .author(request.author())
                .bookType(request.bookType())
                .eshopUrl(request.eshopUrl())
                .privateFileKey(request.privateFileKey())
                .build();
        return toDto(bookRepository.save(book));
    }

    @Transactional
    public BookDto update(String bookId, String userId, UpdateBookRequest request) {
        Book book = findOrThrow(bookId);
        authorizeOwner(book, userId);
        validateEshopUrl(request.bookType(), request.eshopUrl());
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setBookType(request.bookType());
        book.setEshopUrl(request.eshopUrl());
        book.setPrivateFileKey(request.privateFileKey());
        return toDto(bookRepository.save(book));
    }

    @Transactional
    public void delete(String bookId, String userId) {
        Book book = findOrThrow(bookId);
        authorizeOwner(book, userId);
        bookRepository.delete(book);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Book findOrThrow(String bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private void authorizeOwner(Book book, String userId) {
        if (!book.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void validateEshopUrl(BookType bookType, String eshopUrl) {
        if (eshopUrl != null && bookType != BookType.EBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eshopUrl is only allowed for EBOOK type books");
        }
    }

    public BookDto toDto(Book book) {
        return new BookDto(
                book.getId(),
                book.getOwnerId(),
                book.getTitle(),
                book.getAuthor(),
                book.getBookType(),
                book.getEshopUrl(),
                book.getPrivateFileKey(),
                book.getCreatedAt()
        );
    }
}
