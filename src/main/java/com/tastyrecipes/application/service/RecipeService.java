package com.tastyrecipes.application.service;

import com.tastyrecipes.application.dto.RecipeDto;
import com.tastyrecipes.application.exception.ResourceNotFoundException;
import com.tastyrecipes.application.exception.UnauthorizedException;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class RecipeService {
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private UserService userService;

    @Autowired
    private S3Service s3Service;





    public Recipe findById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
    }

    public Page<Recipe> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable);
    }

    public Page<Recipe> searchRecipes(String keyword, Pageable pageable) {
        return recipeRepository.searchRecipes(keyword, pageable);
    }

    @Transactional
    public Recipe createRecipe(RecipeDto recipeDto, Long userId, MultipartFile image) throws IOException {
        User user = userService.findById(userId);

        Recipe recipe = new Recipe();
        recipe.setTitle(recipeDto.getTitle());
        recipe.setDescription(recipeDto.getDescription());
        recipe.setIngredients(recipeDto.getIngredients());
        recipe.setSteps(recipeDto.getSteps());
        recipe.setUser(user);

        if (image != null && !image.isEmpty()) {
            String imageUrl = s3Service.uploadImage(image);
            recipe.setImageUrl(imageUrl);
        }

        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe updateRecipe(Long recipeId, RecipeDto recipeDto, Long userId, MultipartFile image) throws IOException {
        Recipe recipe = findById(recipeId);

        if (!recipe.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own recipes");
        }

        recipe.setTitle(recipeDto.getTitle());
        recipe.setDescription(recipeDto.getDescription());
        recipe.setIngredients(recipeDto.getIngredients());
        recipe.setSteps(recipeDto.getSteps());

        if (image != null && !image.isEmpty()) {
            if (recipe.getImageUrl() != null) {
                s3Service.deleteImage(recipe.getImageUrl());
            }
            String imageUrl = s3Service.uploadImage(image);
            recipe.setImageUrl(imageUrl);
        }

        return recipeRepository.save(recipe);
    }

    @Transactional
    public void deleteRecipe(Long recipeId, Long userId) {
        Recipe recipe = findById(recipeId);

        if (!recipe.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own recipes");
        }

        // Delete image if exists
        if (recipe.getImageUrl() != null) {
            s3Service.deleteImage(recipe.getImageUrl());
        }

        recipeRepository.delete(recipe);
    }



    public RecipeDto convertToDto(Recipe recipe) {
        RecipeDto dto = new RecipeDto();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setIngredients(recipe.getIngredients());
        dto.setSteps(recipe.getSteps());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setUserId(recipe.getUser().getId());
        dto.setUserName(recipe.getUser().getName());
        dto.setCreatedAt(recipe.getCreatedAt());
        dto.setAverageRating(recipe.getAverageRating());
        dto.setReviewCount(recipe.getReviews().size());

        return dto;
    }
}