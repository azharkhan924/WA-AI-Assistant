package com.whatsappai.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.whatsapp.gateway-url:http://localhost:8083}")
    private String gatewayUrl;

    /**
     * Sends a message to the Node.js Gateway.
     * @param toNumber recipient number
     * @param body message content
     */
    public void sendMessage(String toNumber, String body) {
        try {
            String url = gatewayUrl + "/api/whatsapp/sendMessage";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("to", toNumber);
            payload.put("text", body);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message sent to {} via Gateway", toNumber);
            } else {
                log.error("Failed to send message via Gateway, response: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending message via Gateway to {}: {}", toNumber, e.getMessage());
        }
    }
}
