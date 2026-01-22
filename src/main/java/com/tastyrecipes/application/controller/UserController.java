package com.tastyrecipes.application.controller;

import com.tastyrecipes.application.dto.ApiResponse;
import com.tastyrecipes.application.dto.RecipeDto;
import com.tastyrecipes.application.dto.ReviewDto;
import com.tastyrecipes.application.dto.UserDto;
import com.tastyrecipes.application.exception.UnauthorizedException;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.service.RecipeService;
import com.tastyrecipes.application.service.ReviewService;
import com.tastyrecipes.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getUserProfile(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        UserDto userDto = userService.convertToDto(user);

        return ResponseEntity.ok(new ApiResponse(true, "User profile retrieved successfully", userDto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        User user = userService.findById(userId);
        UserDto userDto = userService.convertToDto(user);

        return ResponseEntity.ok(new ApiResponse(true, "User retrieved successfully", userDto));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            @RequestBody UserDto userDto,
            Authentication authentication) {

        User currentUser = userService.findByEmail(authentication.getName());

        // Validate and update name if provided
        if (userDto.getName() != null && !userDto.getName().trim().isEmpty()) {
            if (userDto.getName().trim().length() < 2) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Name must be at least 2 characters long"));
            }
            currentUser.setName(userDto.getName().trim());
        }

        // Validate and update email if provided
        if (userDto.getEmail() != null && !userDto.getEmail().trim().isEmpty()) {
            String newEmail = userDto.getEmail().trim();

            // Check if email is different from current
            if (!newEmail.equals(currentUser.getEmail())) {
                // Validate email format
                if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Invalid email format"));
                }

                // Check if email is already in use
                if (userService.existsByEmail(newEmail)) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Email already in use"));
                }

                currentUser.setEmail(newEmail);
            }
        }

        try {
            // Save updated user
            User updatedUser = userService.updateUser(currentUser);
            UserDto updatedUserDto = userService.convertToDto(updatedUser);

            return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully", updatedUserDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to update profile: " + e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestBody Map<String, String> passwordData,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());

        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        // Validate passwords are provided
        if (currentPassword == null || currentPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Current password and new password are required"));
        }

        // Check if current password is correct
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Current password is incorrect"));
        }

        // Validate new password length
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "New password must be at least 6 characters long"));
        }

        try {
            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);

            return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to change password: " + e.getMessage()));
        }
    }

    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse> getUserRecipes(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<Recipe> recipes = user.getRecipes();

        List<RecipeDto> recipeDtos = recipes.stream()
                .map(recipeService::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(true, "User recipes retrieved successfully", recipeDtos));
    }

    @GetMapping("/{userId}/recipes")
    public ResponseEntity<ApiResponse> getUserRecipesById(@PathVariable Long userId) {
        User user = userService.findById(userId);
        List<Recipe> recipes = user.getRecipes();

        List<RecipeDto> recipeDtos = recipes.stream()
                .map(recipeService::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(true, "User recipes retrieved successfully", recipeDtos));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse> getUserReviews(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<Review> reviews = user.getReviews();

        List<ReviewDto> reviewDtos = reviews.stream()
                .map(reviewService::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(true, "User reviews retrieved successfully", reviewDtos));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {

        User currentUser = userService.findByEmail(authentication.getName());

        // Only allow user to delete their own account
        if (!currentUser.getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this user");
        }

        try {
            userService.deleteAccount(userId);
            return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to delete account: " + e.getMessage()));
        }
    }
}