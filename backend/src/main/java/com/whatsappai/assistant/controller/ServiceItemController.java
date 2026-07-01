package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.dto.ServiceItemDto;
import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.entity.ServiceItem;
import com.whatsappai.assistant.repository.ServiceItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceItemController {

    private final ServiceItemRepository serviceRepo;
    private final BusinessProfileController businessHelper;

    public ServiceItemController(ServiceItemRepository serviceRepo, BusinessProfileController businessHelper) {
        this.serviceRepo = serviceRepo;
        this.businessHelper = businessHelper;
    }

    @GetMapping
    public List<ServiceItemDto> list(HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        return serviceRepo.findByBusinessId(b.getId()).stream().map(s -> {
            ServiceItemDto dto = new ServiceItemDto();
            dto.setId(s.getId());
            dto.setName(s.getName());
            dto.setDescription(s.getDescription());
            dto.setPrice(s.getPrice());
            dto.setAvailable(s.isAvailable());
            return dto;
        }).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceItemDto create(@RequestBody ServiceItemDto dto, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        ServiceItem s = new ServiceItem();
        applyDto(dto, s);
        s.setBusiness(b);
        s = serviceRepo.save(s);
        dto.setId(s.getId());
        return dto;
    }

    @PutMapping("/{id}")
    public ServiceItemDto update(@PathVariable String id, @RequestBody ServiceItemDto dto, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        ServiceItem s = serviceRepo.findById(id)
                .filter(si -> si.getBusiness().getId().equals(b.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        applyDto(dto, s);
        serviceRepo.save(s);
        dto.setId(id);
        return dto;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        ServiceItem s = serviceRepo.findById(id)
                .filter(si -> si.getBusiness().getId().equals(b.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        serviceRepo.delete(s);
    }

    private void applyDto(ServiceItemDto dto, ServiceItem s) {
        s.setName(dto.getName());
        s.setDescription(dto.getDescription());
        s.setPrice(dto.getPrice());
        s.setAvailable(dto.isAvailable());
    }
}
