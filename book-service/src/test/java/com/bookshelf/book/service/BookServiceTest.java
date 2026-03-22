package com.bookshelf.book.service;

import com.bookshelf.book.dto.BookDto;
import com.bookshelf.book.dto.CreateBookRequest;
import com.bookshelf.book.dto.UpdateBookRequest;
import com.bookshelf.book.model.Book;
import com.bookshelf.book.model.BookType;
import com.bookshelf.book.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @InjectMocks BookService bookService;

    // ── listAll ────────────────────────────────────────────────────────────────

    @Test
    void listAll_returnsAllBooks() {
        Book b = book("b1", "owner-1", "Clean Code", "Robert Martin", BookType.PAPER, null);
        when(bookRepository.findAll()).thenReturn(List.of(b));

        List<BookDto> result = bookService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Clean Code");
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_returnsBook_whenExists() {
        Book b = book("b1", "owner-1", "Clean Code", "Robert Martin", BookType.PAPER, null);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(b));

        BookDto result = bookService.getById("b1");

        assertThat(result.id()).isEqualTo("b1");
        assertThat(result.title()).isEqualTo("Clean Code");
    }

    @Test
    void getById_throwsNotFound_whenBookMissing() {
        when(bookRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Book not found");
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsBook_forPaperBook() {
        CreateBookRequest request = new CreateBookRequest("Clean Code", "Robert Martin", BookType.PAPER, null, null);
        Book saved = book("b1", "owner-1", "Clean Code", "Robert Martin", BookType.PAPER, null);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        BookDto result = bookService.create("owner-1", request);

        assertThat(result.title()).isEqualTo("Clean Code");
        assertThat(result.bookType()).isEqualTo(BookType.PAPER);
        assertThat(result.eshopUrl()).isNull();
    }

    @Test
    void create_savesAndReturnsBook_forEbookWithEshopUrl() {
        CreateBookRequest request = new CreateBookRequest("Dune", "Frank Herbert", BookType.EBOOK, "https://amazon.com/dune", null);
        Book saved = book("b2", "owner-1", "Dune", "Frank Herbert", BookType.EBOOK, "https://amazon.com/dune");
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        BookDto result = bookService.create("owner-1", request);

        assertThat(result.eshopUrl()).isEqualTo("https://amazon.com/dune");
    }

    @Test
    void create_throwsBadRequest_whenEshopUrlSetOnPaperBook() {
        CreateBookRequest request = new CreateBookRequest("Clean Code", "Robert Martin", BookType.PAPER, "https://amazon.com", null);

        assertThatThrownBy(() -> bookService.create("owner-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("eshopUrl");
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_updatesAndReturnsBook_whenCallerIsOwner() {
        Book existing = book("b1", "owner-1", "Old Title", "Author", BookType.PAPER, null);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateBookRequest request = new UpdateBookRequest("New Title", "Author", BookType.PAPER, null, null);

        BookDto result = bookService.update("b1", "owner-1", request);

        assertThat(result.title()).isEqualTo("New Title");
    }

    @Test
    void update_throwsForbidden_whenCallerIsNotOwner() {
        Book existing = book("b1", "owner-1", "Clean Code", "Author", BookType.PAPER, null);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));
        UpdateBookRequest request = new UpdateBookRequest("New Title", "Author", BookType.PAPER, null, null);

        assertThatThrownBy(() -> bookService.update("b1", "other-user", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void update_throwsNotFound_whenBookMissing() {
        when(bookRepository.findById("missing")).thenReturn(Optional.empty());
        UpdateBookRequest request = new UpdateBookRequest("Title", "Author", BookType.PAPER, null, null);

        assertThatThrownBy(() -> bookService.update("missing", "owner-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    void update_throwsBadRequest_whenEshopUrlSetOnPaperBook() {
        Book existing = book("b1", "owner-1", "Clean Code", "Author", BookType.PAPER, null);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));
        UpdateBookRequest request = new UpdateBookRequest("Clean Code", "Author", BookType.PAPER, "https://amazon.com", null);

        assertThatThrownBy(() -> bookService.update("b1", "owner-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("eshopUrl");
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_deletesBook_whenCallerIsOwner() {
        Book existing = book("b1", "owner-1", "Clean Code", "Author", BookType.PAPER, null);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));

        bookService.delete("b1", "owner-1");

        verify(bookRepository).delete(existing);
    }

    @Test
    void delete_throwsForbidden_whenCallerIsNotOwner() {
        Book existing = book("b1", "owner-1", "Clean Code", "Author", BookType.PAPER, null);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.delete("b1", "other-user"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void delete_throwsNotFound_whenBookMissing() {
        when(bookRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete("missing", "owner-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Book not found");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Book book(String id, String ownerId, String title, String author, BookType type, String eshopUrl) {
        return Book.builder()
                .id(id).ownerId(ownerId).title(title).author(author)
                .bookType(type).eshopUrl(eshopUrl)
                .build();
    }
}
