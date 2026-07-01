package com.whatsappai.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "faqs")
@Getter
@Setter
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 1000)
    private String question;

    @Column(length = 3000)
    private String answer;

    @ManyToOne
    @JoinColumn(name = "business_id")
    @JsonIgnore
    private BusinessProfile business;
}
