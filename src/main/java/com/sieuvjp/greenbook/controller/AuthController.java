package com.sieuvjp.greenbook.controller;

import com.sieuvjp.greenbook.dto.UserDTO;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.Role;
import com.sieuvjp.greenbook.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/admin";
        }

        return "pages/auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/admin";
        }

        model.addAttribute("user", new UserDTO());
        return "pages/auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserDTO userDTO,
                               BindingResult result,
                               Model model) {

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/auth/register";
        }

        // Check if username already exists
        if (userService.existsByUsername(userDTO.getUsername())) {
            result.rejectValue("username", "error.user", "Username is already taken");
            return "pages/auth/register";
        }

        // Check if email already exists
        if (userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.user", "Email is already in use");
            return "pages/auth/register";
        }

        // Set default role for new user
        userDTO.setRole(Role.CUSTOMER);
        userDTO.setActive(true);

        // Save user
        User user = userDTO.toEntity();
        userService.save(user);

        return "redirect:/login?registered";
    }
}