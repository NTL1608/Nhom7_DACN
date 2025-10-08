package com.sieuvjp.greenbook.dto;

import lombok.*;

import java.io.Serializable;

/**
 * CartItem - DTO cho sản phẩm trong giỏ hàng (lưu trong Session)
 * Không phải Entity, không lưu vào database
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookId;
    private String title;
    private String author;
    private Double price;
    private String imageUrl;
    private Integer quantity;
    private Integer stockQuantity; // Để check số lượng tồn kho

    /**
     * Tính tổng tiền của item này
     */
    public Double getSubtotal() {
        return price * quantity;
    }

    /**
     * Kiểm tra có thể tăng số lượng không
     */
    public boolean canIncreaseQuantity() {
        return quantity < stockQuantity;
    }

    /**
     * Tăng số lượng
     */
    public void increaseQuantity() {
        if (canIncreaseQuantity()) {
            this.quantity++;
        }
    }

    /**
     * Giảm số lượng
     */
    public void decreaseQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }
}