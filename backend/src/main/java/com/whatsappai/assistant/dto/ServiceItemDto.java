package com.whatsappai.assistant.dto;

import lombok.Data;

@Data
public class ServiceItemDto {
    private String id;
    private String name;
    private String description;
    private String price;
    private boolean available;
}
