package com.whatsappai.assistant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "conversation_messages")
@Getter
@Setter
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String businessId;

    // The customer's WhatsApp number (or "test-session" for dashboard preview chats)
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(length = 4000)
    private String content;

    // true if a human agent has taken over this conversation thread
    private boolean handedOffToHuman = false;

    private Instant createdAt = Instant.now();

    public enum Role {
        USER, ASSISTANT
    }
}
