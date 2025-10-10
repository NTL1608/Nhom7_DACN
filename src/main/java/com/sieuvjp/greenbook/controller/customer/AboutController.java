package com.sieuvjp.greenbook.controller.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "Giới thiệu - Greenbook");
        return "customer/about";
    }
}