package com.bookshelf.shelf.service;

import com.bookshelf.shelf.dto.CreateShelfRequest;
import com.bookshelf.shelf.dto.ShelfDto;
import com.bookshelf.shelf.model.Shelf;
import com.bookshelf.shelf.model.ShelfBook;
import com.bookshelf.shelf.model.ShelfType;
import com.bookshelf.shelf.repository.ShelfBookRepository;
import com.bookshelf.shelf.repository.ShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelfService {

    private final ShelfRepository shelfRepository;
    private final ShelfBookRepository shelfBookRepository;

    @Transactional
    public List<ShelfDto> getShelvesByUser(String userId) {
        if (shelfRepository.findByUserId(userId).isEmpty()) {
            createDefaultShelves(userId);
        }
        return shelfRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private void createDefaultShelves(String userId) {
        List<Shelf> defaults = ShelfType.getDefaults().stream()
                .map(type -> Shelf.builder()
                        .userId(userId)
                        .name(type.getDisplayName())
                        .shelfType(type)
                        .build())
                .toList();
        shelfRepository.saveAll(defaults);
    }

    public ShelfDto getShelf(String shelfId, String userId) {
        Shelf shelf = findAndAuthorize(shelfId, userId);
        return toDto(shelf);
    }

    @Transactional
    public ShelfDto createShelf(String userId, CreateShelfRequest request) {
        if (shelfRepository.existsByUserIdAndName(userId, request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shelf name already exists");
        }
        Shelf shelf = Shelf.builder()
                .userId(userId)
                .name(request.name())
                .shelfType(ShelfType.CUSTOM)
                .build();
        return toDto(shelfRepository.save(shelf));
    }

    @Transactional
    public ShelfDto addBook(String shelfId, String userId, String bookId) {
        Shelf shelf = findAndAuthorize(shelfId, userId);
        if (shelfBookRepository.existsByShelfIdAndBookId(shelfId, bookId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Book already on shelf");
        }
        ShelfBook entry = ShelfBook.builder().shelf(shelf).bookId(bookId).build();
        shelfBookRepository.save(entry);
        return toDto(shelfRepository.findById(shelfId).orElseThrow());
    }

    @Transactional
    public void removeBook(String shelfId, String userId, String bookId) {
        findAndAuthorize(shelfId, userId);
        ShelfBook entry = shelfBookRepository.findByShelfIdAndBookId(shelfId, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not on shelf"));
        shelfBookRepository.delete(entry);
    }

    @Transactional
    public void deleteShelf(String shelfId, String userId) {
        Shelf shelf = findAndAuthorize(shelfId, userId);
        if (shelf.getShelfType().isDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Default shelves cannot be deleted");
        }
        shelfRepository.delete(shelf);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Shelf findAndAuthorize(String shelfId, String userId) {
        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shelf not found"));
        if (!shelf.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return shelf;
    }

    public ShelfDto toDto(Shelf shelf) {
        List<String> bookIds = shelf.getBooks().stream()
                .map(ShelfBook::getBookId)
                .toList();
        return ShelfDto.builder()
                .id(shelf.getId())
                .userId(shelf.getUserId())
                .name(shelf.getName())
                .shelfType(shelf.getShelfType())
                .bookIds(bookIds)
                .createdAt(shelf.getCreatedAt())
                .build();
    }

    public Shelf findById(String shelfId) {
        return shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shelf not found"));
    }
}
