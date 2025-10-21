package com.sieuvjp.greenbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "original_price", nullable = false)
    private Double originalPrice;

    @Column(name = "discounted_price")
    private Double discountedPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    // Helper method to calculate subtotal
    @Transient
    public Double getSubtotal() {
        return discountedPrice != null ? discountedPrice * quantity : originalPrice * quantity;
    }


}