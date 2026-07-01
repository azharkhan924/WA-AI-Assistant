package com.whatsappai.assistant.config;

import com.whatsappai.assistant.entity.AdminUser;
import com.whatsappai.assistant.entity.BusinessProfile;
import com.whatsappai.assistant.repository.AdminUserRepository;
import com.whatsappai.assistant.repository.BusinessProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email}")
    private String defaultEmail;

    @Value("${app.admin.default-password}")
    private String defaultPassword;

    public DataInitializer(AdminUserRepository adminUserRepository,
                            BusinessProfileRepository businessProfileRepository,
                            PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() == 0) {
            BusinessProfile business = new BusinessProfile();
            business.setBusinessName("Greenwood International School");
            business.setIndustry("Education / K-12 School");
            business.setDescription("Premier Co-Educational K-12 Day School affiliated with CBSE & Cambridge curricula. Focused on holistic development, academic excellence, and modern STEM facilities.");
            business.setContactNumber("+91 98765 43210");
            business.setWebsite("https://www.greenwoodschool.edu");
            business.setAddress("Plot 45, Knowledge Park, Sector 62, Noida, UP 201301");
            business.setWorkingHours("Mon - Sat: 8:00 AM - 3:30 PM (Office hours)");
            business.setTimezone("Asia/Kolkata");
            business.setLanguages("English, Hindi, Hinglish");
            business.setTone(BusinessProfile.Tone.PROFESSIONAL);
            business.setAiProvider("gemini");
            business.setUseEmojis(true);
            business.setKnowledgeBase("Principal: Dr. Ananya Sharma. Founded: 2005. Student-teacher ratio: 15:1. Facilities: Olympic-size swimming pool, Robotics & AI Lab, Digital Library, Smart Classrooms, AC GPS-enabled buses.");
            business.setCustomInstructions("Always address parents politely. When asking about admissions, encourage scheduling a campus tour.");
            business = businessProfileRepository.save(business);

            AdminUser admin = new AdminUser();
            admin.setEmail(defaultEmail);
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            admin.setBusinessId(business.getId());
            adminUserRepository.save(admin);

            System.out.println("============================================================");
            System.out.println(" Seeded default admin login:");
            System.out.println(" Email:    " + defaultEmail);
            System.out.println(" Password: " + defaultPassword);
            System.out.println(" (change ADMIN_EMAIL / ADMIN_PASSWORD env vars before going live)");
            System.out.println("============================================================");
        }
    }
}
