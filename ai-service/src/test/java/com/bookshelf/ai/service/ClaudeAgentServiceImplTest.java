package com.bookshelf.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.services.blocking.MessageService;
import com.bookshelf.ai.client.BookServiceClient;
import com.bookshelf.ai.client.dto.AuthorResponse;
import com.bookshelf.ai.client.dto.BookResponse;
import com.bookshelf.ai.client.dto.SeriesResponse;
import com.bookshelf.ai.dto.AiCommandResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaudeAgentServiceImplTest {

    @Mock AnthropicClient anthropicClient;
    @Mock MessageService messageService;
    @Mock BookServiceClient bookServiceClient;
    @Mock BookshelfTools bookshelfTools;

    @InjectMocks ClaudeAgentServiceImpl claudeAgentService;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        when(anthropicClient.messages()).thenReturn(messageService);
        when(bookshelfTools.getTools()).thenReturn(List.of());
    }

    // ── end_turn with no tool use ─────────────────────────────────────────────

    @Test
    void executeCommand_endTurnImmediately_returnsTextResult() {
        Message response = endTurnMessage("I don't know how to help with that.");
        when(messageService.create(any(com.anthropic.models.messages.MessageCreateParams.class)))
                .thenReturn(response);

        AiCommandResponse result = claudeAgentService.executeCommand("do something", TOKEN);

        assertThat(result.result()).isEqualTo("I don't know how to help with that.");
        assertThat(result.actionsPerformed()).isEmpty();
        verify(bookServiceClient, never()).createAuthor(any(), any());
    }

    // ── create_author tool use ────────────────────────────────────────────────

    @Test
    void executeCommand_createAuthor_callsBookServiceAndReturnsAction() {
        Message toolUse = toolUseMessage("call-1", "create_author", Map.of("name", "Terry Pratchett"));
        Message endTurn = endTurnMessage("Created author Terry Pratchett.");
        when(messageService.create(any(com.anthropic.models.messages.MessageCreateParams.class)))
                .thenReturn(toolUse, endTurn);
        when(bookServiceClient.createAuthor("Terry Pratchett", TOKEN))
                .thenReturn(AuthorResponse.builder().id("a1").name("Terry Pratchett").build());

        AiCommandResponse result = claudeAgentService.executeCommand("add Terry Pratchett", TOKEN);

        assertThat(result.result()).isEqualTo("Created author Terry Pratchett.");
        assertThat(result.actionsPerformed()).containsExactly("Created author: Terry Pratchett (id: a1)");
        verify(bookServiceClient).createAuthor("Terry Pratchett", TOKEN);
    }

    // ── create_series tool use ────────────────────────────────────────────────

    @Test
    void executeCommand_createSeries_callsBookServiceAndReturnsAction() {
        Message toolUse = toolUseMessage("call-2", "create_series", Map.of("name", "Discworld"));
        Message endTurn = endTurnMessage("Created series Discworld.");
        when(messageService.create(any(com.anthropic.models.messages.MessageCreateParams.class)))
                .thenReturn(toolUse, endTurn);
        when(bookServiceClient.createSeries("Discworld", TOKEN))
                .thenReturn(SeriesResponse.builder().id("s1").name("Discworld").build());

        AiCommandResponse result = claudeAgentService.executeCommand("add Discworld series", TOKEN);

        assertThat(result.result()).isEqualTo("Created series Discworld.");
        assertThat(result.actionsPerformed()).containsExactly("Created series: Discworld (id: s1)");
        verify(bookServiceClient).createSeries("Discworld", TOKEN);
    }

    // ── create_book tool use ──────────────────────────────────────────────────

    @Test
    void executeCommand_createBook_callsBookServiceAndReturnsAction() {
        Message toolUse = toolUseMessage("call-3", "create_book",
                Map.of("title", "The Colour of Magic", "authorIds", List.of("a1"), "bookType", "PAPER"));
        Message endTurn = endTurnMessage("Created book The Colour of Magic.");
        when(messageService.create(any(com.anthropic.models.messages.MessageCreateParams.class)))
                .thenReturn(toolUse, endTurn);
        when(bookServiceClient.createBook(any(), any()))
                .thenReturn(BookResponse.builder().id("b1").title("The Colour of Magic").build());

        AiCommandResponse result = claudeAgentService.executeCommand("add the book", TOKEN);

        assertThat(result.result()).isEqualTo("Created book The Colour of Magic.");
        assertThat(result.actionsPerformed()).containsExactly("Created book: The Colour of Magic (id: b1)");
        verify(bookServiceClient).createBook(any(), any());
    }

    // ── multiple tools across turns ───────────────────────────────────────────

    @Test
    void executeCommand_multipleToolCalls_returnsAllActions() {
        Message authorTool = toolUseMessage("call-a", "create_author", Map.of("name", "Terry Pratchett"));
        Message seriesTool = toolUseMessage("call-s", "create_series", Map.of("name", "Discworld"));
        Message endTurn = endTurnMessage("Done.");
        when(messageService.create(any(com.anthropic.models.messages.MessageCreateParams.class)))
                .thenReturn(authorTool, seriesTool, endTurn);
        when(bookServiceClient.createAuthor("Terry Pratchett", TOKEN))
                .thenReturn(AuthorResponse.builder().id("a1").name("Terry Pratchett").build());
        when(bookServiceClient.createSeries("Discworld", TOKEN))
                .thenReturn(SeriesResponse.builder().id("s1").name("Discworld").build());

        AiCommandResponse result = claudeAgentService.executeCommand(
                "add Terry Pratchett and Discworld", TOKEN);

        assertThat(result.result()).isEqualTo("Done.");
        assertThat(result.actionsPerformed()).containsExactlyInAnyOrder(
                "Created author: Terry Pratchett (id: a1)",
                "Created series: Discworld (id: s1)"
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Message endTurnMessage(String text) {
        TextBlock textBlock = mock(TextBlock.class);
        when(textBlock.text()).thenReturn(text);

        ContentBlock contentBlock = mock(ContentBlock.class);
        when(contentBlock.text()).thenReturn(Optional.of(textBlock));

        Message msg = mock(Message.class);
        when(msg.stopReason()).thenReturn(Optional.of(StopReason.END_TURN));
        when(msg.content()).thenReturn(List.of(contentBlock));
        return msg;
    }

    private Message toolUseMessage(String toolUseId, String toolName, Map<String, Object> input) {
        ToolUseBlock toolUseBlock = mock(ToolUseBlock.class);
        when(toolUseBlock.id()).thenReturn(toolUseId);
        when(toolUseBlock.name()).thenReturn(toolName);
        when(toolUseBlock._input()).thenReturn(JsonValue.from(input));

        ContentBlock contentBlock = mock(ContentBlock.class);
        when(contentBlock.toolUse()).thenReturn(Optional.of(toolUseBlock));

        Message msg = mock(Message.class);
        when(msg.stopReason()).thenReturn(Optional.of(StopReason.TOOL_USE));
        when(msg.content()).thenReturn(List.of(contentBlock));
        when(msg.toParam()).thenReturn(MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content("assistant placeholder")
                .build());
        return msg;
    }
}
