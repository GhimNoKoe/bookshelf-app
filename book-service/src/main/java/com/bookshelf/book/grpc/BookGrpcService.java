package com.bookshelf.book.grpc;

import com.bookshelf.book.dto.BookDto;
import com.bookshelf.book.service.BookService;
import com.bookshelf.grpc.book.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {

    private final BookService bookService;

    @Override
    public void getBook(GetBookRequest request, StreamObserver<BookResponse> responseObserver) {
        try {
            BookDto dto = bookService.getById(request.getBookId());
            responseObserver.onNext(toProto(dto));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getBook failed for id={}", request.getBookId(), e);
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listBooks(ListBooksRequest request, StreamObserver<BooksResponse> responseObserver) {
        try {
            BooksResponse.Builder builder = BooksResponse.newBuilder();
            bookService.listAll().forEach(dto -> builder.addBooks(toProto(dto)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private BookResponse toProto(BookDto dto) {
        BookResponse.Builder builder = BookResponse.newBuilder()
                .setBookId(dto.id())
                .setOwnerId(dto.ownerId())
                .setTitle(dto.title())
                .setAuthor(dto.author())
                .setBookType(dto.bookType().name());
        if (dto.eshopUrl() != null) builder.setEshopUrl(dto.eshopUrl());
        if (dto.privateFileKey() != null) builder.setPrivateFileKey(dto.privateFileKey());
        return builder.build();
    }
}
