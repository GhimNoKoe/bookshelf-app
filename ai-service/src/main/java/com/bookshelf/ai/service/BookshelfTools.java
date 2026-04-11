package com.bookshelf.ai.service;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class BookshelfTools {

    private final SchemaGenerator schemaGenerator;

    public BookshelfTools() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(new JacksonModule());
        configBuilder.forFields().withRequiredCheck(field -> true);
        this.schemaGenerator = new SchemaGenerator(configBuilder.build());
    }

    public List<Tool> getTools() {
        return List.of(
                toolFromClass(CreateAuthorInput.class),
                toolFromClass(CreateSeriesInput.class),
                toolFromClass(CreateSubSeriesInput.class),
                toolFromClass(CreateBookInput.class)
        );
    }

    private Tool toolFromClass(Class<?> inputClass) {
        ObjectNode schema = schemaGenerator.generateSchema(inputClass);

        String name = Optional.ofNullable(inputClass.getAnnotation(JsonTypeName.class))
                .map(JsonTypeName::value)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing @JsonTypeName on " + inputClass.getSimpleName()));

        String description = Optional.ofNullable(inputClass.getAnnotation(JsonClassDescription.class))
                .map(JsonClassDescription::value)
                .orElse(null);

        Tool.InputSchema.Properties.Builder propsBuilder = Tool.InputSchema.Properties.builder();
        ObjectNode propsNode = (ObjectNode) schema.get("properties");
        if (propsNode != null) {
            propsNode.fields().forEachRemaining(entry ->
                    propsBuilder.putAdditionalProperty(entry.getKey(), JsonValue.fromJsonNode(entry.getValue()))
            );
        }

        List<String> required = new ArrayList<>();
        if (schema.has("required")) {
            schema.get("required").forEach(node -> required.add(node.asText()));
        }

        Tool.Builder toolBuilder = Tool.builder()
                .name(name)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(propsBuilder.build())
                        .required(required)
                        .build());
        if (description != null) {
            toolBuilder.description(description);
        }
        return toolBuilder.build();
    }

    // ── Tool input classes ────────────────────────────────────────────────────

    @JsonTypeName("create_author")
    @JsonClassDescription("Create a new author in the bookshelf catalog")
    public record CreateAuthorInput(
            @JsonPropertyDescription("Full name of the author")
            String name
    ) {}

    @JsonTypeName("create_series")
    @JsonClassDescription("Create a new book series in the bookshelf catalog")
    public record CreateSeriesInput(
            @JsonPropertyDescription("Name of the series")
            String name
    ) {}

    @JsonTypeName("create_sub_series")
    @JsonClassDescription("Create a new sub-series within an existing series")
    public record CreateSubSeriesInput(
            @JsonPropertyDescription("Name of the sub-series")
            String name,

            @JsonPropertyDescription("ID of the parent series")
            String seriesId
    ) {}

    @JsonTypeName("create_book")
    @JsonClassDescription("Create a new book in the bookshelf catalog")
    public record CreateBookInput(
            @JsonPropertyDescription("Title of the book")
            String title,

            @JsonPropertyDescription("Original title if different from translated title")
            String originalTitle,

            @JsonPropertyDescription("List of author IDs")
            List<String> authorIds,

            @JsonPropertyDescription("Type of book format: PAPER, EBOOK, or AUDIOBOOK")
            String bookType,

            @JsonPropertyDescription("URL to purchase the book")
            String eshopUrl,

            @JsonPropertyDescription("ID of the series this book belongs to")
            String seriesId,

            @JsonPropertyDescription("ID of the sub-series this book belongs to")
            String subSeriesId,

            @JsonPropertyDescription("Position of this book within the series")
            Integer seriesOrder,

            @JsonPropertyDescription("Position of this book within the sub-series")
            Integer subSeriesOrder
    ) {}
}
