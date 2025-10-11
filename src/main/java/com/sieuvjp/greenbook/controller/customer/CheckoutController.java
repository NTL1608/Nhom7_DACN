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
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false, defaultValue = "1") Integer quantity,
            @RequestParam(required = false, defaultValue = "false") Boolean buyNow,
            HttpSession session,
            Model model,
            Authentication authentication
    ) {
        log.info("=== CHECKOUT PAGE - BuyNow: {}, BookId: {} ===", buyNow, bookId);

        // Kiểm tra đăng nhập
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login?redirect=/checkout";
        }

        // ✨ XỬ LÝ MUA NGAY
        if (buyNow != null && buyNow && bookId != null) {
            log.info("Processing BUY NOW for book ID: {}", bookId);

            try {
                // Lấy sách
                Book book = bookService.findById(bookId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

                // Kiểm tra tồn kho
                if (book.getStockQuantity() < quantity) {
                    return "redirect:/books?error=outofstock";
                }

                // Tạo CartItem tạm cho mua ngay
                CartItem buyNowItem = CartItem.builder()
                        .bookId(book.getId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .price(book.getOriginalPrice())
                        .quantity(quantity)
                        .stockQuantity(book.getStockQuantity())
                        .imageUrl(book.getImageUrls() != null && !book.getImageUrls().isEmpty()
                                ? book.getImageUrls().get(0) : null)
                        .build();

                List<CartItem> buyNowItems = List.of(buyNowItem);

                double totalAmount = book.getOriginalPrice() * quantity;
                double shippingFee = totalAmount >= 200000 ? 0.0 : 30000.0;
                double finalAmount = totalAmount + shippingFee;

                // Lấy user
                User currentUser = getUserFromAuthentication(authentication);
                if (currentUser == null) {
                    return "redirect:/login";
                }

                model.addAttribute("user", currentUser);
                model.addAttribute("cartItems", buyNowItems);
                model.addAttribute("totalAmount", totalAmount);
                model.addAttribute("shippingFee", shippingFee);
                model.addAttribute("finalAmount", finalAmount);
                model.addAttribute("isBuyNow", true);
                model.addAttribute("buyNowBookId", bookId);
                model.addAttribute("buyNowQuantity", quantity);
                model.addAttribute("title", "Thanh toán");

                addAuthInfoToModel(model, authentication);

                return "customer/checkout";

            } catch (Exception e) {
                log.error("Error in buy now checkout: ", e);
                return "redirect:/books?error=checkout";
            }
        }

        // ✅ XỬ LÝ CHECKOUT BÌNH THƯỜNG (TỪ GIỎ HÀNG)
        if (cartService.isEmpty(session)) {
            return "redirect:/cart";
        }

        User currentUser = getUserFromAuthentication(authentication);
        if (currentUser == null) {
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
        model.addAttribute("isBuyNow", false);
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
            @RequestParam(required = false, defaultValue = "false") Boolean isBuyNow,
            @RequestParam(required = false) Long buyNowBookId,
            @RequestParam(required = false, defaultValue = "1") Integer buyNowQuantity,
            HttpSession session,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== PLACE ORDER - IsBuyNow: {} ===", isBuyNow);

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            User currentUser = getUserFromAuthentication(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            List<CartItem> cartItems;
            double totalAmount;

            // ✨ XỬ LÝ MUA NGAY
            if (isBuyNow != null && isBuyNow && buyNowBookId != null) {
                log.info("Processing BUY NOW order for book: {}", buyNowBookId);

                Book book = bookService.findById(buyNowBookId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

                if (book.getStockQuantity() < buyNowQuantity) {
                    redirectAttributes.addFlashAttribute("error", "Sách không đủ số lượng!");
                    return "redirect:/books";
                }

                CartItem buyNowItem = CartItem.builder()
                        .bookId(book.getId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .price(book.getOriginalPrice())
                        .quantity(buyNowQuantity)
                        .stockQuantity(book.getStockQuantity())
                        .build();

                cartItems = List.of(buyNowItem);
                totalAmount = book.getOriginalPrice() * buyNowQuantity;

            } else {
                cartItems = cartService.getCart(session);
                if (cartItems.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống!");
                    return "redirect:/cart";
                }
                totalAmount = cartService.getTotalAmount(session);
            }

            double shippingFee = totalAmount >= 200000 ? 0.0 : 30000.0;
            double finalAmount = totalAmount + shippingFee;

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

            for (CartItem cartItem : cartItems) {
                Book book = bookService.findById(cartItem.getBookId())
                        .orElseThrow(() -> new RuntimeException("Sách không tồn tại!"));

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

                book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
                book.setSoldQuantity(book.getSoldQuantity() + cartItem.getQuantity());
                bookService.save(book);
            }

            Order savedOrder = orderService.save(order);

            // CHỈ XÓA GIỎ HÀNG NẾU KHÔNG PHẢI MUA NGAY
            if (isBuyNow == null || !isBuyNow) {
                cartService.clearCart(session);
            }

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

        Order order = orderService.getOrderWithDetails(orderId);

        model.addAttribute("order", order);
        model.addAttribute("title", "Đặt hàng thành công");

        addAuthInfoToModel(model, authentication);

        return "customer/order-confirmation";
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private User getUserFromAuthentication(Authentication authentication) {
        User currentUser = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            currentUser = userService.findByUsername(username).orElse(null);
        } else if (principal instanceof User) {
            currentUser = (User) principal;
        }

        return currentUser;
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