package com.sieuvjp.greenbook.entity;

import com.sieuvjp.greenbook.enums.OrderStatus;
import com.sieuvjp.greenbook.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "final_amount", nullable = false)
    private Double finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default  // ✅ THÊM ANNOTATION NÀY
    private List<OrderDetail> orderDetails = new ArrayList<>();

    // Helper method to add order detail
    public void addOrderDetail(OrderDetail orderDetail) {
        if (this.orderDetails == null) {  // ✅ THÊM KIỂM TRA NÀY
            this.orderDetails = new ArrayList<>();
        }
        orderDetails.add(orderDetail);
        orderDetail.setOrder(this);
    }

    // Helper method to remove order detail
    public void removeOrderDetail(OrderDetail orderDetail) {
        if (this.orderDetails != null) {
            orderDetails.remove(orderDetail);
            orderDetail.setOrder(null);
        }
    }

    // ✅ THÊM METHOD NÀY để đảm bảo discountAmount không null
    public Double getDiscountAmount() {
        return discountAmount != null ? discountAmount : 0.0;
    }
}