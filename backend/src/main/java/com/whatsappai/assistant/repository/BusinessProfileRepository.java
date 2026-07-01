package com.whatsappai.assistant.repository;

import com.whatsappai.assistant.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, String> {
    Optional<BusinessProfile> findByWhatsappNumber(String whatsappNumber);
}
