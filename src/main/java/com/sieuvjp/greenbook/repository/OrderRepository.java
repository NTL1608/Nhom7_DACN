package com.sieuvjp.greenbook.repository;

import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    Double getTotalRevenue();

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.orderDate BETWEEN ?1 AND ?2")
    Double getRevenueBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN ?1 AND ?2")
    Long getOrderCountBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT MONTH(o.orderDate), YEAR(o.orderDate), SUM(o.finalAmount) FROM Order o WHERE o.status = 'COMPLETED' GROUP BY MONTH(o.orderDate), YEAR(o.orderDate) ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> getMonthlySales();

    // THÊM MỚI: Tìm ngày order mới nhất
    @Query("SELECT MAX(o.orderDate) FROM Order o")
    LocalDateTime findMaxOrderDate();

    // THÊM MỚI: Tìm ngày order cũ nhất
    @Query("SELECT MIN(o.orderDate) FROM Order o")
    LocalDateTime findMinOrderDate();

    // THÊM MỚI: Đếm số orders trong khoảng thời gian
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :start AND o.orderDate <= :end")
    long countOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}