package com.whatsappai.assistant.repository;

import com.whatsappai.assistant.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, String> {
    List<ServiceItem> findByBusinessId(String businessId);
}
