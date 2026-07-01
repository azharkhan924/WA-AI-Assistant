package com.whatsappai.assistant.service;

import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.entity.ConversationMessage;
import com.whatsappai.assistant.repository.ConversationMessageRepository;
import com.whatsappai.assistant.repository.BusinessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationMessageRepository messageRepository;
    private final BusinessProfileRepository businessRepository;
    private final PromptBuilderService promptBuilderService;
    private final AiService aiService;

    private static final int HISTORY_TURNS = 10;

    public ConversationService(ConversationMessageRepository messageRepository,
                                BusinessProfileRepository businessRepository,
                                PromptBuilderService promptBuilderService,
                                AiService aiService) {
        this.messageRepository = messageRepository;
        this.businessRepository = businessRepository;
        this.promptBuilderService = promptBuilderService;
        this.aiService = aiService;
    }

    @Transactional
    public String handleIncomingMessage(BusinessProfile businessParam, String customerNumber, String userMessage) {
        BusinessProfile business = businessParam.getId() != null
                ? businessRepository.findById(businessParam.getId()).orElse(businessParam)
                : businessParam;

        // Save the incoming user message
        saveMessage(business.getId(), customerNumber, ConversationMessage.Role.USER, userMessage);

        // Build prior conversation context (oldest first)
        List<ConversationMessage> recent = messageRepository
                .findTop20ByBusinessIdAndCustomerNumberOrderByCreatedAtDesc(business.getId(), customerNumber);
        Collections.reverse(recent);

        List<String[]> turns = new ArrayList<>();
        // Exclude the message we just saved (last item) from history; it's passed separately
        int limit = Math.max(0, recent.size() - 1);
        int start = Math.max(0, limit - (HISTORY_TURNS * 2));
        for (int i = start; i < limit; i++) {
            ConversationMessage m = recent.get(i);
            String role = m.getRole() == ConversationMessage.Role.USER ? "user" : "assistant";
            turns.add(new String[]{role, m.getContent()});
        }

        String systemPrompt = promptBuilderService.buildSystemPrompt(business);
        String reply = aiService.generateReply(systemPrompt, turns, userMessage, business.getAiProvider());

        saveMessage(business.getId(), customerNumber, ConversationMessage.Role.ASSISTANT, reply);
        return reply;
    }

    private void saveMessage(String businessId, String customerNumber, ConversationMessage.Role role, String content) {
        ConversationMessage m = new ConversationMessage();
        m.setBusinessId(businessId);
        m.setCustomerNumber(customerNumber);
        m.setRole(role);
        m.setContent(content);
        messageRepository.save(m);
    }
}
