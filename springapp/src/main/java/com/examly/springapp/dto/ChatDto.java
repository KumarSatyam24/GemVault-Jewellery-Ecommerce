package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ChatDto {
    private ChatDto() {
    }

    public record ChatRequest(
            @NotBlank @Size(max = 1000) String message,
            @Pattern(regexp = "[0-9a-fA-F-]{36}") String sessionId) {
    }

    public record ChatResponse(String reply, boolean matched, String matchedQuestion,
            String category, double confidence, String source, String sessionId,
            String resolvedQuestion) {
    }

    public record FaqResponse(Long id, String category, String question, String answer) {
    }
}
