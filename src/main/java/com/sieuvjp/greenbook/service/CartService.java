package com.sieuvjp.greenbook.service;

import com.sieuvjp.greenbook.dto.CartItem;
import com.sieuvjp.greenbook.entity.Book;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CartService - Quản lý giỏ hàng trong Session
 */
@Slf4j
@Service
public class CartService {

    private static final String CART_SESSION_KEY = "SHOPPING_CART";

    /**
     * Lấy giỏ hàng từ session
     */
    public List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    /**
     * Thêm sách vào giỏ hàng
     */
    public void addToCart(HttpSession session, Book book, Integer quantity) {
        List<CartItem> cart = getCart(session);

        // Kiểm tra sách đã có trong giỏ chưa
        Optional<CartItem> existingItem = cart.stream()
                .filter(item -> item.getBookId().equals(book.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Nếu đã có, tăng số lượng
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // Check stock
            if (newQuantity <= book.getStockQuantity()) {
                item.setQuantity(newQuantity);
                log.info("Updated quantity for book {} to {}", book.getId(), newQuantity);
            } else {
                log.warn("Cannot add more. Stock limit reached for book {}", book.getId());
                throw new RuntimeException("Số lượng vượt quá tồn kho!");
            }
        } else {
            // Nếu chưa có, thêm mới
            CartItem newItem = CartItem.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .price(book.getOriginalPrice())
                    .imageUrl(book.getImageUrls() != null && !book.getImageUrls().isEmpty()
                            ? book.getImageUrls().get(0)
                            : null)
                    .quantity(quantity)
                    .stockQuantity(book.getStockQuantity())
                    .build();

            cart.add(newItem);
            log.info("Added new book {} to cart", book.getId());
        }

        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Cập nhật số lượng sản phẩm
     */
    public void updateQuantity(HttpSession session, Long bookId, Integer quantity) {
        List<CartItem> cart = getCart(session);

        cart.stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity > 0 && quantity <= item.getStockQuantity()) {
                        item.setQuantity(quantity);
                        log.info("Updated quantity for book {} to {}", bookId, quantity);
                    } else {
                        log.warn("Invalid quantity {} for book {}", quantity, bookId);
                    }
                });

        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    public void removeFromCart(HttpSession session, Long bookId) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(item -> item.getBookId().equals(bookId));
        session.setAttribute(CART_SESSION_KEY, cart);
        log.info("Removed book {} from cart", bookId);
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
        log.info("Cart cleared");
    }

    /**
     * Tính tổng số lượng sản phẩm trong giỏ
     */
    public int getTotalItems(HttpSession session) {
        List<CartItem> cart = getCart(session);
        return cart.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Tính tổng tiền giỏ hàng
     */
    public Double getTotalAmount(HttpSession session) {
        List<CartItem> cart = getCart(session);
        return cart.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }

    /**
     * Kiểm tra giỏ hàng có rỗng không
     */
    public boolean isEmpty(HttpSession session) {
        return getCart(session).isEmpty();
    }

    /**
     * Lấy số lượng sản phẩm
     */
    public int getCartSize(HttpSession session) {
        return getCart(session).size();
    }
}