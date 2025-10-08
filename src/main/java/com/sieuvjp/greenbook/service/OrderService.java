package com.sieuvjp.greenbook.service;

import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.enums.OrderStatus;
import com.sieuvjp.greenbook.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    List<Order> findAll();
    Page<Order> findAllPaginated(Pageable pageable);
    Optional<Order> findById(Long id);
    Order save(Order order);
    void deleteById(Long id);
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    Page<Order> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable);
    void updateOrderStatus(Long orderId, OrderStatus status);
    double getTotalRevenue();
    double getRevenueBetween(LocalDateTime start, LocalDateTime end);
    long getOrderCountBetween(LocalDateTime start, LocalDateTime end);
    Map<String, Double> getMonthlySales();
    List<Map<String, Object>> getTopSellingBooks(int limit);
    double getTotalDiscount();

    // ✅ THÊM 2 METHOD MỚI ĐÂY
    Order getOrderWithDetails(Long orderId);
}