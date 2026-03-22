package com.bookshelf.book.controller;

import com.bookshelf.book.dto.CreateBookRequest;
import com.bookshelf.book.dto.UpdateBookRequest;
import com.bookshelf.book.grpc.UserGrpcClient;
import com.bookshelf.book.model.BookType;
import com.bookshelf.book.service.BookService;
import com.bookshelf.grpc.user.ValidateTokenResponse;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BookService bookService;

    @MockBean UserGrpcClient userGrpcClient;

    private static final String TOKEN = "test-token";
    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-999";

    @BeforeEach
    void stubTokenValidation() {
        ValidateTokenResponse validResponse = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId(USER_ID)
                .setUsername("alice")
                .build();
        when(userGrpcClient.validateToken(TOKEN)).thenReturn(validResponse);
    }

    // ── GET /api/books ─────────────────────────────────────────────────────────

    @Test
    void listBooks_returns200WithBooks() throws Exception {
        bookService.create(USER_ID, new CreateBookRequest("Clean Code", "Robert Martin", BookType.PAPER, null, null));

        mockMvc.perform(get("/api/books").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    void listBooks_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/books ────────────────────────────────────────────────────────

    @Test
    void createBook_returns201_forPaperBook() throws Exception {
        CreateBookRequest request = new CreateBookRequest("Clean Code", "Robert Martin", BookType.PAPER, null, null);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.bookType").value("PAPER"))
                .andExpect(jsonPath("$.ownerId").value(USER_ID));
    }

    @Test
    void createBook_returns201_forEbookWithEshopUrl() throws Exception {
        CreateBookRequest request = new CreateBookRequest("Dune", "Frank Herbert", BookType.EBOOK, "https://amazon.com/dune", null);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookType").value("EBOOK"))
                .andExpect(jsonPath("$.eshopUrl").value("https://amazon.com/dune"));
    }

    @Test
    void createBook_returns400_whenEshopUrlSetOnPaperBook() throws Exception {
        CreateBookRequest request = new CreateBookRequest("Clean Code", "Robert Martin", BookType.PAPER, "https://amazon.com", null);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBook_returns400_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"Author\",\"bookType\":\"PAPER\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/books/{id} ────────────────────────────────────────────────────

    @Test
    void getBook_returns200_whenBookExists() throws Exception {
        var created = bookService.create(USER_ID, new CreateBookRequest("Dune", "Frank Herbert", BookType.PAPER, null, null));

        mockMvc.perform(get("/api/books/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.title").value("Dune"));
    }

    @Test
    void getBook_returns404_whenBookNotFound() throws Exception {
        mockMvc.perform(get("/api/books/nonexistent")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/books/{id} ────────────────────────────────────────────────────

    @Test
    void updateBook_returns200_whenCallerIsOwner() throws Exception {
        var created = bookService.create(USER_ID, new CreateBookRequest("Old Title", "Author", BookType.PAPER, null, null));
        UpdateBookRequest update = new UpdateBookRequest("New Title", "Author", BookType.PAPER, null, null);

        mockMvc.perform(put("/api/books/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void updateBook_returns403_whenCallerIsNotOwner() throws Exception {
        var created = bookService.create(OTHER_USER_ID, new CreateBookRequest("Title", "Author", BookType.PAPER, null, null));
        UpdateBookRequest update = new UpdateBookRequest("Hacked Title", "Author", BookType.PAPER, null, null);

        mockMvc.perform(put("/api/books/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/books/{id} ─────────────────────────────────────────────────

    @Test
    void deleteBook_returns204_whenCallerIsOwner() throws Exception {
        var created = bookService.create(USER_ID, new CreateBookRequest("Title", "Author", BookType.PAPER, null, null));

        mockMvc.perform(delete("/api/books/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBook_returns403_whenCallerIsNotOwner() throws Exception {
        var created = bookService.create(OTHER_USER_ID, new CreateBookRequest("Title", "Author", BookType.PAPER, null, null));

        mockMvc.perform(delete("/api/books/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
    }
}
