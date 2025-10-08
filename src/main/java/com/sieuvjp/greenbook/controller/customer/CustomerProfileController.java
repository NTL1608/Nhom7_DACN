package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.OrderService;
import com.sieuvjp.greenbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final UserService userService;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Trang profile chính
     */
    @GetMapping
    public String profile(
            @RequestParam(defaultValue = "info") String tab,
            Model model,
            Authentication authentication
    ) {
        log.info("=== PROFILE PAGE - Tab: {} ===", tab);

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("activeTab", tab);

        // Nếu tab là orders, lấy danh sách đơn hàng
        if ("orders".equals(tab)) {
            try {
                var orders = orderService.findByUserId(currentUser.getId());
                model.addAttribute("orders", orders);
                log.info("Found {} orders for user {}", orders.size(), currentUser.getId());
            } catch (Exception e) {
                log.error("Error loading orders: ", e);
                model.addAttribute("orders", java.util.Collections.emptyList());
            }
        }

        model.addAttribute("title", "Thông tin cá nhân");
        addAuthInfoToModel(model, authentication);

        return "customer/profile";
    }

    /**
     * Cập nhật thông tin cá nhân
     */
    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== UPDATE PROFILE REQUEST ===");

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            // Kiểm tra email đã tồn tại chưa (nếu đổi email)
            if (!currentUser.getEmail().equals(email)) {
                if (userService.existsByEmail(email)) {
                    redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng!");
                    return "redirect:/profile?tab=info";
                }
            }

            // Cập nhật thông tin
            currentUser.setFullName(fullName);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            currentUser.setAddress(address);
            userService.save(currentUser);

            log.info("Profile updated successfully for user: {}", currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");

        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra. Vui lòng thử lại!");
        }

        return "redirect:/profile?tab=info";
    }

    /**
     * Đổi mật khẩu
     */
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== CHANGE PASSWORD REQUEST ===");

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            // Validate mật khẩu cũ
            if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu cũ không đúng!");
                return "redirect:/profile?tab=password";
            }

            // Validate mật khẩu mới
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự!");
                return "redirect:/profile?tab=password";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
                return "redirect:/profile?tab=password";
            }

            // Cập nhật mật khẩu
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userService.save(currentUser);

            log.info("Password changed successfully for user: {}", currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");

        } catch (Exception e) {
            log.error("Error changing password: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra. Vui lòng thử lại!");
        }

        return "redirect:/profile?tab=password";
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        // Fallback: load from database by username
        String username = authentication.getName();
        return userService.findByUsername(username).orElse(null);
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

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userName", userName);
    }
}