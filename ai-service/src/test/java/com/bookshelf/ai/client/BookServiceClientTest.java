package com.bookshelf.ai.client;

import com.bookshelf.ai.client.dto.AuthorResponse;
import com.bookshelf.ai.client.dto.BookResponse;
import com.bookshelf.ai.client.dto.CreateBookClientRequest;
import com.bookshelf.ai.client.dto.SeriesResponse;
import com.bookshelf.ai.client.dto.SubSeriesResponse;
import com.bookshelf.ai.grpc.UserGrpcClient;
import com.bookshelf.ai.service.ClaudeAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class BookServiceClientTest {

    @Autowired BookServiceClient bookServiceClient;
    @Autowired RestTemplate restTemplate;

    @MockBean UserGrpcClient userGrpcClient;
    @MockBean ClaudeAgentService claudeAgentService;

    private MockRestServiceServer server;

    private static final String AUTH = "Bearer test-token";
    private static final String BASE = "http://localhost:8083";

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    // ── createAuthor ───────────────────────────────────────────────────────────

    @Test
    void createAuthor_postsToBookService_andReturnsAuthor() {
        server.expect(requestTo(BASE + "/api/authors"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", AUTH))
                .andRespond(withSuccess(
                        "{\"id\":\"a1\",\"name\":\"Terry Pratchett\"}",
                        MediaType.APPLICATION_JSON));

        AuthorResponse result = bookServiceClient.createAuthor("Terry Pratchett", AUTH);

        assertThat(result.id()).isEqualTo("a1");
        assertThat(result.name()).isEqualTo("Terry Pratchett");
        server.verify();
    }

    // ── createSeries ───────────────────────────────────────────────────────────

    @Test
    void createSeries_postsToBookService_andReturnsSeries() {
        server.expect(requestTo(BASE + "/api/series"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", AUTH))
                .andRespond(withSuccess(
                        "{\"id\":\"s1\",\"name\":\"Discworld\"}",
                        MediaType.APPLICATION_JSON));

        SeriesResponse result = bookServiceClient.createSeries("Discworld", AUTH);

        assertThat(result.id()).isEqualTo("s1");
        assertThat(result.name()).isEqualTo("Discworld");
        server.verify();
    }

    // ── createSubSeries ────────────────────────────────────────────────────────

    @Test
    void createSubSeries_postsToBookService_andReturnsSubSeries() {
        server.expect(requestTo(BASE + "/api/sub-series"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", AUTH))
                .andRespond(withSuccess(
                        "{\"id\":\"ss1\",\"name\":\"Rincewind\",\"seriesId\":\"s1\"}",
                        MediaType.APPLICATION_JSON));

        SubSeriesResponse result = bookServiceClient.createSubSeries("Rincewind", "s1", AUTH);

        assertThat(result.id()).isEqualTo("ss1");
        assertThat(result.name()).isEqualTo("Rincewind");
        assertThat(result.seriesId()).isEqualTo("s1");
        server.verify();
    }

    // ── createBook ─────────────────────────────────────────────────────────────

    @Test
    void createBook_postsToBookService_andReturnsBook() {
        server.expect(requestTo(BASE + "/api/books"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", AUTH))
                .andRespond(withSuccess(
                        "{\"id\":\"b1\",\"title\":\"The Colour of Magic\"}",
                        MediaType.APPLICATION_JSON));

        CreateBookClientRequest request = CreateBookClientRequest.builder()
                .title("The Colour of Magic")
                .authorIds(List.of("a1"))
                .bookType("PAPER")
                .seriesId("s1")
                .subSeriesId("ss1")
                .seriesOrder(1)
                .subSeriesOrder(1)
                .build();

        BookResponse result = bookServiceClient.createBook(request, AUTH);

        assertThat(result.id()).isEqualTo("b1");
        assertThat(result.title()).isEqualTo("The Colour of Magic");
        server.verify();
    }
}
