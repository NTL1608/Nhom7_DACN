package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.dto.UserDTO;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.Role;
import com.sieuvjp.greenbook.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        List<UserDTO> users = userService.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        return "pages/user/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new UserDTO());
        model.addAttribute("roles", Role.values());
        return "pages/user/form";
    }

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("user") UserDTO userDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        model.addAttribute("roles", Role.values());

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/user/form";
        }

        // Check if username already exists
        if (userService.existsByUsername(userDTO.getUsername())) {
            result.rejectValue("username", "error.user", "Username is already taken");
            return "pages/user/form";
        }

        // Check if email already exists
        if (userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.user", "Email is already in use");
            return "pages/user/form";
        }

        // Save user
        User user = userDTO.toEntity();
        userService.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));

        model.addAttribute("user", UserDTO.fromEntity(user));
        model.addAttribute("roles", Role.values());
        return "pages/user/form";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") UserDTO userDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        model.addAttribute("roles", Role.values());

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/user/form";
        }

        // Get existing user
        User existingUser = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));

        // Check if username already exists (except for this user)
        if (!existingUser.getUsername().equals(userDTO.getUsername()) &&
                userService.existsByUsername(userDTO.getUsername())) {
            result.rejectValue("username", "error.user", "Username is already taken");
            return "pages/user/form";
        }

        // Check if email already exists (except for this user)
        if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
                userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.user", "Email is already in use");
            return "pages/user/form";
        }

        // Prepare user for update
        User user = userDTO.toEntity();

        // If password is empty, keep the existing password
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        }

        // Save user
        userService.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.toggleActiveStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully");
        return "redirect:/admin/users";
    }
}