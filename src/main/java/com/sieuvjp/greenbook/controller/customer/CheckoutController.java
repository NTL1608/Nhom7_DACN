package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.dto.CartItem;
import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.entity.OrderDetail;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.OrderStatus;
import com.sieuvjp.greenbook.enums.PaymentMethod;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CartService;
import com.sieuvjp.greenbook.service.OrderService;
import com.sieuvjp.greenbook.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final BookService bookService;
    private final UserService userService;

    /**
     * Hiển thị trang checkout
     */
    @GetMapping
    public String showCheckout(
                                 HttpSession session,
                                 Model model,
                                 Authentication authentication
    ) {
        log.info("=== CHECKOUT PAGE ===");

        // Kiểm tra đăng nhập
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login?redirect=/checkout";
        }

        // Kiểm tra giỏ hàng
        if (cartService.isEmpty(session)) {
            return "redirect:/cart";
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

        // Kiểm tra NULL
        if (currentUser == null) {
            log.error("Cannot get current user!");
            return "redirect:/login";
        }

        log.info("Current user: {}", currentUser.getUsername());

        var cartItems = cartService.getCart(session);
        var totalAmount = cartService.getTotalAmount(session);
        var shippingFee = totalAmount >= 200000 ? 0.0 : 30000.0;
        var finalAmount = totalAmount + shippingFee;

        model.addAttribute("user", currentUser);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("finalAmount", finalAmount);
        model.addAttribute("title", "Thanh toán");

        addAuthInfoToModel(model, authentication);

        return "customer/checkout";
    }

    /**
     * Xử lý đặt hàng
     */
    @PostMapping("/place-order")
    public String placeOrder(
            @RequestParam String shippingAddress,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String note,
            HttpSession session,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== PLACE ORDER ===");

        try {
            // Kiểm tra đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Kiểm tra giỏ hàng
            List<CartItem> cartItems = cartService.getCart(session);
            if (cartItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống!");
                return "redirect:/cart";
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

            // Tính toán
            double totalAmount = cartService.getTotalAmount(session);
            double shippingFee = totalAmount >= 200000 ? 0.0 : 30000.0;
            double finalAmount = totalAmount + shippingFee;

            // Tạo Order
            Order order = Order.builder()
                    .user(currentUser)
                    .totalAmount(totalAmount)
                    .discountAmount(0.0)
                    .finalAmount(finalAmount)
                    .status(OrderStatus.PENDING)
                    .shippingAddress(shippingAddress)
                    .paymentMethod(PaymentMethod.valueOf(paymentMethod))
                    .note(note)
                    .orderDate(LocalDateTime.now())
                    .build();

            // Tạo OrderDetails từ CartItems
            for (CartItem cartItem : cartItems) {
                Book book = bookService.findById(cartItem.getBookId())
                        .orElseThrow(() -> new RuntimeException("Sách không tồn tại!"));

                // Kiểm tra tồn kho
                if (book.getStockQuantity() < cartItem.getQuantity()) {
                    redirectAttributes.addFlashAttribute("error",
                            "Sách '" + book.getTitle() + "' không đủ số lượng!");
                    return "redirect:/cart";
                }

                OrderDetail detail = OrderDetail.builder()
                        .book(book)
                        .quantity(cartItem.getQuantity())
                        .originalPrice(cartItem.getPrice())
                        .discountedPrice(null)
                        .promotion(null)
                        .build();

                order.addOrderDetail(detail);

                // Trừ tồn kho
                book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
                book.setSoldQuantity(book.getSoldQuantity() + cartItem.getQuantity());
                bookService.save(book);
            }

            // Lưu Order
            Order savedOrder = orderService.save(order);

            // Xóa giỏ hàng
            cartService.clearCart(session);

            log.info("Order created successfully: {}", savedOrder.getId());
            redirectAttributes.addFlashAttribute("orderId", savedOrder.getId());
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công!");

            return "redirect:/checkout/confirmation";

        } catch (Exception e) {
            log.error("Error placing order: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    /**
     * Trang xác nhận đơn hàng
     */
    @GetMapping("/confirmation")
    public String orderConfirmation(
            @ModelAttribute("orderId") Long orderId,
            Model model,
            Authentication authentication
    ) {
        log.info("=== ORDER CONFIRMATION - Order ID: {} ===", orderId);

        if (orderId == null) {
            return "redirect:/";
        }

        // ✅ KEY FIX: Dùng getOrderWithDetails()
        Order order = orderService.getOrderWithDetails(orderId);

        model.addAttribute("order", order);
        model.addAttribute("title", "Đặt hàng thành công");

        addAuthInfoToModel(model, authentication);

        return "customer/order-confirmation";
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