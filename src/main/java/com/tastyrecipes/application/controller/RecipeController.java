package com.tastyrecipes.application.controller;

import com.tastyrecipes.application.dto.ApiResponse;
import com.tastyrecipes.application.dto.RecipeDto;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.service.RecipeService;
import com.tastyrecipes.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Recipe> recipePage = recipeService.findAll(pageable);

        List<RecipeDto> content = recipePage.getContent().stream()
                .map(recipeService::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(true, "Recipes retrieved successfully", content));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchRecipes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Recipe> recipePage = recipeService.searchRecipes(keyword, pageable);

        List<RecipeDto> content = recipePage.getContent().stream()
                .map(recipeService::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(true, "Search results", content));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRecipeById(@PathVariable Long id) {
        Recipe recipe = recipeService.findById(id);
        RecipeDto recipeDto = recipeService.convertToDto(recipe);

        return ResponseEntity.ok(new ApiResponse(true, "Recipe retrieved successfully", recipeDto));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> createRecipe(
            @Valid @RequestPart("recipe") RecipeDto recipeDto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) throws IOException {

        User user = userService.findByEmail(authentication.getName());
        Recipe recipe = recipeService.createRecipe(recipeDto, user.getId(), image);
        RecipeDto createdRecipe = recipeService.convertToDto(recipe);

        return new ResponseEntity<>(
                new ApiResponse(true, "Recipe created successfully", createdRecipe),
                HttpStatus.CREATED
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestPart("recipe") RecipeDto recipeDto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) throws IOException {

        User user = userService.findByEmail(authentication.getName());
        Recipe recipe = recipeService.updateRecipe(id, recipeDto, user.getId(), image);
        RecipeDto updatedRecipe = recipeService.convertToDto(recipe);

        return ResponseEntity.ok(new ApiResponse(true, "Recipe updated successfully", updatedRecipe));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRecipe(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());
        recipeService.deleteRecipe(id, user.getId());

        return ResponseEntity.ok(new ApiResponse(true, "Recipe deleted successfully"));
    }
}