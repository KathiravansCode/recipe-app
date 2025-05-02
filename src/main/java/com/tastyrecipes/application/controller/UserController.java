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
            @Valid @RequestBody UserDto userDto,
            Authentication authentication) {

        User currentUser = userService.findByEmail(authentication.getName());

        // Only update allowed fields (name and email)
        if (userDto.getName() != null) {
            currentUser.setName(userDto.getName());
        }

        // If email is being changed, check if it's already in use
        if (userDto.getEmail() != null && !userDto.getEmail().equals(currentUser.getEmail())) {
            if (userService.existsByEmail(userDto.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Email already in use"));
            }
            currentUser.setEmail(userDto.getEmail());
        }

        // Save updated user
        User updatedUser = userService.updateUser(currentUser);
        UserDto updatedUserDto = userService.convertToDto(updatedUser);

        return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully", updatedUserDto));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestBody Map<String, String> passwordData,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());

        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        // Validate current password
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Current password and new password are required"));
        }

        // Check if current password is correct
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Current password is incorrect"));
        }

        // Validate new password (you may add more validation rules)
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "New password must be at least 6 characters long"));
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);

        return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
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

        // Only allow admin or self to delete account
        if (!currentUser.getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this user");
        }

        userService.deleteAccount(userId);

        return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
    }
}