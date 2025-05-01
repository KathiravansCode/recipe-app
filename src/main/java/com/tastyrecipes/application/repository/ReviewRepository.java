package com.tastyrecipes.application.repository;

import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRecipe(Recipe recipe);
    Optional<Review> findByUserAndRecipe(User user, Recipe recipe);
    boolean existsByUserAndRecipe(User user, Recipe recipe);
}