package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final BookService bookService;

    /**
     * Hiển thị giỏ hàng
     */
    @GetMapping
    public String viewCart(
            HttpSession session,
            Model model,
            Authentication authentication
    ) {
        log.info("=== VIEW CART ===");

        addAuthInfoToModel(model, authentication);

        var cart = cartService.getCart(session);
        var totalAmount = cartService.getTotalAmount(session);
        var totalItems = cartService.getTotalItems(session);

        model.addAttribute("cartItems", cart);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("title", "Giỏ hàng");

        log.info("Cart has {} items, total: {}", cart.size(), totalAmount);

        return "customer/cart";
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") Integer quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== ADD TO CART - Book ID: {}, Quantity: {} ===", bookId, quantity);

        try {
            Book book = bookService.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách!"));

            if (book.getStockQuantity() < quantity) {
                redirectAttributes.addFlashAttribute("error", "Số lượng sách không đủ!");
                return "redirect:/books/" + bookId;
            }

            cartService.addToCart(session, book, quantity);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sách vào giỏ hàng!");

        } catch (Exception e) {
            log.error("Error adding to cart: ", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * Cập nhật số lượng sản phẩm
     */
    @PostMapping("/update")
    public String updateCart(
            @RequestParam Long bookId,
            @RequestParam Integer quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== UPDATE CART - Book ID: {}, New Quantity: {} ===", bookId, quantity);

        try {
            if (quantity <= 0) {
                cartService.removeFromCart(session, bookId);
                redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng!");
            } else {
                cartService.updateQuantity(session, bookId, quantity);
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật số lượng!");
            }
        } catch (Exception e) {
            log.error("Error updating cart: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra!");
        }

        return "redirect:/cart";
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam Long bookId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== REMOVE FROM CART - Book ID: {} ===", bookId);

        try {
            cartService.removeFromCart(session, bookId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng!");
        } catch (Exception e) {
            log.error("Error removing from cart: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra!");
        }

        return "redirect:/cart";
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    @PostMapping("/clear")
    public String clearCart(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        log.info("=== CLEAR CART ===");

        cartService.clearCart(session);
        redirectAttributes.addFlashAttribute("success", "Đã xóa toàn bộ giỏ hàng!");

        return "redirect:/cart";
    }

    // ========================================
    // HELPER METHOD
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
            } else {
                userName = authentication.getName();
            }
        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userName", userName);
    }
    /**
     * API thêm sản phẩm vào giỏ hàng (AJAX)
     */
    @PostMapping("/add/ajax")
    @ResponseBody
    public ResponseEntity<?> addToCartAjax(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") Integer quantity,
            HttpSession session
    ) {
        log.info("=== AJAX ADD TO CART - Book ID: {}, Quantity: {} ===", bookId, quantity);

        try {
            Book book = bookService.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách!"));

            if (book.getStockQuantity() < quantity) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Số lượng sách không đủ!"));
            }

            cartService.addToCart(session, book, quantity);
            int cartItemCount = cartService.getTotalItems(session);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm sách vào giỏ hàng!",
                    "cartItemCount", cartItemCount
            ));

        } catch (Exception e) {
            log.error("Error adding to cart: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}