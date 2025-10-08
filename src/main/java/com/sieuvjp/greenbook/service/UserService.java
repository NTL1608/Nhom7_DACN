package com.sieuvjp.greenbook.service;

import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAll();
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User save(User user);
    void deleteById(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByIsActive(boolean isActive);
    void updatePassword(Long userId, String newPassword);
    void toggleActiveStatus(Long userId);
    User updateProfile(Long userId, String fullName, String email, String phone, String address);
    void changePassword(Long userId, String oldPassword, String newPassword);
}