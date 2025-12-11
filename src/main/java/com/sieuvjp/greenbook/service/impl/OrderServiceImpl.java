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
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@Slf4j
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

// ⭐ Update book stock and sold quantities - FIX BUG
        log.info("=== CẬP NHẬT KHO ===");
        for (OrderDetail detail : savedOrder.getOrderDetails()) {
            Long bookId = detail.getBook().getId();
            int quantityOrdered = detail.getQuantity();

            // ⭐ RELOAD book từ database để có giá trị chính xác
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách ID: " + bookId));

            int stockBefore = book.getStockQuantity();

            log.info("📚 Sách: {} | Kho trước: {} | Đặt: {} | Kho sau: {}",
                    book.getTitle(), stockBefore, quantityOrdered, stockBefore - quantityOrdered);

            // Kiểm tra đủ hàng
            if (stockBefore < quantityOrdered) {
                throw new RuntimeException("Sách '" + book.getTitle() +
                        "' không đủ hàng! Còn " + stockBefore + ", cần " + quantityOrdered);
            }

            book.setStockQuantity(stockBefore - quantityOrdered);
            book.setSoldQuantity(book.getSoldQuantity() + quantityOrdered);
            bookRepository.save(book);
            bookRepository.flush();
        }
        log.info("=== CẬP NHẬT KHO HOÀN TẤT ===");
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
        Map<String, Double> result = new LinkedHashMap<>(); // Giữ thứ tự

        // Tên tháng tiếng Việt
        String[] monthNames = {
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };

        // Chỉ lấy 6 tháng gần nhất
        int count = 0;
        for (Object[] row : monthlySales) {
            if (count >= 6) break; // Giới hạn 6 tháng
            
            int month = (int) row[0];
            int year = (int) row[1];
            Double amount = (Double) row[2];

            // Format: "Tháng 12/2024"
            String key = monthNames[month - 1] + "/" + year;
            result.put(key, amount != null ? amount : 0.0);
            count++;
        }

        // Nếu không có dữ liệu, tạo dữ liệu mẫu cho 6 tháng gần nhất
        if (result.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 5; i >= 0; i--) {
                LocalDateTime date = now.minusMonths(i);
                String key = monthNames[date.getMonthValue() - 1] + "/" + date.getYear();
                result.put(key, 0.0);
            }
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

    // ✅ COPY ĐOẠN NÀY VÀO CUỐI CLASS OrderServiceImpl (trước dấu })
    @Override
    public Order getOrderWithDetails(Long orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));
    }
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }

        // Chỉ cho phép yêu cầu hủy ở trạng thái PENDING hoặc PROCESSING
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái hiện tại!");
        }

        // ⭐ THAY ĐỔI: Không trừ kho ngay, chỉ chuyển sang CANCELLATION_REQUESTED
        order.setStatus(OrderStatus.CANCELLATION_REQUESTED);
        orderRepository.save(order);

    }
    // ⭐ THÊM METHOD MỚI - DUYỆT HỦY ĐỠN (CHO ADMIN/LIBRARIAN)
    @Override
    @Transactional
    public void approveCancellation(Long orderId) {
        log.info("========================================");
        log.info("⭐ DUYỆT HỦY ĐƠN HÀNG #{}", orderId);
        log.info("========================================");

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        log.info("📦 Trạng thái hiện tại: {}", order.getStatus());

        if (order.getStatus() != OrderStatus.CANCELLATION_REQUESTED) {
            throw new RuntimeException("Đơn hàng này không yêu cầu hủy!");
        }

        // HOÀN KHO TRƯỚC KHI ĐỔI TRẠNG THÁI
        log.info("=== BẮT ĐẦU HOÀN KHO ===");
        for (OrderDetail detail : order.getOrderDetails()) {
            Long bookId = detail.getBook().getId();
            int quantityToReturn = detail.getQuantity();

            // ⭐ RELOAD book từ database để có giá trị chính xác
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách ID: " + bookId));

            int stockBefore = book.getStockQuantity();
            int soldBefore = book.getSoldQuantity();

            log.info("📚 Sách: {} (ID: {})", book.getTitle(), bookId);
            log.info("   Kho TRƯỚC: {} | Hoàn: {} | Kho SAU: {}",
                    stockBefore, quantityToReturn, stockBefore + quantityToReturn);

            // Cập nhật kho
            book.setStockQuantity(stockBefore + quantityToReturn);
            book.setSoldQuantity(Math.max(0, soldBefore - quantityToReturn));

            bookRepository.save(book);
            bookRepository.flush();

            // Xác nhận lại
            Book verifyBook = bookRepository.findById(bookId).orElseThrow();
            log.info("   ✅ Đã lưu! Kho thực tế trong DB: {}", verifyBook.getStockQuantity());
        }

        // Đổi trạng thái SAU KHI hoàn kho
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("✅ Đã chuyển trạng thái sang CANCELLED");
        log.info("========================================");
        log.info("🎉 DUYỆT HỦY HOÀN TẤT!");
        log.info("========================================");
    }

    // ⭐ THÊM METHOD TỪ CHỐI HỦY (OPTIONAL)
    @Override
    @Transactional
    public void rejectCancellation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.CANCELLATION_REQUESTED) {
            throw new RuntimeException("Đơn hàng này không yêu cầu hủy!");
        }

        // Trả về trạng thái trước đó (PROCESSING)
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
    }



}