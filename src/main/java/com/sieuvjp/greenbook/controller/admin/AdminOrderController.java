package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.dto.OrderDTO;
import com.sieuvjp.greenbook.entity.Order;
import com.sieuvjp.greenbook.enums.OrderStatus;
import com.sieuvjp.greenbook.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage;

        if (startDate != null && endDate != null) {
            orderPage = orderService.findByDateRange(startDate, endDate, pageable);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        } else {
            orderPage = orderService.findAllPaginated(pageable);
        }

        List<OrderDTO> orders = orderPage.getContent().stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());

        // Filter by status if provided
        if (status != null) {
            orders = orders.stream()
                    .filter(order -> order.getStatus() == status)
                    .collect(Collectors.toList());
            model.addAttribute("status", status);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", orderPage.getNumber());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("orderStatuses", OrderStatus.values());

        return "pages/order/index";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + id));

        model.addAttribute("order", OrderDTO.fromEntity(order));
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "pages/order/view";
    }

    @GetMapping("/{id}/edit")
    public String editOrder(@PathVariable Long id, Model model) {
        // Redirect to view page because orders are managed through status updates
        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + id));

        model.addAttribute("order", OrderDTO.fromEntity(order));
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "pages/order/view";
    }

    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa đơn hàng: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/by-status/{status}")
    public String listOrdersByStatus(@PathVariable OrderStatus status, Model model) {
        List<OrderDTO> orders = orderService.findByStatus(status).stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "pages/order/index";
    }
    // ⭐ DUYỆT YÊU CẦU HỦY ĐƠN
    @PostMapping("/{id}/approve-cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // Cả 2 role
    public String approveCancellation(@PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            orderService.approveCancellation(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã duyệt hủy đơn hàng và hoàn kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ⭐ TỪ CHỐI YÊU CẦU HỦY (OPTIONAL)
    @PostMapping("/{id}/reject-cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String rejectCancellation(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            orderService.rejectCancellation(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã từ chối yêu cầu hủy đơn hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}