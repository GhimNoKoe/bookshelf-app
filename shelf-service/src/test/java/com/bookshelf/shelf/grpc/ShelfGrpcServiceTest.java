package com.bookshelf.shelf.grpc;

import com.bookshelf.grpc.shelf.*;
import com.bookshelf.shelf.dto.ShelfDto;
import com.bookshelf.shelf.model.Shelf;
import com.bookshelf.shelf.model.ShelfType;
import com.bookshelf.shelf.service.ShelfService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelfGrpcServiceTest {

    @Mock ShelfService shelfService;
    @InjectMocks ShelfGrpcService shelfGrpcService;

    // ── getShelf ───────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getShelf_sendsShelfResponseAndCompletes_whenShelfExists() {
        Shelf shelf = buildShelf("s1", "u1", "My Shelf", List.of("book-1"));
        when(shelfService.findById("s1")).thenReturn(shelf);
        StreamObserver<ShelfResponse> responseObserver = mock(StreamObserver.class);

        shelfGrpcService.getShelf(GetShelfRequest.newBuilder().setShelfId("s1").build(), responseObserver);

        ArgumentCaptor<ShelfResponse> captor = ArgumentCaptor.forClass(ShelfResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getShelfId()).isEqualTo("s1");
        assertThat(captor.getValue().getUserId()).isEqualTo("u1");
        assertThat(captor.getValue().getName()).isEqualTo("My Shelf");
        assertThat(captor.getValue().getBookIdsList()).containsExactly("book-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getShelf_sendsError_whenShelfNotFound() {
        when(shelfService.findById("missing")).thenThrow(mock(ResponseStatusException.class));
        StreamObserver<ShelfResponse> responseObserver = mock(StreamObserver.class);

        shelfGrpcService.getShelf(GetShelfRequest.newBuilder().setShelfId("missing").build(), responseObserver);

        verify(responseObserver).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    // ── getShelvesByUser ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getShelvesByUser_sendsShelvesResponseAndCompletes_whenServiceSucceeds() {
        ShelfDto dto = ShelfDto.builder()
                .id("s1").userId("u1").name("Read")
                .shelfType(ShelfType.READ).bookIds(List.of("book-1", "book-2"))
                .build();
        when(shelfService.getShelvesByUser("u1")).thenReturn(List.of(dto));
        StreamObserver<ShelvesResponse> responseObserver = mock(StreamObserver.class);

        shelfGrpcService.getShelvesByUser(
                GetShelvesByUserRequest.newBuilder().setUserId("u1").build(), responseObserver);

        ArgumentCaptor<ShelvesResponse> captor = ArgumentCaptor.forClass(ShelvesResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getShelvesList()).hasSize(1);
        assertThat(captor.getValue().getShelves(0).getShelfId()).isEqualTo("s1");
        assertThat(captor.getValue().getShelves(0).getBookIdsList()).containsExactly("book-1", "book-2");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getShelvesByUser_sendsError_whenServiceThrows() {
        when(shelfService.getShelvesByUser("u1")).thenThrow(new RuntimeException("db error"));
        StreamObserver<ShelvesResponse> responseObserver = mock(StreamObserver.class);

        shelfGrpcService.getShelvesByUser(
                GetShelvesByUserRequest.newBuilder().setUserId("u1").build(), responseObserver);

        verify(responseObserver).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    // ── addBookToShelf ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void addBookToShelf_sendsUpdatedShelfAndCompletes_whenSuccess() {
        Shelf shelf = buildShelf("s1", "u1", "Read", List.of());
        ShelfDto updated = ShelfDto.builder()
                .id("s1").userId("u1").name("Read")
                .shelfType(ShelfType.READ).bookIds(List.of("book-99"))
                .build();
        when(shelfService.findById("s1")).thenReturn(shelf);
        when(shelfService.addBook("s1", "u1", "book-99")).thenReturn(updated);
        StreamObserver<ShelfResponse> responseObserver = mock(StreamObserver.class);

        shelfGrpcService.addBookToShelf(
                AddBookToShelfRequest.newBuilder().setShelfId("s1").setBookId("book-99").build(),
                responseObserver);

        ArgumentCaptor<ShelfResponse> captor = ArgumentCaptor.forClass(ShelfResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getBookIdsList()).containsExactly("book-99");
    }

    @Test
    @SuppressWarnings("unchecked")
    void addBookToShelf_sendsError_whenShelfNotFound() {
        when(shelfService.findById("missing")).thenThrow(mock(ResponseStatusException.class));
        StreamObserver<ShelfResponse> responseObserver = mock(StreamObserver.class);

        shelfGrpcService.addBookToShelf(
                AddBookToShelfRequest.newBuilder().setShelfId("missing").setBookId("book-1").build(),
                responseObserver);

        verify(responseObserver).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Shelf buildShelf(String id, String userId, String name, List<String> bookIds) {
        Shelf shelf = Shelf.builder().id(id).userId(userId).name(name)
                .shelfType(ShelfType.CUSTOM).build();
        bookIds.forEach(shelf::addBook);
        return shelf;
    }
}
