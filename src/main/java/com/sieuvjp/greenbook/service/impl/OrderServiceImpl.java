package com.sieuvjp.greenbook.service.impl;

import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.entity.OrderDetail;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.OrderStatus;
import com.sieuvjp.greenbook.exception.ResourceNotFoundException;
import com.sieuvjp.greenbook.repository.BookRepository;
import com.sieuvjp.greenbook.repository.OrderDetailRepository;
import com.sieuvjp.greenbook.repository.OrderRepository;
import com.sieuvjp.greenbook.repository.UserRepository;
import com.sieuvjp.greenbook.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Page<Order> findAllPaginated(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional
    public Order save(Order order) {
        // Calculate total, discount, and final amounts
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            double totalAmount = order.getOrderDetails().stream()
                    .mapToDouble(od -> od.getOriginalPrice() * od.getQuantity())
                    .sum();

            double discountAmount = order.getOrderDetails().stream()
                    .mapToDouble(od -> {
                        if (od.getDiscountedPrice() != null) {
                            return (od.getOriginalPrice() - od.getDiscountedPrice()) * od.getQuantity();
                        }
                        return 0.0;
                    })
                    .sum();

            order.setTotalAmount(totalAmount);
            order.setDiscountAmount(discountAmount);
            order.setFinalAmount(totalAmount - discountAmount);
        }

        Order savedOrder = orderRepository.save(order);

        // Update book stock and sold quantities
        for (OrderDetail detail : savedOrder.getOrderDetails()) {
            Book book = detail.getBook();
            book.setStockQuantity(book.getStockQuantity() - detail.getQuantity());
            book.setSoldQuantity(book.getSoldQuantity() + detail.getQuantity());
            bookRepository.save(book);
        }

        return savedOrder;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return orderRepository.findByUser(user);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    public Page<Order> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return orderRepository.findByOrderDateBetween(start, end, pageable);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(status);

        if (status == OrderStatus.COMPLETED) {
            order.setCompletedDate(LocalDateTime.now());
        }

        orderRepository.save(order);
    }

    @Override
    public double getTotalRevenue() {
        Double revenue = orderRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public double getRevenueBetween(LocalDateTime start, LocalDateTime end) {
        Double revenue = orderRepository.getRevenueBetween(start, end);
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public long getOrderCountBetween(LocalDateTime start, LocalDateTime end) {
        Long count = orderRepository.getOrderCountBetween(start, end);
        return count != null ? count : 0L;
    }

    @Override
    public Map<String, Double> getMonthlySales() {
        List<Object[]> monthlySales = orderRepository.getMonthlySales();
        Map<String, Double> result = new HashMap<>();

        for (Object[] row : monthlySales) {
            int month = (int) row[0];
            int year = (int) row[1];
            Double amount = (Double) row[2];

            String key = Month.of(month).name() + " " + year;
            result.put(key, amount);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getTopSellingBooks(int limit) {
        List<Object[]> topBooks = orderDetailRepository.findMostSoldBooks(limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : topBooks) {
            Long bookId = (Long) row[0];
            Long quantity = (Long) row[1];

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

            Map<String, Object> bookData = new HashMap<>();
            bookData.put("id", book.getId());
            bookData.put("title", book.getTitle());
            bookData.put("author", book.getAuthor());
            bookData.put("soldQuantity", quantity);
            bookData.put("revenue", quantity * book.getOriginalPrice());

            result.add(bookData);
        }

        return result;
    }

    @Override
    public double getTotalDiscount() {
        Double discount = orderDetailRepository.getTotalDiscount();
        return discount != null ? discount : 0.0;
    }

}