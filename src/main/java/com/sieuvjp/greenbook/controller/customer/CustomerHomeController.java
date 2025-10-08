package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // ← THÊM
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j  // ← THÊM
@Controller
@RequiredArgsConstructor
public class CustomerHomeController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication authentication) {
        log.info("=== HOME PAGE REQUEST ===");  // ← THÊM

        addAuthInfoToModel(model, authentication);

        // Lấy 8 sách mới nhất
        try {
            var latestBooks = bookService.findNewArrivals(8);
            log.info("Found {} books", latestBooks.size());  // ← THÊM
            model.addAttribute("latestBooks", latestBooks);
        } catch (Exception e) {
            log.error("Error loading books: ", e);  // ← THÊM
            model.addAttribute("latestBooks", java.util.Collections.emptyList());
        }

        // Lấy danh mục
        // Lấy 8 danh mục đầu tiên
        try {
            var categories = categoryService.findAll();
            var limitedCategories = categories.stream()
                    .limit(8)  // ← GIỚI HẠN 8 DANH MỤC
                    .toList();
            log.info("Found {} categories, showing {}", categories.size(), limitedCategories.size());
            model.addAttribute("categories", limitedCategories);
        } catch (Exception e) {
            log.error("Error loading categories: ", e);
            model.addAttribute("categories", java.util.Collections.emptyList());
        }

        model.addAttribute("title", "Trang chủ");

        log.info("=== RENDERING INDEX.HTML ===");  // ← THÊM
        return "customer/index";
    }

    private void addAuthInfoToModel(Model model, Authentication authentication) {
        boolean isLoggedIn = false;
        String userName = null;

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            isLoggedIn = true;

            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                userName = user.getFullName();
            } else {
                userName = authentication.getName();
            }
        }

        log.info("isLoggedIn: {}, userName: {}", isLoggedIn, userName);  // ← THÊM
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userName", userName);
    }
}