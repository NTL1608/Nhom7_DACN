package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.dto.CategoryDTO;
import com.sieuvjp.greenbook.entity.Category;
import com.sieuvjp.greenbook.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/suggest-description")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public ResponseEntity<Map<String, String>> suggestCategoryDescription(@RequestParam String name) {
        try {
            Client client = new Client();

            String prompt = "Viết một đoạn mô tả ngắn và thu hút cho thể loại sách có tên là: \"" + name + "\". Đoạn mô tả nên giải thích ngắn gọn thể loại này là gì và tại sao người đọc nên quan tâm.";

            GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash", prompt, null);

            return ResponseEntity.ok(Map.of("description", response.text()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("description", "Lỗi AI: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String listCategories(Model model) {
        List<CategoryDTO> categories = categoryService.findAll().stream()
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("categories", categories);
        return "pages/category/index";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')") // ⭐ CHỈ ADMIN
    public String showCreateForm(Model model) {
        model.addAttribute("category", new CategoryDTO());
        return "pages/category/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')") // ⭐ CHỈ ADMIN
    public String createCategory(@Valid @ModelAttribute("category") CategoryDTO categoryDTO,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "pages/category/form";
        }

        if (categoryService.existsByName(categoryDTO.getName())) {
            result.rejectValue("name", "error.category", "Category name already exists");
            return "pages/category/form";
        }

        categoryDTO.setActive(true);

        Category category = categoryDTO.toEntity();
        categoryService.save(category);

        redirectAttributes.addFlashAttribute("successMessage", "Category created successfully");
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + id));

        model.addAttribute("category", CategoryDTO.fromEntity(category));
        return "pages/category/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") CategoryDTO categoryDTO,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication) { // ⭐ THÊM Authentication

        if (result.hasErrors()) {
            return "pages/category/form";
        }

        Category existingCategory = categoryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + id));

        if (!existingCategory.getName().equals(categoryDTO.getName()) &&
                categoryService.existsByName(categoryDTO.getName())) {
            result.rejectValue("name", "error.category", "Category name already exists");
            return "pages/category/form";
        }

        // ⭐ LIBRARIAN KHÔNG ĐƯỢC THAY ĐỔI TRẠNG THÁI
        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_LIBRARIAN"));

        categoryDTO.setId(id);

        if (isLibrarian) {
            // Giữ nguyên trạng thái cũ
            categoryDTO.setActive(existingCategory.isActive());
            System.out.println("🔒 LIBRARIAN không được đổi trạng thái - Giữ nguyên");
        }

        Category category = categoryDTO.toEntity();
        categoryService.save(category);

        redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully");
        return "redirect:/admin/categories";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ⭐ CHỈ ADMIN
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully");
        return "redirect:/admin/categories";
    }

    @GetMapping("/toggle-status/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ⭐ CHỈ ADMIN
    public String toggleCategoryStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.toggleActiveStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Category status updated successfully");
        return "redirect:/admin/categories";
    }
}