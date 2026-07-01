package com.whatsappai.assistant.repository;

import com.whatsappai.assistant.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, String> {
    List<Faq> findByBusinessId(String businessId);
}
