package com.whatsappai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank
    private String message;

    // Customer identifier for the test/preview chat — defaults to "test-session" if omitted
    private String customerNumber;
}
