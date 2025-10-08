package com.sieuvjp.greenbook.config;

import com.sieuvjp.greenbook.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor tự động thêm số lượng giỏ hàng vào mọi request
 */
@Component
@RequiredArgsConstructor
public class CartInterceptor implements HandlerInterceptor {

    private final CartService cartService;

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView
    ) throws Exception {

        if (modelAndView != null) {
            // Thêm số lượng sản phẩm trong giỏ hàng
            int cartItemCount = cartService.getTotalItems(request.getSession());
            modelAndView.addObject("cartItemCount", cartItemCount);
        }
    }
}