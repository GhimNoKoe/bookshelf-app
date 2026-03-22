package com.bookshelf.review.grpc;

import com.bookshelf.grpc.review.*;
import com.bookshelf.review.dto.CreateReviewRequest;
import com.bookshelf.review.dto.ReviewDto;
import com.bookshelf.review.service.ReviewService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewGrpcServiceTest {

    @Mock ReviewService reviewService;
    @InjectMocks ReviewGrpcService reviewGrpcService;

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 12, 0);

    // ── getReviewsByBook ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getReviewsByBook_sendsReviewsResponseAndCompletes() {
        ReviewDto dto = new ReviewDto("r1", "u1", "book-1", 5, "Great!", true, NOW);
        when(reviewService.getByBook("book-1")).thenReturn(List.of(dto));
        StreamObserver<ReviewsResponse> responseObserver = mock(StreamObserver.class);

        reviewGrpcService.getReviewsByBook(
                GetReviewsByBookRequest.newBuilder().setBookId("book-1").build(), responseObserver);

        ArgumentCaptor<ReviewsResponse> captor = ArgumentCaptor.forClass(ReviewsResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getReviewsList()).hasSize(1);
        assertThat(captor.getValue().getReviews(0).getReviewId()).isEqualTo("r1");
        assertThat(captor.getValue().getReviews(0).getRating()).isEqualTo(5);
        assertThat(captor.getValue().getReviews(0).getVerifiedReader()).isTrue();
    }

    // ── getReviewsByUser ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getReviewsByUser_sendsReviewsResponseAndCompletes() {
        ReviewDto dto = new ReviewDto("r2", "u1", "book-2", 3, "Ok", false, NOW);
        when(reviewService.getByUser("u1")).thenReturn(List.of(dto));
        StreamObserver<ReviewsResponse> responseObserver = mock(StreamObserver.class);

        reviewGrpcService.getReviewsByUser(
                GetReviewsByUserRequest.newBuilder().setUserId("u1").build(), responseObserver);

        ArgumentCaptor<ReviewsResponse> captor = ArgumentCaptor.forClass(ReviewsResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getReviewsList()).hasSize(1);
        assertThat(captor.getValue().getReviews(0).getReviewId()).isEqualTo("r2");
        assertThat(captor.getValue().getReviews(0).getBookId()).isEqualTo("book-2");
    }

    // ── createReview ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void createReview_sendsReviewResponseAndCompletes() {
        ReviewDto dto = new ReviewDto("r3", "u1", "book-3", 4, "Good read", false, NOW);
        when(reviewService.create(eq("u1"), any(CreateReviewRequest.class))).thenReturn(dto);
        StreamObserver<ReviewResponse> responseObserver = mock(StreamObserver.class);

        reviewGrpcService.createReview(
                com.bookshelf.grpc.review.CreateReviewRequest.newBuilder()
                        .setUserId("u1").setBookId("book-3").setRating(4).setContent("Good read")
                        .build(),
                responseObserver);

        ArgumentCaptor<ReviewResponse> captor = ArgumentCaptor.forClass(ReviewResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getReviewId()).isEqualTo("r3");
        assertThat(captor.getValue().getRating()).isEqualTo(4);
        assertThat(captor.getValue().getContent()).isEqualTo("Good read");
    }
}
