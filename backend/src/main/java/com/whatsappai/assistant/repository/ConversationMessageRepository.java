package com.whatsappai.assistant.repository;

import com.whatsappai.assistant.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, String> {
    List<ConversationMessage> findTop20ByBusinessIdAndCustomerNumberOrderByCreatedAtDesc(
            String businessId, String customerNumber);

    List<ConversationMessage> findByBusinessIdAndCustomerNumberOrderByCreatedAtAsc(
            String businessId, String customerNumber);
}
