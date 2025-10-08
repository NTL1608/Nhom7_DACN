package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.OrderService;
import com.sieuvjp.greenbook.service.UserService;  // ✅ THÊM IMPORT
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;  // ✅ THÊM IMPORT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final UserService userService;  // ✅ THÊM DEPENDENCY

    /**
     * Xem chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public String viewOrderDetail(
            @PathVariable Long id,
            Model model,
            Authentication authentication
    ) {
        log.info("=== VIEW ORDER DETAIL - ID: {} ===", id);

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // ✅ LẤY USER TỪ DATABASE
        User currentUser = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        } else if (principal instanceof User) {
            currentUser = (User) principal;
        }

        if (currentUser == null) {
            return "redirect:/login";
        }

        // ✅ KEY FIX: Dùng getOrderWithDetails()
        Order order = orderService.getOrderWithDetails(id);

        // Kiểm tra quyền xem đơn hàng
        if (!order.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} tried to access order {} of user {}",
                    currentUser.getId(), id, order.getUser().getId());
            return "redirect:/profile?tab=orders&error=unauthorized";
        }

        model.addAttribute("order", order);
        model.addAttribute("title", "Chi tiết đơn hàng #" + id);

        addAuthInfoToModel(model, authentication);

        return "customer/order-detail";
    }

    // ========================================
    // HELPER METHODS
    // ========================================

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
            } else if (principal instanceof UserDetails) {
                userName = ((UserDetails) principal).getUsername();
            } else {
                userName = authentication.getName();
            }
        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userName", userName);
    }
}