package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.enums.OrderStatus;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.OrderService;
import com.sieuvjp.greenbook.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final BookService bookService;
    private final UserService userService;

    @GetMapping({"/dashboard"})
    public String dashboard(Model model) {
        // Total revenue
        double totalRevenue = orderService.getTotalRevenue();

        // Revenue for current month
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime now = LocalDateTime.now();
        double monthlyRevenue = orderService.getRevenueBetween(startOfMonth, now);

        // Total orders
        long totalOrders = orderService.getOrderCountBetween(
                LocalDateTime.now().minusYears(10), // A long time ago
                LocalDateTime.now());

        // Pending orders
        int pendingOrders = orderService.findByStatus(OrderStatus.PENDING).size();

        // Low stock alerts
        int lowStockAlerts = bookService.findByLowStock(5).size();

        // Top selling books
        var topSellingBooks = orderService.getTopSellingBooks(5);

        // Monthly sales chart data
        var monthlySales = orderService.getMonthlySales();

        // Add data to model
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("lowStockAlerts", lowStockAlerts);
        model.addAttribute("topSellingBooks", topSellingBooks);
        model.addAttribute("monthlySales", monthlySales);

        return "pages/home/index";
    }
}