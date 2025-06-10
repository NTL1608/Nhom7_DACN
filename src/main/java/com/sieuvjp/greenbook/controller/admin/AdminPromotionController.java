package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.dto.PromotionDTO;
import com.sieuvjp.greenbook.entity.Promotion;
import com.sieuvjp.greenbook.enums.PromotionType;
import com.sieuvjp.greenbook.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public String listPromotions(Model model) {
        List<PromotionDTO> activePromotions = promotionService.findActivePromotions().stream()
                .map(PromotionDTO::fromEntity)
                .collect(Collectors.toList());

        List<PromotionDTO> upcomingPromotions = promotionService.findUpcomingPromotions().stream()
                .map(PromotionDTO::fromEntity)
                .collect(Collectors.toList());

        List<PromotionDTO> expiredPromotions = promotionService.findExpiredPromotions().stream()
                .map(PromotionDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("activePromotions", activePromotions);
        model.addAttribute("upcomingPromotions", upcomingPromotions);
        model.addAttribute("expiredPromotions", expiredPromotions);
        return "pages/promotion/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        PromotionDTO promotion = new PromotionDTO();
        promotion.setStartDate(LocalDate.now());
        promotion.setEndDate(LocalDate.now().plusDays(30));
        promotion.setActive(true);

        model.addAttribute("promotion", promotion);
        model.addAttribute("promotionTypes", PromotionType.values());
        return "pages/promotion/form";
    }

    @PostMapping("/create")
    public String createPromotion(@Valid @ModelAttribute("promotion") PromotionDTO promotionDTO,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        model.addAttribute("promotionTypes", PromotionType.values());

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/promotion/form";
        }

        // Check if promotion code already exists
        if (promotionService.existsByCode(promotionDTO.getCode())) {
            result.rejectValue("code", "error.promotion", "Promotion code already exists");
            return "pages/promotion/form";
        }

        // Check if start date is after end date
        if (promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
            result.rejectValue("startDate", "error.promotion", "Start date cannot be after end date");
            return "pages/promotion/form";
        }

        // Save promotion
        Promotion promotion = promotionDTO.toEntity();
        promotionService.save(promotion);

        redirectAttributes.addFlashAttribute("successMessage", "Promotion created successfully");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Promotion promotion = promotionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid promotion ID: " + id));

        model.addAttribute("promotion", PromotionDTO.fromEntity(promotion));
        model.addAttribute("promotionTypes", PromotionType.values());
        return "pages/promotion/form";
    }

    @PostMapping("/edit/{id}")
    public String updatePromotion(@PathVariable Long id,
                                  @Valid @ModelAttribute("promotion") PromotionDTO promotionDTO,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        model.addAttribute("promotionTypes", PromotionType.values());

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/promotion/form";
        }

        // Get existing promotion
        Promotion existingPromotion = promotionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid promotion ID: " + id));

        // Check if promotion code already exists (except for this promotion)
        if (!existingPromotion.getCode().equals(promotionDTO.getCode()) &&
                promotionService.existsByCode(promotionDTO.getCode())) {
            result.rejectValue("code", "error.promotion", "Promotion code already exists");
            return "pages/promotion/form";
        }

        // Check if start date is after end date
        if (promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
            result.rejectValue("startDate", "error.promotion", "Start date cannot be after end date");
            return "pages/promotion/form";
        }

        // Save promotion
        promotionDTO.setId(id);
        Promotion promotion = promotionDTO.toEntity();
        promotionService.save(promotion);

        redirectAttributes.addFlashAttribute("successMessage", "Promotion updated successfully");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        promotionService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Promotion deleted successfully");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/toggle-status/{id}")
    public String togglePromotionStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        promotionService.toggleActiveStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Promotion status updated successfully");
        return "redirect:/admin/promotions";
    }
}