package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.dto.FaqDto;
import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.entity.Faq;
import com.whatsappai.assistant.repository.FaqRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqRepository faqRepo;
    private final BusinessProfileController businessHelper;

    public FaqController(FaqRepository faqRepo, BusinessProfileController businessHelper) {
        this.faqRepo = faqRepo;
        this.businessHelper = businessHelper;
    }

    @GetMapping
    public List<FaqDto> list(HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        return faqRepo.findByBusinessId(b.getId()).stream().map(f -> {
            FaqDto dto = new FaqDto();
            dto.setId(f.getId());
            dto.setQuestion(f.getQuestion());
            dto.setAnswer(f.getAnswer());
            return dto;
        }).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaqDto create(@RequestBody FaqDto dto, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        Faq f = new Faq();
        f.setQuestion(dto.getQuestion());
        f.setAnswer(dto.getAnswer());
        f.setBusiness(b);
        f = faqRepo.save(f);
        dto.setId(f.getId());
        return dto;
    }

    @PutMapping("/{id}")
    public FaqDto update(@PathVariable String id, @RequestBody FaqDto dto, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        Faq f = faqRepo.findById(id)
                .filter(faq -> faq.getBusiness().getId().equals(b.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        f.setQuestion(dto.getQuestion());
        f.setAnswer(dto.getAnswer());
        faqRepo.save(f);
        dto.setId(id);
        return dto;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, HttpServletRequest req) {
        BusinessProfile b = businessHelper.loadOwnBusiness(req);
        Faq f = faqRepo.findById(id)
                .filter(faq -> faq.getBusiness().getId().equals(b.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        faqRepo.delete(f);
    }
}
