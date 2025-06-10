package com.sieuvjp.greenbook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class HomeController {
    @GetMapping({"/", ""})
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            if (roles.contains("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (roles.contains("ROLE_LIBRARIAN")) {
                return "redirect:/admin/books";
            }
        }
        return "redirect:/login";
    }
}
