package com.tastyrecipes.application.dto;




import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class ReviewDto {
    private Long id;

    @NotNull(message = "Recipe ID is required")
    private Long recipeId;

    private Long userId;
    private String userName;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    private String comment;
    private LocalDateTime createdAt;

}