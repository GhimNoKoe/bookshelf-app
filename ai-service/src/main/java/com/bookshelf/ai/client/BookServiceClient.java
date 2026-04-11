package com.bookshelf.ai.client;

import com.bookshelf.ai.client.dto.AuthorResponse;
import com.bookshelf.ai.client.dto.BookResponse;
import com.bookshelf.ai.client.dto.CreateBookClientRequest;
import com.bookshelf.ai.client.dto.SeriesResponse;
import com.bookshelf.ai.client.dto.SubSeriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookServiceClient {

    private final RestTemplate restTemplate;

    @Value("${book-service.base-url}")
    private String baseUrl;

    public AuthorResponse createAuthor(String name, String authorizationHeader) {
        HttpEntity<Map<String, String>> request = buildRequest(
                Map.of("name", name), authorizationHeader);
        return restTemplate.postForObject(baseUrl + "/api/authors", request, AuthorResponse.class);
    }

    public SeriesResponse createSeries(String name, String authorizationHeader) {
        HttpEntity<Map<String, String>> request = buildRequest(
                Map.of("name", name), authorizationHeader);
        return restTemplate.postForObject(baseUrl + "/api/series", request, SeriesResponse.class);
    }

    public SubSeriesResponse createSubSeries(String name, String seriesId, String authorizationHeader) {
        HttpEntity<Map<String, String>> request = buildRequest(
                Map.of("name", name, "seriesId", seriesId), authorizationHeader);
        return restTemplate.postForObject(baseUrl + "/api/sub-series", request, SubSeriesResponse.class);
    }

    public BookResponse createBook(CreateBookClientRequest bookRequest, String authorizationHeader) {
        HttpEntity<CreateBookClientRequest> request = buildRequest(bookRequest, authorizationHeader);
        return restTemplate.postForObject(baseUrl + "/api/books", request, BookResponse.class);
    }

    private <T> HttpEntity<T> buildRequest(T body, String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return new HttpEntity<>(body, headers);
    }
}
