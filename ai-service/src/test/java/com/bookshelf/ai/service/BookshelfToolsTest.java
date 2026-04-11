package com.bookshelf.ai.service;

import com.anthropic.models.messages.Tool;
import com.bookshelf.ai.grpc.UserGrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.anthropic.core.JsonValue;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookshelfToolsTest {

    @Autowired BookshelfTools bookshelfTools;

    @MockBean UserGrpcClient userGrpcClient;
    @MockBean ClaudeAgentService claudeAgentService;

    // ── getTools returns exactly 4 tools ──────────────────────────────────────

    @Test
    void getTools_returnsFourTools() {
        List<Tool> tools = bookshelfTools.getTools();
        assertThat(tools).hasSize(4);
    }

    // ── create_author ─────────────────────────────────────────────────────────

    @Test
    void createAuthorTool_hasCorrectName() {
        Tool tool = findTool("create_author");
        assertThat(tool.name()).isEqualTo("create_author");
    }

    @Test
    void createAuthorTool_hasDescription() {
        Tool tool = findTool("create_author");
        assertThat(tool.description()).isPresent();
        assertThat(tool.description().get()).isNotBlank();
    }

    @Test
    void createAuthorTool_requiresName() {
        Tool tool = findTool("create_author");
        assertThat(tool.inputSchema().required().orElse(List.of())).contains("name");
    }

    @Test
    void createAuthorTool_hasNameProperty() {
        Tool tool = findTool("create_author");
        Map<String, JsonValue> props = tool.inputSchema().properties().get()._additionalProperties();
        assertThat(props).containsKey("name");
    }

    // ── create_series ─────────────────────────────────────────────────────────

    @Test
    void createSeriesTool_hasCorrectName() {
        Tool tool = findTool("create_series");
        assertThat(tool.name()).isEqualTo("create_series");
    }

    @Test
    void createSeriesTool_hasDescription() {
        Tool tool = findTool("create_series");
        assertThat(tool.description()).isPresent();
        assertThat(tool.description().get()).isNotBlank();
    }

    @Test
    void createSeriesTool_requiresName() {
        Tool tool = findTool("create_series");
        assertThat(tool.inputSchema().required().orElse(List.of())).contains("name");
    }

    // ── create_sub_series ─────────────────────────────────────────────────────

    @Test
    void createSubSeriesTool_hasCorrectName() {
        Tool tool = findTool("create_sub_series");
        assertThat(tool.name()).isEqualTo("create_sub_series");
    }

    @Test
    void createSubSeriesTool_hasDescription() {
        Tool tool = findTool("create_sub_series");
        assertThat(tool.description()).isPresent();
        assertThat(tool.description().get()).isNotBlank();
    }

    @Test
    void createSubSeriesTool_requiresNameAndSeriesId() {
        Tool tool = findTool("create_sub_series");
        assertThat(tool.inputSchema().required().orElse(List.of())).contains("name", "seriesId");
    }

    @Test
    void createSubSeriesTool_hasSeriesIdProperty() {
        Tool tool = findTool("create_sub_series");
        Map<String, JsonValue> props = tool.inputSchema().properties().get()._additionalProperties();
        assertThat(props).containsKey("seriesId");
    }

    // ── create_book ───────────────────────────────────────────────────────────

    @Test
    void createBookTool_hasCorrectName() {
        Tool tool = findTool("create_book");
        assertThat(tool.name()).isEqualTo("create_book");
    }

    @Test
    void createBookTool_hasDescription() {
        Tool tool = findTool("create_book");
        assertThat(tool.description()).isPresent();
        assertThat(tool.description().get()).isNotBlank();
    }

    @Test
    void createBookTool_requiresTitleAuthorIdsAndBookType() {
        Tool tool = findTool("create_book");
        assertThat(tool.inputSchema().required().orElse(List.of())).contains("title", "authorIds", "bookType");
    }

    @Test
    void createBookTool_hasOptionalSeriesFields() {
        Tool tool = findTool("create_book");
        Map<String, JsonValue> props = tool.inputSchema().properties().get()._additionalProperties();
        assertThat(props).containsKeys("seriesId", "subSeriesId", "seriesOrder", "subSeriesOrder");
    }

    @Test
    void createBookTool_hasOptionalOriginalTitleAndEshopUrl() {
        Tool tool = findTool("create_book");
        Map<String, JsonValue> props = tool.inputSchema().properties().get()._additionalProperties();
        assertThat(props).containsKeys("originalTitle", "eshopUrl");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Tool findTool(String name) {
        return bookshelfTools.getTools().stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tool not found: " + name));
    }
}
