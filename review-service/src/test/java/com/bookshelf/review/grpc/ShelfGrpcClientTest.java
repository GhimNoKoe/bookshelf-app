package com.bookshelf.review.grpc;

import com.bookshelf.grpc.shelf.GetShelvesByUserRequest;
import com.bookshelf.grpc.shelf.ShelfResponse;
import com.bookshelf.grpc.shelf.ShelfServiceGrpc;
import com.bookshelf.grpc.shelf.ShelvesResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShelfGrpcClientTest {

    private ShelfGrpcClient shelfGrpcClient;
    private ShelfServiceGrpc.ShelfServiceBlockingStub shelfStub;

    @BeforeEach
    void setUp() {
        shelfGrpcClient = new ShelfGrpcClient();
        shelfStub = mock(ShelfServiceGrpc.ShelfServiceBlockingStub.class);
        ReflectionTestUtils.setField(shelfGrpcClient, "shelfStub", shelfStub);
    }

    @Test
    void isBookOnUserShelf_returnsTrue_whenBookIsOnOneOfTheUsersShelves() {
        ShelvesResponse response = ShelvesResponse.newBuilder()
                .addShelves(ShelfResponse.newBuilder()
                        .setShelfId("s1").setUserId("user-1")
                        .addBookIds("book-abc")
                        .build())
                .addShelves(ShelfResponse.newBuilder()
                        .setShelfId("s2").setUserId("user-1")
                        .addBookIds("book-xyz")
                        .build())
                .build();
        when(shelfStub.getShelvesByUser(any(GetShelvesByUserRequest.class))).thenReturn(response);

        assertThat(shelfGrpcClient.isBookOnUserShelf("user-1", "book-abc")).isTrue();
    }

    @Test
    void isBookOnUserShelf_returnsFalse_whenBookIsNotOnAnyShelf() {
        ShelvesResponse response = ShelvesResponse.newBuilder()
                .addShelves(ShelfResponse.newBuilder()
                        .setShelfId("s1").setUserId("user-1")
                        .addBookIds("book-xyz")
                        .build())
                .build();
        when(shelfStub.getShelvesByUser(any(GetShelvesByUserRequest.class))).thenReturn(response);

        assertThat(shelfGrpcClient.isBookOnUserShelf("user-1", "book-abc")).isFalse();
    }

    @Test
    void isBookOnUserShelf_returnsFalse_whenShelfServiceIsUnavailable() {
        when(shelfStub.getShelvesByUser(any(GetShelvesByUserRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        assertThat(shelfGrpcClient.isBookOnUserShelf("user-1", "book-abc")).isFalse();
    }
}
