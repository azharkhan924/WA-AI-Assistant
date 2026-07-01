package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.dto.ChatRequest;
import com.whatsappai.assistant.dto.ChatResponse;
import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatPreviewController {

    private final ConversationService conversationService;
    private final BusinessProfileController businessHelper;

    public ChatPreviewController(ConversationService conversationService,
                                  BusinessProfileController businessHelper) {
        this.conversationService = conversationService;
        this.businessHelper = businessHelper;
    }

    /**
     * Sends a test message to the AI using the saved business profile.
     * Used by the admin dashboard "Preview Bot" panel.
     */
    @PostMapping("/preview")
    public ChatResponse preview(@Valid @RequestBody ChatRequest chatReq, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        String customerNumber = chatReq.getCustomerNumber() != null
                ? chatReq.getCustomerNumber() : "preview-session";
        String reply = conversationService.handleIncomingMessage(b, customerNumber, chatReq.getMessage());
        return new ChatResponse(reply);
    }
}
