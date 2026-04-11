package com.bookshelf.ai.service;

import com.bookshelf.ai.dto.AiCommandResponse;

public interface ClaudeAgentService {

    AiCommandResponse executeCommand(String prompt, String token);
}
