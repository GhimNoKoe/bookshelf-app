package com.bookshelf.ai.controller;

import com.bookshelf.ai.dto.AiCommandRequest;
import com.bookshelf.ai.dto.AiCommandResponse;
import com.bookshelf.ai.grpc.UserGrpcClient;
import com.bookshelf.ai.service.ClaudeAgentService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserGrpcClient userGrpcClient;
    @MockBean ClaudeAgentService claudeAgentService;

    private static final String TOKEN = "test-token";
    private static final String USER_ID = "user-123";

    @BeforeEach
    void stubTokenValidation() {
        ValidateTokenResponse valid = ValidateTokenResponse.newBuilder()
                .setValid(true).setUserId(USER_ID).setUsername("alice").build();
        when(userGrpcClient.validateToken(TOKEN)).thenReturn(valid);
    }

    // ── POST /api/ai/command ───────────────────────────────────────────────────

    @Test
    void executeCommand_returns200_withResult() throws Exception {
        AiCommandResponse response = AiCommandResponse.builder()
                .result("Done.")
                .actionsPerformed(List.of("Created author: Terry Pratchett"))
                .build();
        when(claudeAgentService.executeCommand(eq("add Terry Pratchett"), eq("Bearer " + TOKEN)))
                .thenReturn(response);

        mockMvc.perform(post("/api/ai/command")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiCommandRequest.builder().prompt("add Terry Pratchett").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Done."))
                .andExpect(jsonPath("$.actionsPerformed[0]").value("Created author: Terry Pratchett"));
    }

    @Test
    void executeCommand_returns401_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/ai/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiCommandRequest.builder().prompt("add Terry Pratchett").build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void executeCommand_returns400_whenPromptIsBlank() throws Exception {
        mockMvc.perform(post("/api/ai/command")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiCommandRequest.builder().prompt("").build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeCommand_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/ai/command")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
