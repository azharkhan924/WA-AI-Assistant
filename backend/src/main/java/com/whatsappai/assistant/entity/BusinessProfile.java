package com.whatsappai.assistant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business_profiles")
@Getter
@Setter
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Used to route incoming WhatsApp numbers to the right tenant
    @Column(unique = true)
    private String whatsappNumber;

    private String businessName;
    private String industry;

    @Column(length = 4000)
    private String description;

    private String contactNumber;
    private String website;

    @Column(length = 1000)
    private String address;

    private String workingHours;
    private String timezone;

    // comma separated, e.g. "English, Hindi"
    private String languages;

    // FRIENDLY, FORMAL, CASUAL, PROFESSIONAL, ENTHUSIASTIC
    @Enumerated(EnumType.STRING)
    private Tone tone = Tone.PROFESSIONAL;

    private boolean useEmojis = false;

    @Column(length = 8000)
    private String knowledgeBase;

    @Column(length = 4000)
    private String customInstructions;

    // "anthropic" or "openai" — per-tenant override; falls back to global default if null
    private String aiProvider;

    private boolean allowGeneralConversation = false;

    private boolean active = true;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Faq> faqs = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceItem> services = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Policy> policies = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum Tone {
        FRIENDLY, FORMAL, CASUAL, PROFESSIONAL, ENTHUSIASTIC
    }
}
