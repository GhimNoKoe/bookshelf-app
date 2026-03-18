package com.bookshelf.shelf.service;

import com.bookshelf.shelf.dto.CreateShelfRequest;
import com.bookshelf.shelf.dto.ShelfDto;
import com.bookshelf.shelf.model.Shelf;
import com.bookshelf.shelf.model.ShelfBook;
import com.bookshelf.shelf.model.ShelfType;
import com.bookshelf.shelf.repository.ShelfBookRepository;
import com.bookshelf.shelf.repository.ShelfRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {

    @Mock ShelfRepository shelfRepository;
    @Mock ShelfBookRepository shelfBookRepository;
    @InjectMocks ShelfService shelfService;

    // ── getShelvesByUser ───────────────────────────────────────────────────────

    @Test
    void getShelvesByUser_returnsExistingShelves_withoutCreatingDefaults() {
        Shelf existing = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findByUserId("user-1")).thenReturn(List.of(existing));

        List<ShelfDto> result = shelfService.getShelvesByUser("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Read");
        verify(shelfRepository, never()).saveAll(any());
    }

    @Test
    void getShelvesByUser_createsDefaultShelves_whenUserHasNone() {
        // first call returns empty (triggers creation), second call returns defaults
        Shelf defaultShelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findByUserId("user-1"))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(defaultShelf));

        List<ShelfDto> result = shelfService.getShelvesByUser("user-1");

        verify(shelfRepository).saveAll(any());
        assertThat(result).isNotEmpty();
    }

    // ── createShelf ────────────────────────────────────────────────────────────

    @Test
    void createShelf_savesAndReturnsCustomShelf() {
        when(shelfRepository.existsByUserIdAndName("user-1", "Favourites")).thenReturn(false);
        Shelf saved = shelf("s2", "user-1", "Favourites", ShelfType.CUSTOM);
        when(shelfRepository.save(any(Shelf.class))).thenReturn(saved);

        ShelfDto result = shelfService.createShelf("user-1", new CreateShelfRequest("Favourites"));

        assertThat(result.name()).isEqualTo("Favourites");
        assertThat(result.shelfType()).isEqualTo(ShelfType.CUSTOM);
    }

    @Test
    void createShelf_throwsConflict_whenNameAlreadyExists() {
        when(shelfRepository.existsByUserIdAndName("user-1", "Favourites")).thenReturn(true);

        assertThatThrownBy(() -> shelfService.createShelf("user-1", new CreateShelfRequest("Favourites")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Shelf name already exists");
    }

    // ── getShelf ───────────────────────────────────────────────────────────────

    @Test
    void getShelf_returnsDto_whenUserOwnsShelf() {
        Shelf shelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));

        ShelfDto result = shelfService.getShelf("s1", "user-1");

        assertThat(result.id()).isEqualTo("s1");
    }

    @Test
    void getShelf_throwsForbidden_whenUserDoesNotOwnShelf() {
        Shelf shelf = shelf("s1", "owner-id", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.getShelf("s1", "different-user"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getShelf_throwsNotFound_whenShelfDoesNotExist() {
        when(shelfRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfService.getShelf("missing", "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Shelf not found");
    }

    // ── addBook ────────────────────────────────────────────────────────────────

    @Test
    void addBook_savesShelfBookAndReturnsUpdatedShelf() {
        Shelf shelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.existsByShelfIdAndBookId("s1", "book-abc")).thenReturn(false);
        when(shelfRepository.save(shelf)).thenReturn(shelf);

        shelfService.addBook("s1", "user-1", "book-abc");

        verify(shelfRepository).save(shelf);
        assertThat(shelf.getBooks())
                .extracting(ShelfBook::getBookId)
                .containsExactly("book-abc");
    }

    @Test
    void addBook_throwsConflict_whenBookAlreadyOnShelf() {
        Shelf shelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.existsByShelfIdAndBookId("s1", "book-abc")).thenReturn(true);

        assertThatThrownBy(() -> shelfService.addBook("s1", "user-1", "book-abc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Book already on shelf");
    }

    // ── removeBook ─────────────────────────────────────────────────────────────

    @Test
    void removeBook_deletesShelfBook() {
        Shelf shelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));
        ShelfBook shelfBook = new ShelfBook();
        when(shelfBookRepository.findByShelfIdAndBookId("s1", "book-abc"))
                .thenReturn(Optional.of(shelfBook));

        shelfService.removeBook("s1", "user-1", "book-abc");

        verify(shelfBookRepository).delete(shelfBook);
    }

    @Test
    void removeBook_throwsNotFound_whenBookNotOnShelf() {
        Shelf shelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.findByShelfIdAndBookId("s1", "missing-book"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfService.removeBook("s1", "user-1", "missing-book"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Book not on shelf");
    }

    // ── deleteShelf ────────────────────────────────────────────────────────────

    @Test
    void deleteShelf_deletesCustomShelf() {
        Shelf shelf = shelf("s1", "user-1", "My Shelf", ShelfType.CUSTOM);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));

        shelfService.deleteShelf("s1", "user-1");

        verify(shelfRepository).delete(shelf);
    }

    @Test
    void deleteShelf_throwsForbidden_whenDeletingDefaultShelf() {
        Shelf shelf = shelf("s1", "user-1", "Read", ShelfType.READ);
        when(shelfRepository.findById("s1")).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.deleteShelf("s1", "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Default shelves cannot be deleted");
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private Shelf shelf(String id, String userId, String name, ShelfType type) {
        return Shelf.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .shelfType(type)
                .build();
    }
}
