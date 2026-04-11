package com.bookshelf.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.bookshelf.ai.client.BookServiceClient;
import com.bookshelf.ai.client.dto.AuthorResponse;
import com.bookshelf.ai.client.dto.BookResponse;
import com.bookshelf.ai.client.dto.CreateBookClientRequest;
import com.bookshelf.ai.client.dto.SeriesResponse;
import com.bookshelf.ai.client.dto.SubSeriesResponse;
import com.bookshelf.ai.dto.AiCommandResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClaudeAgentServiceImpl implements ClaudeAgentService {

    private static final int MAX_ITERATIONS = 10;

    private final AnthropicClient anthropicClient;
    private final BookServiceClient bookServiceClient;
    private final BookshelfTools bookshelfTools;

    @Override
    public AiCommandResponse executeCommand(String prompt, String token) {
        List<String> actionsPerformed = new ArrayList<>();

        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(Model.CLAUDE_OPUS_4_6)
                .maxTokens(4096)
                .tools(bookshelfTools.getTools().stream()
                        .map(com.anthropic.models.messages.ToolUnion::ofTool)
                        .toList())
                .addUserMessage(prompt);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Message response = anthropicClient.messages().create(paramsBuilder.build());

            Optional<StopReason> stopReason = response.stopReason();

            if (stopReason.map(StopReason.END_TURN::equals).orElse(false)) {
                String result = response.content().stream()
                        .map(ContentBlock::text)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(com.anthropic.models.messages.TextBlock::text)
                        .findFirst()
                        .orElse("");
                return AiCommandResponse.builder()
                        .result(result)
                        .actionsPerformed(actionsPerformed)
                        .build();
            }

            if (stopReason.map(StopReason.TOOL_USE::equals).orElse(false)) {
                paramsBuilder.addMessage(response.toParam());

                List<ContentBlockParam> toolResults = new ArrayList<>();
                for (ContentBlock block : response.content()) {
                    block.toolUse().ifPresent(toolUse -> {
                        String toolResult = executeTool(toolUse, token, actionsPerformed);
                        toolResults.add(ContentBlockParam.ofToolResult(
                                ToolResultBlockParam.builder()
                                        .toolUseId(toolUse.id())
                                        .content(toolResult)
                                        .build()));
                    });
                }
                paramsBuilder.addUserMessageOfBlockParams(toolResults);
            }
        }

        return AiCommandResponse.builder()
                .result("Maximum iterations reached without completing the task.")
                .actionsPerformed(actionsPerformed)
                .build();
    }

    private String executeTool(ToolUseBlock toolUse, String token, List<String> actionsPerformed) {
        Map<String, Object> input = toolUse._input().convert(new TypeReference<>() {});

        return switch (toolUse.name()) {
            case "create_author" -> {
                String name = (String) input.get("name");
                AuthorResponse author = bookServiceClient.createAuthor(name, token);
                actionsPerformed.add("Created author: " + author.name() + " (id: " + author.id() + ")");
                yield "Author created successfully: " + author.name() + " (id: " + author.id() + ")";
            }
            case "create_series" -> {
                String name = (String) input.get("name");
                SeriesResponse series = bookServiceClient.createSeries(name, token);
                actionsPerformed.add("Created series: " + series.name() + " (id: " + series.id() + ")");
                yield "Series created successfully: " + series.name() + " (id: " + series.id() + ")";
            }
            case "create_sub_series" -> {
                String name = (String) input.get("name");
                String seriesId = (String) input.get("seriesId");
                SubSeriesResponse subSeries = bookServiceClient.createSubSeries(name, seriesId, token);
                actionsPerformed.add("Created sub-series: " + subSeries.name() + " (id: " + subSeries.id() + ")");
                yield "Sub-series created successfully: " + subSeries.name() + " (id: " + subSeries.id() + ")";
            }
            case "create_book" -> {
                CreateBookClientRequest request = buildCreateBookRequest(input);
                BookResponse book = bookServiceClient.createBook(request, token);
                actionsPerformed.add("Created book: " + book.title() + " (id: " + book.id() + ")");
                yield "Book created successfully: " + book.title() + " (id: " + book.id() + ")";
            }
            default -> "Unknown tool: " + toolUse.name();
        };
    }

    @SuppressWarnings("unchecked")
    private CreateBookClientRequest buildCreateBookRequest(Map<String, Object> input) {
        return CreateBookClientRequest.builder()
                .title((String) input.get("title"))
                .originalTitle((String) input.get("originalTitle"))
                .authorIds((List<String>) input.get("authorIds"))
                .bookType((String) input.get("bookType"))
                .eshopUrl((String) input.get("eshopUrl"))
                .seriesId((String) input.get("seriesId"))
                .subSeriesId((String) input.get("subSeriesId"))
                .seriesOrder(input.get("seriesOrder") != null ? ((Number) input.get("seriesOrder")).intValue() : null)
                .subSeriesOrder(input.get("subSeriesOrder") != null ? ((Number) input.get("subSeriesOrder")).intValue() : null)
                .build();
    }
}
