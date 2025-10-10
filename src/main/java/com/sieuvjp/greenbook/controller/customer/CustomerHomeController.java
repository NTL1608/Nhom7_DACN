package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.dto.BlogDTO;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.BlogService;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerHomeController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final BlogService blogService; // ✅ ĐÃ CÓ

    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication authentication) {
        log.info("=== HOME PAGE REQUEST ===");

        addAuthInfoToModel(model, authentication);

        // Lấy 8 sách mới nhất
        try {
            var latestBooks = bookService.findNewArrivals(8);
            log.info("Found {} books", latestBooks.size());
            model.addAttribute("latestBooks", latestBooks);
        } catch (Exception e) {
            log.error("Error loading books: ", e);
            model.addAttribute("latestBooks", java.util.Collections.emptyList());
        }

        // Lấy danh mục (giới hạn 8)
        try {
            var categories = categoryService.findAll();
            var limitedCategories = categories.stream()
                    .limit(8)
                    .toList();
            log.info("Found {} categories, showing {}", categories.size(), limitedCategories.size());
            model.addAttribute("categories", limitedCategories);
        } catch (Exception e) {
            log.error("Error loading categories: ", e);
            model.addAttribute("categories", java.util.Collections.emptyList());
        }

        // ✅ THÊM: Lấy 3 blog mới nhất
        try {
            List<BlogDTO> recentBlogs = blogService.findRecentPublishedBlogs(3).stream()
                    .map(BlogDTO::fromEntity)
                    .collect(Collectors.toList());
            log.info("Found {} recent blogs", recentBlogs.size());
            model.addAttribute("recentBlogs", recentBlogs);
        } catch (Exception e) {
            log.error("Error loading blogs: ", e);
            model.addAttribute("recentBlogs", java.util.Collections.emptyList());
        }

        model.addAttribute("title", "Trang chủ");

        log.info("=== RENDERING customer/index.html ===");
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

        log.info("isLoggedIn: {}, userName: {}", isLoggedIn, userName);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userName", userName);
    }
}