package com.tastyrecipes.application.service;

import com.tastyrecipes.application.dto.ReviewDto;
import com.tastyrecipes.application.exception.ResourceNotFoundException;
import com.tastyrecipes.application.exception.UnauthorizedException;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private RecipeService recipeService;


    public List<Review> findByRecipeId(Long recipeId) {
        Recipe recipe = recipeService.findById(recipeId);
        return reviewRepository.findByRecipe(recipe);
    }

    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
    }

    @Transactional
    public Review createReview(ReviewDto reviewDto, Long userId) {
        User user = userService.findById(userId);
        Recipe recipe = recipeService.findById(reviewDto.getRecipeId());

        // Check if user already reviewed this recipe
        if (reviewRepository.existsByUserAndRecipe(user, recipe)) {
            throw new IllegalArgumentException("You have already reviewed this recipe");
        }

        Review review = new Review();
        review.setUser(user);
        review.setRecipe(recipe);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        return reviewRepository.save(review);
    }

    @Transactional
    public Review updateReview(Long reviewId, ReviewDto reviewDto, Long userId) {
        Review review = findById(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own reviews");
        }

        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = findById(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    public List<ReviewDto> convertToDtoList(List<Review> reviews) {
        return reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ReviewDto convertToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRecipeId(review.getRecipe().getId());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());

        return dto;
    }
}