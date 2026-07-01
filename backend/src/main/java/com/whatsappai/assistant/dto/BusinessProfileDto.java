package com.whatsappai.assistant.dto;

import com.whatsappai.assistant.entity.BusinessProfile;
import lombok.Data;

@Data
public class BusinessProfileDto {
    private String id;
    private String whatsappNumber;
    private String businessName;
    private String industry;
    private String description;
    private String contactNumber;
    private String website;
    private String address;
    private String workingHours;
    private String timezone;
    private String languages;
    private String tone;
    private boolean useEmojis;
    private String knowledgeBase;
    private String customInstructions;
    private String aiProvider;
    private boolean allowGeneralConversation;
    private boolean active;

    public static BusinessProfileDto fromEntity(BusinessProfile b) {
        BusinessProfileDto dto = new BusinessProfileDto();
        dto.setId(b.getId());
        dto.setWhatsappNumber(b.getWhatsappNumber());
        dto.setBusinessName(b.getBusinessName());
        dto.setIndustry(b.getIndustry());
        dto.setDescription(b.getDescription());
        dto.setContactNumber(b.getContactNumber());
        dto.setWebsite(b.getWebsite());
        dto.setAddress(b.getAddress());
        dto.setWorkingHours(b.getWorkingHours());
        dto.setTimezone(b.getTimezone());
        dto.setLanguages(b.getLanguages());
        dto.setTone(b.getTone() != null ? b.getTone().name() : null);
        dto.setUseEmojis(b.isUseEmojis());
        dto.setKnowledgeBase(b.getKnowledgeBase());
        dto.setCustomInstructions(b.getCustomInstructions());
        dto.setAiProvider(b.getAiProvider());
        dto.setAllowGeneralConversation(b.isAllowGeneralConversation());
        dto.setActive(b.isActive());
        return dto;
    }

    public void applyToEntity(BusinessProfile b) {
        b.setWhatsappNumber(this.whatsappNumber);
        b.setBusinessName(this.businessName);
        b.setIndustry(this.industry);
        b.setDescription(this.description);
        b.setContactNumber(this.contactNumber);
        b.setWebsite(this.website);
        b.setAddress(this.address);
        b.setWorkingHours(this.workingHours);
        b.setTimezone(this.timezone);
        b.setLanguages(this.languages);
        if (this.tone != null) {
            b.setTone(BusinessProfile.Tone.valueOf(this.tone.toUpperCase()));
        }
        b.setUseEmojis(this.useEmojis);
        b.setKnowledgeBase(this.knowledgeBase);
        b.setCustomInstructions(this.customInstructions);
        b.setAiProvider(this.aiProvider);
        b.setAllowGeneralConversation(this.allowGeneralConversation);
        b.setActive(this.active);
    }
}
