package com.tastyrecipes.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class RecipeDto {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Ingredients are required")
    private String ingredients;

    @NotBlank(message = "Steps are required")
    private String steps;

    private String imageUrl;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private Double averageRating;
    private Integer reviewCount;




}