package com.bookshelf.review.controller;

import com.bookshelf.grpc.user.ValidateTokenResponse;
import com.bookshelf.review.dto.CreateReviewRequest;
import com.bookshelf.review.grpc.ShelfGrpcClient;
import com.bookshelf.review.grpc.UserGrpcClient;
import com.bookshelf.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ReviewService reviewService;

    @MockBean UserGrpcClient userGrpcClient;  // replaces the external gRPC call in JwtAuthFilter
    @MockBean ShelfGrpcClient shelfGrpcClient; // replaces the external gRPC call in ReviewService

    private static final String TOKEN = "test-token";
    private static final String USER_ID = "user-123";

    @BeforeEach
    void stubTokenValidation() {
        ValidateTokenResponse validResponse = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId(USER_ID)
                .setUsername("alice")
                .build();
        when(userGrpcClient.validateToken(TOKEN)).thenReturn(validResponse);
        when(shelfGrpcClient.isBookOnUserShelf(any(), any())).thenReturn(false);
    }

    // ── GET /api/reviews/book/{bookId} ─────────────────────────────────────────

    @Test
    void getByBook_returns200WithReviews() throws Exception {
        reviewService.create(USER_ID, new CreateReviewRequest("book-abc", 5, "Great"));

        mockMvc.perform(get("/api/reviews/book/book-abc")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookId").value("book-abc"))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    // ── GET /api/reviews/user/{userId} ─────────────────────────────────────────

    @Test
    void getByUser_returns200WithReviews() throws Exception {
        reviewService.create(USER_ID, new CreateReviewRequest("book-abc", 4, "Good"));

        mockMvc.perform(get("/api/reviews/user/" + USER_ID)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));
    }

    // ── POST /api/reviews ──────────────────────────────────────────────────────

    @Test
    void create_returns201WithVerifiedReaderFalse_whenBookNotOnShelf() throws Exception {
        when(shelfGrpcClient.isBookOnUserShelf(USER_ID, "book-abc")).thenReturn(false);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateReviewRequest("book-abc", 4, "Decent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verifiedReader").value(false))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void create_returns201WithVerifiedReaderTrue_whenBookIsOnShelf() throws Exception {
        when(shelfGrpcClient.isBookOnUserShelf(USER_ID, "book-abc")).thenReturn(true);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateReviewRequest("book-abc", 5, "Loved it"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verifiedReader").value(true));
    }

    @Test
    void create_returns409_whenUserAlreadyReviewedBook() throws Exception {
        reviewService.create(USER_ID, new CreateReviewRequest("book-abc", 4, "First"));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateReviewRequest("book-abc", 3, "Second"))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returns400_whenRatingIsOutOfRange() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateReviewRequest("book-abc", 6, "Too high"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateReviewRequest("book-abc", 4, "Test"))))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/reviews/{id} ───────────────────────────────────────────────

    @Test
    void delete_returns204() throws Exception {
        var created = reviewService.create(USER_ID, new CreateReviewRequest("book-abc", 5, "Great"));

        mockMvc.perform(delete("/api/reviews/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns403_whenUserDoesNotOwnReview() throws Exception {
        var created = reviewService.create("other-user", new CreateReviewRequest("book-abc", 5, "Mine"));

        mockMvc.perform(delete("/api/reviews/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
    }
}
