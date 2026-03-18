package com.bookshelf.shelf.controller;

import com.bookshelf.grpc.user.ValidateTokenResponse;
import com.bookshelf.shelf.dto.CreateShelfRequest;
import com.bookshelf.shelf.dto.ShelfDto;
import com.bookshelf.shelf.grpc.UserGrpcClient;
import com.bookshelf.shelf.service.ShelfService;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShelfControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ShelfService shelfService;

    @MockBean UserGrpcClient userGrpcClient;  // replaces the external gRPC call in JwtAuthFilter

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
    }

    // ── GET /api/shelves ───────────────────────────────────────────────────────

    @Test
    void listShelves_createsAndReturnsDefaultShelves_onFirstAccess() throws Exception {
        mockMvc.perform(get("/api/shelves").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))  // READ, CURRENTLY_READING, OWNED, WISH_LIST
                .andExpect(jsonPath("$[0].userId").value(USER_ID));
    }

    @Test
    void listShelves_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/shelves"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/shelves ──────────────────────────────────────────────────────

    @Test
    void createShelf_returns201WithNewCustomShelf() throws Exception {
        mockMvc.perform(post("/api/shelves")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShelfRequest("Favourites"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Favourites"))
                .andExpect(jsonPath("$.shelfType").value("CUSTOM"))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void createShelf_returns409_whenNameAlreadyExists() throws Exception {
        shelfService.createShelf(USER_ID, new CreateShelfRequest("Favourites"));

        mockMvc.perform(post("/api/shelves")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShelfRequest("Favourites"))))
                .andExpect(status().isConflict());
    }

    // ── GET /api/shelves/{id} ──────────────────────────────────────────────────

    @Test
    void getShelf_returns200WithShelf() throws Exception {
        ShelfDto created = shelfService.createShelf(USER_ID, new CreateShelfRequest("My Books"));

        mockMvc.perform(get("/api/shelves/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.name").value("My Books"));
    }

    @Test
    void getShelf_returns403_whenShelfBelongsToAnotherUser() throws Exception {
        ShelfDto otherUserShelf = shelfService.createShelf("other-user", new CreateShelfRequest("Private"));

        mockMvc.perform(get("/api/shelves/" + otherUserShelf.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/shelves/{id} ───────────────────────────────────────────────

    @Test
    void deleteShelf_returns204_forCustomShelf() throws Exception {
        ShelfDto created = shelfService.createShelf(USER_ID, new CreateShelfRequest("Temporary"));

        mockMvc.perform(delete("/api/shelves/" + created.id())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteShelf_returns403_forDefaultShelf() throws Exception {
        List<ShelfDto> defaults = shelfService.getShelvesByUser(USER_ID);
        String defaultShelfId = defaults.get(0).id();

        mockMvc.perform(delete("/api/shelves/" + defaultShelfId)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/shelves/{id}/books ───────────────────────────────────────────

    @Test
    void addBook_returns200WithBookInList() throws Exception {
        ShelfDto shelf = shelfService.createShelf(USER_ID, new CreateShelfRequest("To Read"));

        mockMvc.perform(post("/api/shelves/" + shelf.id() + "/books")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"book-abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookIds[0]").value("book-abc"));
    }

    @Test
    void addBook_returns409_whenBookAlreadyOnShelf() throws Exception {
        ShelfDto shelf = shelfService.createShelf(USER_ID, new CreateShelfRequest("To Read"));
        shelfService.addBook(shelf.id(), USER_ID, "book-abc");

        mockMvc.perform(post("/api/shelves/" + shelf.id() + "/books")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"book-abc\"}"))
                .andExpect(status().isConflict());
    }

    // ── DELETE /api/shelves/{id}/books/{bookId} ────────────────────────────────

    @Test
    void removeBook_returns204() throws Exception {
        ShelfDto shelf = shelfService.createShelf(USER_ID, new CreateShelfRequest("To Read"));
        shelfService.addBook(shelf.id(), USER_ID, "book-abc");

        mockMvc.perform(delete("/api/shelves/" + shelf.id() + "/books/book-abc")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }
}
