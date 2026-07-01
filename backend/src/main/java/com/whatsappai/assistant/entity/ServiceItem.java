package com.whatsappai.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "service_items")
@Getter
@Setter
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Column(length = 2000)
    private String description;

    // Stored as text so businesses can write "₹499" or "Starting at $20" — never invented by the AI
    private String price;

    private boolean available = true;

    @ManyToOne
    @JoinColumn(name = "business_id")
    @JsonIgnore
    private BusinessProfile business;
}
