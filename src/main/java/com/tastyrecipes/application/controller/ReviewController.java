package com.tastyrecipes.application.controller;

import com.tastyrecipes.application.dto.ApiResponse;
import com.tastyrecipes.application.dto.ReviewDto;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.service.ReviewService;
import com.tastyrecipes.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes/{recipeId}/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> getReviewsByRecipeId(@PathVariable Long recipeId) {
        List<Review> reviews = reviewService.findByRecipeId(recipeId);
        List<ReviewDto> reviewDtos = reviewService.convertToDtoList(reviews);

        return ResponseEntity.ok(new ApiResponse(true, "Reviews retrieved successfully", reviewDtos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createReview(
            @PathVariable Long recipeId,
            @RequestBody ReviewDto reviewDto,  // Remove @Valid here
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());
        reviewDto.setRecipeId(recipeId); // Set recipeId from path variable

        // Validate manually after setting recipeId
        Review review = reviewService.createReview(reviewDto, user.getId());
        ReviewDto createdReview = reviewService.convertToDto(review);

        return new ResponseEntity<>(
                new ApiResponse(true, "Review created successfully", createdReview),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> updateReview(
            @PathVariable Long recipeId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDto reviewDto,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());
        reviewDto.setRecipeId(recipeId); // Ensure recipeId from path is used

        Review review = reviewService.updateReview(reviewId, reviewDto, user.getId());
        ReviewDto updatedReview = reviewService.convertToDto(review);

        return ResponseEntity.ok(new ApiResponse(true, "Review updated successfully", updatedReview));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview(
            @PathVariable Long recipeId,
            @PathVariable Long reviewId,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());
        reviewService.deleteReview(reviewId, user.getId());

        return ResponseEntity.ok(new ApiResponse(true, "Review deleted successfully"));
    }
}