package com.sieuvjp.greenbook.dto;

import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^[0-9\\s\\-]{10,15}$", message = "Phone number should be valid")
    private String phone;

    private String address;

    private Role role;

    private boolean active;

    // Convert Entity to DTO
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    // Convert DTO to Entity
    public User toEntity() {
        User user = new User();
        user.setId(this.id);
        user.setUsername(this.username);
        if (this.password != null && !this.password.isEmpty()) {
            user.setPassword(this.password);
        }
        user.setEmail(this.email);
        user.setFullName(this.fullName);
        user.setPhone(this.phone);
        user.setAddress(this.address);
        user.setRole(this.role);
        user.setActive(this.active);
        return user;
    }
}