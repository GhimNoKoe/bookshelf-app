package com.bookshelf.ai.controller;

import com.bookshelf.ai.dto.AiCommandRequest;
import com.bookshelf.ai.dto.AiCommandResponse;
import com.bookshelf.ai.service.ClaudeAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ClaudeAgentService claudeAgentService;

    @PostMapping("/command")
    public ResponseEntity<AiCommandResponse> executeCommand(
            @Valid @RequestBody AiCommandRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        AiCommandResponse response = claudeAgentService.executeCommand(
                request.prompt(), authorizationHeader);
        return ResponseEntity.ok(response);
    }
}
