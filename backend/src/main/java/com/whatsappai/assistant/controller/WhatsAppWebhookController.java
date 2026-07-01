package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.repository.BusinessProfileRepository;
import com.whatsappai.assistant.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Twilio sends an HTTP POST to this endpoint each time a WhatsApp message
 * arrives on your configured number. Configure the Twilio webhook URL to:
 *
 *   https://your-domain.com/api/webhook/whatsapp
 *
 * Twilio POST body (form-encoded) includes:
 *   - From:  "whatsapp:+919876543210"
 *   - To:    "whatsapp:+14155238886" (your Twilio sandbox or approved number)
 *   - Body:  "Hello, what are your hours?"
 */
@RestController
@RequestMapping("/api/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final BusinessProfileRepository businessProfileRepository;
    private final ConversationService conversationService;

    @Value("${app.twilio.whatsapp-number}")
    private String twilioNumber;

    public WhatsAppWebhookController(BusinessProfileRepository businessProfileRepository,
                                      ConversationService conversationService) {
        this.businessProfileRepository = businessProfileRepository;
        this.conversationService = conversationService;
    }

    @PostMapping(value = "/whatsapp",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> receiveMessage(@RequestParam Map<String, String> params) {
        String from   = params.getOrDefault("From", "");
        String to     = params.getOrDefault("To", twilioNumber);
        String body   = params.getOrDefault("Body", "").trim();

        log.info("Webhook received: from={} to={} body={}", from, to, body);

        if (body.isEmpty()) {
            return ResponseEntity.ok(twiml("I didn't catch that — could you please send a text message?"));
        }

        // Route to the business that owns this destination number
        Optional<BusinessProfile> businessOpt = businessProfileRepository.findByWhatsappNumber(to);

        if (businessOpt.isEmpty()) {
            // Fallback: if only one business is configured, use it (makes single-tenant setup easier)
            long count = businessProfileRepository.count();
            if (count == 1) {
                businessOpt = Optional.of(businessProfileRepository.findAll().get(0));
            }
        }

        if (businessOpt.isEmpty() || !businessOpt.get().isActive()) {
            log.warn("No active business found for WhatsApp number: {}", to);
            return ResponseEntity.ok(twiml("This service is not currently available. Please try again later."));
        }

        BusinessProfile business = businessOpt.get();

        String reply;
        try {
            reply = conversationService.handleIncomingMessage(business, from, body);
        } catch (Exception e) {
            log.error("Error generating reply for business {}", business.getId(), e);
            reply = "Sorry, I'm having trouble responding right now. Please contact us directly at "
                    + (business.getContactNumber() != null ? business.getContactNumber() : "our support line.")
                    + ".";
        }

        return ResponseEntity.ok(twiml(reply));
    }

    /** Wraps a plain text message in TwiML so Twilio sends it as a WhatsApp reply. */
    private String twiml(String message) {
        // Escape XML special chars
        String safe = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<Response>\n" +
               "  <Message>" + safe + "</Message>\n" +
               "</Response>";
    }
}
