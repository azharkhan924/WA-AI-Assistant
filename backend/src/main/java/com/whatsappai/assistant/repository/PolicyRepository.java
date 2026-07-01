package com.whatsappai.assistant.repository;

import com.whatsappai.assistant.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, String> {
    List<Policy> findByBusinessId(String businessId);
}
