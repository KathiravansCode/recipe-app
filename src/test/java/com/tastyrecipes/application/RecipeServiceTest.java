package com.tastyrecipes.application;



import com.tastyrecipes.application.dto.RecipeDto;
import com.tastyrecipes.application.exception.ResourceNotFoundException;
import com.tastyrecipes.application.exception.UnauthorizedException;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.repository.RecipeRepository;
import com.tastyrecipes.application.service.RecipeService;
import com.tastyrecipes.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService Tests")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RecipeService recipeService;

    private User testUser;
    private User otherUser;
    private Recipe testRecipe;
    private RecipeDto testRecipeDto;
    private Review testReview;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Set upload dir via reflection (normally injected via @Value)
        ReflectionTestUtils.setField(recipeService, "uploadDir", "test-uploads");

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Chef Alice");
        testUser.setEmail("alice@example.com");
        testUser.setPassword("encoded");
        testUser.setRecipes(new ArrayList<>());
        testUser.setReviews(new ArrayList<>());

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setName("Chef Bob");
        otherUser.setEmail("bob@example.com");

        testReview = new Review();
        testReview.setId(1L);
        testReview.setRating(5);
        testReview.setComment("Delicious!");
        testReview.setUser(testUser);
        testReview.setCreatedAt(LocalDateTime.now());

        testRecipe = new Recipe();
        testRecipe.setId(1L);
        testRecipe.setTitle("Pasta Carbonara");
        testRecipe.setDescription("A classic Italian pasta");
        testRecipe.setIngredients("Pasta, Eggs, Bacon, Parmesan");
        testRecipe.setSteps("1. Cook pasta. 2. Mix eggs. 3. Combine.");
        testRecipe.setUser(testUser);
        testRecipe.setCreatedAt(LocalDateTime.now());
        testRecipe.setReviews(new ArrayList<>());

        testRecipeDto = new RecipeDto();
        testRecipeDto.setTitle("Pasta Carbonara");
        testRecipeDto.setDescription("A classic Italian pasta");
        testRecipeDto.setIngredients("Pasta, Eggs, Bacon, Parmesan");
        testRecipeDto.setSteps("1. Cook pasta. 2. Mix eggs. 3. Combine.");

        pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // =========================================================
    // findById
    // =========================================================
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return recipe when ID exists")
        void shouldReturnRecipeWhenIdExists() {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));

            Recipe result = recipeService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Pasta Carbonara");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowExceptionWhenIdNotFound() {
            given(recipeRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipe not found with id: 99");
        }

        @Test
        @DisplayName("Should call repository with correct ID")
        void shouldCallRepositoryWithCorrectId() {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));

            recipeService.findById(1L);

            then(recipeRepository).should(times(1)).findById(1L);
        }
    }

    // =========================================================
    // findAll
    // =========================================================
    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Should return paginated list of recipes")
        void shouldReturnPaginatedRecipes() {
            List<Recipe> recipes = Arrays.asList(testRecipe);
            Page<Recipe> page = new PageImpl<>(recipes, pageable, 1);
            given(recipeRepository.findAll(pageable)).willReturn(page);

            Page<Recipe> result = recipeService.findAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Pasta Carbonara");
        }

        @Test
        @DisplayName("Should return empty page when no recipes exist")
        void shouldReturnEmptyPageWhenNoRecipes() {
            Page<Recipe> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
            given(recipeRepository.findAll(pageable)).willReturn(emptyPage);

            Page<Recipe> result = recipeService.findAll(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return multiple recipes correctly")
        void shouldReturnMultipleRecipes() {
            Recipe recipe2 = new Recipe();
            recipe2.setId(2L);
            recipe2.setTitle("Risotto");
            recipe2.setUser(testUser);
            recipe2.setReviews(new ArrayList<>());

            Page<Recipe> page = new PageImpl<>(Arrays.asList(testRecipe, recipe2), pageable, 2);
            given(recipeRepository.findAll(pageable)).willReturn(page);

            Page<Recipe> result = recipeService.findAll(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    // =========================================================
    // searchRecipes
    // =========================================================
    @Nested
    @DisplayName("searchRecipes()")
    class SearchRecipes {

        @Test
        @DisplayName("Should return matching recipes for a keyword")
        void shouldReturnMatchingRecipesForKeyword() {
            Page<Recipe> page = new PageImpl<>(List.of(testRecipe), pageable, 1);
            given(recipeRepository.searchRecipes("Pasta", pageable)).willReturn(page);

            Page<Recipe> result = recipeService.searchRecipes("Pasta", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("Pasta");
        }

        @Test
        @DisplayName("Should return empty page when no recipes match keyword")
        void shouldReturnEmptyPageWhenNoMatch() {
            Page<Recipe> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
            given(recipeRepository.searchRecipes("NonExistent", pageable)).willReturn(emptyPage);

            Page<Recipe> result = recipeService.searchRecipes("NonExistent", pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should pass keyword to repository correctly")
        void shouldPassKeywordToRepository() {
            Page<Recipe> page = new PageImpl<>(new ArrayList<>());
            given(recipeRepository.searchRecipes(anyString(), any(Pageable.class))).willReturn(page);

            recipeService.searchRecipes("eggs", pageable);

            then(recipeRepository).should(times(1)).searchRecipes("eggs", pageable);
        }
    }

    // =========================================================
    // createRecipe
    // =========================================================
    @Nested
    @DisplayName("createRecipe()")
    class CreateRecipe {

        @Test
        @DisplayName("Should create recipe without image")
        void shouldCreateRecipeWithoutImage() throws IOException {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeRepository.save(any(Recipe.class))).willReturn(testRecipe);

            Recipe result = recipeService.createRecipe(testRecipeDto, 1L, null);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Pasta Carbonara");
            then(recipeRepository).should(times(1)).save(any(Recipe.class));
        }

        @Test
        @DisplayName("Should create recipe with empty MultipartFile (treated as no image)")
        void shouldCreateRecipeWithEmptyMultipartFile() throws IOException {
            MockMultipartFile emptyFile = new MockMultipartFile("image", new byte[0]);
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeRepository.save(any(Recipe.class))).willReturn(testRecipe);

            Recipe result = recipeService.createRecipe(testRecipeDto, 1L, emptyFile);

            assertThat(result).isNotNull();
            // No image URL should be set for an empty file
            ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
            then(recipeRepository).should().save(captor.capture());
            assertThat(captor.getValue().getImageUrl()).isNull();
        }

        @Test
        @DisplayName("Should set correct fields when creating recipe")
        void shouldSetCorrectFieldsWhenCreatingRecipe() throws IOException {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeRepository.save(any(Recipe.class))).willAnswer(inv -> inv.getArgument(0));

            Recipe result = recipeService.createRecipe(testRecipeDto, 1L, null);

            assertThat(result.getTitle()).isEqualTo("Pasta Carbonara");
            assertThat(result.getDescription()).isEqualTo("A classic Italian pasta");
            assertThat(result.getIngredients()).isEqualTo("Pasta, Eggs, Bacon, Parmesan");
            assertThat(result.getSteps()).isEqualTo("1. Cook pasta. 2. Mix eggs. 3. Combine.");
            assertThat(result.getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userService.findById(99L)).willThrow(new ResourceNotFoundException("User not found with id: 99"));

            assertThatThrownBy(() -> recipeService.createRecipe(testRecipeDto, 99L, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 99");

            then(recipeRepository).should(never()).save(any(Recipe.class));
        }

        @Test
        @DisplayName("Should create recipe with a valid image file")
        void shouldCreateRecipeWithValidImage() throws IOException {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "pasta.jpg", "image/jpeg", "fake-image-data".getBytes());

            given(userService.findById(1L)).willReturn(testUser);
            given(recipeRepository.save(any(Recipe.class))).willAnswer(inv -> {
                Recipe r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            Recipe result = recipeService.createRecipe(testRecipeDto, 1L, imageFile);

            assertThat(result).isNotNull();
            assertThat(result.getImageUrl()).isNotNull();
            assertThat(result.getImageUrl()).contains("pasta.jpg");
        }
    }

    // =========================================================
    // updateRecipe
    // =========================================================
    @Nested
    @DisplayName("updateRecipe()")
    class UpdateRecipe {

        @Test
        @DisplayName("Should update recipe when user is owner")
        void shouldUpdateRecipeWhenUserIsOwner() throws IOException {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeRepository.save(any(Recipe.class))).willReturn(testRecipe);

            RecipeDto updatedDto = new RecipeDto();
            updatedDto.setTitle("Updated Carbonara");
            updatedDto.setDescription("Updated description");
            updatedDto.setIngredients("Updated ingredients");
            updatedDto.setSteps("Updated steps");

            Recipe result = recipeService.updateRecipe(1L, updatedDto, 1L, null);

            assertThat(result).isNotNull();
            then(recipeRepository).should(times(1)).save(any(Recipe.class));
        }

        @Test
        @DisplayName("Should update all text fields of the recipe")
        void shouldUpdateAllTextFields() throws IOException {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeRepository.save(any(Recipe.class))).willAnswer(inv -> inv.getArgument(0));

            RecipeDto updatedDto = new RecipeDto();
            updatedDto.setTitle("New Title");
            updatedDto.setDescription("New Description");
            updatedDto.setIngredients("New Ingredients");
            updatedDto.setSteps("New Steps");

            Recipe result = recipeService.updateRecipe(1L, updatedDto, 1L, null);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getDescription()).isEqualTo("New Description");
            assertThat(result.getIngredients()).isEqualTo("New Ingredients");
            assertThat(result.getSteps()).isEqualTo("New Steps");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));

            assertThatThrownBy(() -> recipeService.updateRecipe(1L, testRecipeDto, 2L, null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You can only update your own recipes");

            then(recipeRepository).should(never()).save(any(Recipe.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe does not exist")
        void shouldThrowExceptionWhenRecipeNotFound() {
            given(recipeRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.updateRecipe(99L, testRecipeDto, 1L, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipe not found with id: 99");
        }

        @Test
        @DisplayName("Should update recipe image when a new image is provided")
        void shouldUpdateRecipeImageWhenNewImageProvided() throws IOException {
            testRecipe.setImageUrl(null); // No previous image
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeRepository.save(any(Recipe.class))).willAnswer(inv -> inv.getArgument(0));

            MockMultipartFile newImage = new MockMultipartFile(
                    "image", "new-image.jpg", "image/jpeg", "new-content".getBytes());

            Recipe result = recipeService.updateRecipe(1L, testRecipeDto, 1L, newImage);

            assertThat(result.getImageUrl()).isNotNull();
            assertThat(result.getImageUrl()).contains("new-image.jpg");
        }

        @Test
        @DisplayName("Should not update image when image is null")
        void shouldNotUpdateImageWhenImageIsNull() throws IOException {
            testRecipe.setImageUrl("existing-image.jpg");
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeRepository.save(any(Recipe.class))).willAnswer(inv -> inv.getArgument(0));

            Recipe result = recipeService.updateRecipe(1L, testRecipeDto, 1L, null);

            assertThat(result.getImageUrl()).isEqualTo("existing-image.jpg");
        }
    }

    // =========================================================
    // deleteRecipe
    // =========================================================
    @Nested
    @DisplayName("deleteRecipe()")
    class DeleteRecipe {

        @Test
        @DisplayName("Should delete recipe when user is the owner")
        void shouldDeleteRecipeWhenUserIsOwner() {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            willDoNothing().given(recipeRepository).delete(testRecipe);

            recipeService.deleteRecipe(1L, 1L);

            then(recipeRepository).should(times(1)).delete(testRecipe);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not the owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));

            assertThatThrownBy(() -> recipeService.deleteRecipe(1L, 2L))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You can only delete your own recipes");

            then(recipeRepository).should(never()).delete(any(Recipe.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe does not exist")
        void shouldThrowExceptionWhenRecipeNotFound() {
            given(recipeRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.deleteRecipe(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipe not found with id: 99");
        }

        @Test
        @DisplayName("Should delete recipe without image gracefully")
        void shouldDeleteRecipeWithoutImage() {
            testRecipe.setImageUrl(null);
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));

            assertThatCode(() -> recipeService.deleteRecipe(1L, 1L))
                    .doesNotThrowAnyException();

            then(recipeRepository).should(times(1)).delete(testRecipe);
        }
    }

    // =========================================================
    // convertToDto
    // =========================================================
    @Nested
    @DisplayName("convertToDto()")
    class ConvertToDto {

        @Test
        @DisplayName("Should map all fields from Recipe to RecipeDto correctly")
        void shouldMapAllFieldsCorrectly() {
            RecipeDto dto = recipeService.convertToDto(testRecipe);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitle()).isEqualTo("Pasta Carbonara");
            assertThat(dto.getDescription()).isEqualTo("A classic Italian pasta");
            assertThat(dto.getIngredients()).isEqualTo("Pasta, Eggs, Bacon, Parmesan");
            assertThat(dto.getSteps()).isEqualTo("1. Cook pasta. 2. Mix eggs. 3. Combine.");
            assertThat(dto.getUserId()).isEqualTo(1L);
            assertThat(dto.getUserName()).isEqualTo("Chef Alice");
        }

        @Test
        @DisplayName("Should return average rating of 0 when there are no reviews")
        void shouldReturnZeroAverageRatingWithNoReviews() {
            testRecipe.setReviews(new ArrayList<>());

            RecipeDto dto = recipeService.convertToDto(testRecipe);

            assertThat(dto.getAverageRating()).isEqualTo(0.0);
            assertThat(dto.getReviewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return correct average rating when reviews exist")
        void shouldReturnCorrectAverageRatingWithReviews() {
            Review review1 = new Review();
            review1.setRating(4);
            review1.setUser(testUser);
            review1.setRecipe(testRecipe);

            Review review2 = new Review();
            review2.setRating(2);
            review2.setUser(otherUser);
            review2.setRecipe(testRecipe);

            testRecipe.setReviews(Arrays.asList(review1, review2));

            RecipeDto dto = recipeService.convertToDto(testRecipe);

            assertThat(dto.getAverageRating()).isEqualTo(3.0);
            assertThat(dto.getReviewCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should include image URL when recipe has an image")
        void shouldIncludeImageUrlWhenRecipeHasImage() {
            testRecipe.setImageUrl("uploads/pasta.jpg");

            RecipeDto dto = recipeService.convertToDto(testRecipe);

            assertThat(dto.getImageUrl()).isEqualTo("uploads/pasta.jpg");
        }

        @Test
        @DisplayName("Should have null image URL when recipe has no image")
        void shouldHaveNullImageUrlWhenNoImage() {
            testRecipe.setImageUrl(null);

            RecipeDto dto = recipeService.convertToDto(testRecipe);

            assertThat(dto.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("Should include createdAt timestamp in DTO")
        void shouldIncludeCreatedAtTimestamp() {
            LocalDateTime now = LocalDateTime.now();
            testRecipe.setCreatedAt(now);

            RecipeDto dto = recipeService.convertToDto(testRecipe);

            assertThat(dto.getCreatedAt()).isEqualTo(now);
        }
    }
}
