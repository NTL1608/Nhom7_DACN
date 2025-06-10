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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
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

    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {

        orderService.updateOrderStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Order deleted successfully");
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
}