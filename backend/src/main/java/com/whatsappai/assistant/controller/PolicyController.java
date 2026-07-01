package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.dto.PolicyDto;
import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.entity.Policy;
import com.whatsappai.assistant.repository.PolicyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyRepository policyRepo;
    private final BusinessProfileController businessHelper;

    public PolicyController(PolicyRepository policyRepo, BusinessProfileController businessHelper) {
        this.policyRepo = policyRepo;
        this.businessHelper = businessHelper;
    }

    @GetMapping
    public List<PolicyDto> list(HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        return policyRepo.findByBusinessId(b.getId()).stream().map(p -> {
            PolicyDto dto = new PolicyDto();
            dto.setId(p.getId());
            dto.setTitle(p.getTitle());
            dto.setContent(p.getContent());
            return dto;
        }).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyDto create(@RequestBody PolicyDto dto, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        Policy p = new Policy();
        p.setTitle(dto.getTitle());
        p.setContent(dto.getContent());
        p.setBusiness(b);
        p = policyRepo.save(p);
        dto.setId(p.getId());
        return dto;
    }

    @PutMapping("/{id}")
    public PolicyDto update(@PathVariable String id, @RequestBody PolicyDto dto, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        Policy p = policyRepo.findById(id)
                .filter(pol -> pol.getBusiness().getId().equals(b.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setTitle(dto.getTitle());
        p.setContent(dto.getContent());
        policyRepo.save(p);
        dto.setId(id);
        return dto;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        Policy p = policyRepo.findById(id)
                .filter(pol -> pol.getBusiness().getId().equals(b.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        policyRepo.delete(p);
    }
}
