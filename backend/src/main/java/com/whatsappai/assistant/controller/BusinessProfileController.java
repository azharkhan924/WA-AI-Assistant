package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.dto.BusinessProfileDto;
import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.repository.BusinessProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/business")
public class BusinessProfileController {

    private final BusinessProfileRepository repo;

    public BusinessProfileController(BusinessProfileRepository repo) {
        this.repo = repo;
    }

    /** GET own business profile */
    @GetMapping
    public BusinessProfileDto get(HttpServletRequest req) {
        return BusinessProfileDto.fromEntity(loadOwnBusiness(req));
    }

    /** PUT (full update) of business profile */
    @PutMapping
    public BusinessProfileDto update(@RequestBody BusinessProfileDto dto, HttpServletRequest req) {
        BusinessProfile b = loadOwnBusiness(req);
        dto.applyToEntity(b);
        return BusinessProfileDto.fromEntity(repo.save(b));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    BusinessProfile loadOwnBusiness(HttpServletRequest req) {
        String businessId = (String) req.getAttribute("businessId");
        if (businessId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return repo.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
    }
}
