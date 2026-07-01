package com.whatsappai.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policies")
@Getter
@Setter
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // e.g. "Refund Policy", "Cancellation Policy", "Delivery Policy", "Payment Policy"
    private String title;

    @Column(length = 3000)
    private String content;

    @ManyToOne
    @JoinColumn(name = "business_id")
    @JsonIgnore
    private BusinessProfile business;
}
