package com.sieuvjp.greenbook.controller.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class ContactController {

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Liên hệ - Greenbook");
        return "customer/contact";
    }

    @PostMapping("/contact")
    public String submitContact(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {

        log.info("Contact form submitted - Name: {}, Email: {}, Subject: {}", name, email, subject);

        // TODO: Xử lý logic gửi email hoặc lưu database
        // Ví dụ: emailService.sendContactEmail(name, email, phone, subject, message);

        redirectAttributes.addFlashAttribute("successMessage",
                "Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi trong vòng 24h.");

        return "redirect:/contact";
    }
}