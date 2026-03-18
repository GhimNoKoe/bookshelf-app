package com.bookshelf.review.service;

import com.bookshelf.review.dto.CreateReviewRequest;
import com.bookshelf.review.dto.ReviewDto;
import com.bookshelf.review.grpc.ShelfGrpcClient;
import com.bookshelf.review.model.Review;
import com.bookshelf.review.repository.ReviewRepository;
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
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ShelfGrpcClient shelfGrpcClient;
    @InjectMocks ReviewService reviewService;

    // ── getByBook ──────────────────────────────────────────────────────────────

    @Test
    void getByBook_returnsAllReviewsForBook() {
        Review review = review("r1", "user-1", "book-abc", 4, false);
        when(reviewRepository.findByBookId("book-abc")).thenReturn(List.of(review));

        List<ReviewDto> result = reviewService.getByBook("book-abc");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookId()).isEqualTo("book-abc");
    }

    // ── getByUser ──────────────────────────────────────────────────────────────

    @Test
    void getByUser_returnsAllReviewsByUser() {
        Review review = review("r1", "user-1", "book-abc", 4, false);
        when(reviewRepository.findByUserId("user-1")).thenReturn(List.of(review));

        List<ReviewDto> result = reviewService.getByUser("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo("user-1");
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_savesReviewWithVerifiedReaderTrue_whenBookIsOnShelf() {
        when(reviewRepository.existsByUserIdAndBookId("user-1", "book-abc")).thenReturn(false);
        when(shelfGrpcClient.isBookOnUserShelf("user-1", "book-abc")).thenReturn(true);
        Review saved = review("r1", "user-1", "book-abc", 5, true);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewDto result = reviewService.create("user-1", new CreateReviewRequest("book-abc", 5, "Great read"));

        assertThat(result.verifiedReader()).isTrue();
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void create_savesReviewWithVerifiedReaderFalse_whenBookIsNotOnShelf() {
        when(reviewRepository.existsByUserIdAndBookId("user-1", "book-abc")).thenReturn(false);
        when(shelfGrpcClient.isBookOnUserShelf("user-1", "book-abc")).thenReturn(false);
        Review saved = review("r1", "user-1", "book-abc", 3, false);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewDto result = reviewService.create("user-1", new CreateReviewRequest("book-abc", 3, "Okay"));

        assertThat(result.verifiedReader()).isFalse();
    }

    @Test
    void create_throwsConflict_whenUserAlreadyReviewedBook() {
        when(reviewRepository.existsByUserIdAndBookId("user-1", "book-abc")).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create("user-1", new CreateReviewRequest("book-abc", 4, "Again")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already reviewed");
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_removesReview_whenUserOwnsIt() {
        Review review = review("r1", "user-1", "book-abc", 4, false);
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        reviewService.delete("r1", "user-1");

        verify(reviewRepository).delete(review);
    }

    @Test
    void delete_throwsForbidden_whenUserDoesNotOwnReview() {
        Review review = review("r1", "owner-id", "book-abc", 4, false);
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete("r1", "different-user"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void delete_throwsNotFound_whenReviewDoesNotExist() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.delete("missing", "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Review not found");
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private Review review(String id, String userId, String bookId, int rating, boolean verifiedReader) {
        return Review.builder()
                .id(id)
                .userId(userId)
                .bookId(bookId)
                .rating(rating)
                .verifiedReader(verifiedReader)
                .build();
    }
}
