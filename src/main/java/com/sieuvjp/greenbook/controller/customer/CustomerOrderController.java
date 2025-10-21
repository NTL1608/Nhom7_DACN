package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.OrderService;
import com.sieuvjp.greenbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * Hiển thị danh sách đơn hàng của user
     */
    @GetMapping
    public String myOrders(
            Model model,
            Authentication authentication
    ) {
        log.info("=== MY ORDERS PAGE ===");

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Lấy user từ database
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

        // Lấy danh sách đơn hàng của user
        List<Order> orders = orderService.findByUserId(currentUser.getId());

        model.addAttribute("orders", orders);
        model.addAttribute("title", "Đơn hàng của tôi");
        addAuthInfoToModel(model, authentication);

        return "customer/my-orders";
    }

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

        // Lấy user từ database
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

        // Lấy order với details
        Order order = orderService.getOrderWithDetails(id);

        // Kiểm tra quyền xem đơn hàng
        if (!order.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} tried to access order {} of user {}",
                    currentUser.getId(), id, order.getUser().getId());
            return "redirect:/orders?error=unauthorized";
        }

        model.addAttribute("order", order);
        model.addAttribute("title", "Chi tiết đơn hàng #" + id);

        addAuthInfoToModel(model, authentication);

        return "customer/order-detail";
    }

    /**
     * Hủy đơn hàng
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== CANCEL ORDER - ID: {} ===", id);

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Lấy user
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

            // Hủy đơn hàng
            orderService.cancelOrder(id, currentUser.getId());

            redirectAttributes.addFlashAttribute("success", "Hủy đơn hàng thành công!");

        } catch (Exception e) {
            log.error("Error cancelling order: ", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders/" + id;
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