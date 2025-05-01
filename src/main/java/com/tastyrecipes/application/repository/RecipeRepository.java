package com.tastyrecipes.application.repository;

import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByUser(User user);

    Page<Recipe> findAll(Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE r.title LIKE %:keyword% OR r.description LIKE %:keyword% OR r.ingredients LIKE %:keyword%")
    Page<Recipe> searchRecipes(String keyword, Pageable pageable);
}
