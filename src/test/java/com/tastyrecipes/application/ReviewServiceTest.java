package com.tastyrecipes.application;



import com.tastyrecipes.application.dto.ReviewDto;
import com.tastyrecipes.application.exception.ResourceNotFoundException;
import com.tastyrecipes.application.exception.UnauthorizedException;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.repository.ReviewRepository;
import com.tastyrecipes.application.service.RecipeService;
import com.tastyrecipes.application.service.ReviewService;
import com.tastyrecipes.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserService userService;

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private User otherUser;
    private Recipe testRecipe;
    private Review testReview;
    private ReviewDto testReviewDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Alice");
        testUser.setEmail("alice@example.com");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setName("Bob");
        otherUser.setEmail("bob@example.com");

        testRecipe = new Recipe();
        testRecipe.setId(10L);
        testRecipe.setTitle("Spaghetti Bolognese");
        testRecipe.setUser(testUser);
        testRecipe.setReviews(new ArrayList<>());

        testReview = new Review();
        testReview.setId(100L);
        testReview.setUser(testUser);
        testReview.setRecipe(testRecipe);
        testReview.setRating(4);
        testReview.setComment("Very tasty!");
        testReview.setCreatedAt(LocalDateTime.now());

        testReviewDto = new ReviewDto();
        testReviewDto.setRecipeId(10L);
        testReviewDto.setRating(4);
        testReviewDto.setComment("Very tasty!");
    }

    // =========================================================
    // findByRecipeId
    // =========================================================
    @Nested
    @DisplayName("findByRecipeId()")
    class FindByRecipeId {

        @Test
        @DisplayName("Should return list of reviews for an existing recipe")
        void shouldReturnReviewsForExistingRecipe() {
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.findByRecipe(testRecipe)).willReturn(List.of(testReview));

            List<Review> result = reviewService.findByRecipeId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRating()).isEqualTo(4);
            assertThat(result.get(0).getComment()).isEqualTo("Very tasty!");
        }

        @Test
        @DisplayName("Should return empty list when recipe has no reviews")
        void shouldReturnEmptyListWhenNoReviews() {
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.findByRecipe(testRecipe)).willReturn(new ArrayList<>());

            List<Review> result = reviewService.findByRecipeId(10L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return multiple reviews for a recipe")
        void shouldReturnMultipleReviews() {
            Review review2 = new Review();
            review2.setId(101L);
            review2.setUser(otherUser);
            review2.setRecipe(testRecipe);
            review2.setRating(5);
            review2.setComment("Excellent!");

            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.findByRecipe(testRecipe)).willReturn(Arrays.asList(testReview, review2));

            List<Review> result = reviewService.findByRecipeId(10L);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe does not exist")
        void shouldThrowExceptionWhenRecipeNotFound() {
            given(recipeService.findById(99L)).willThrow(new ResourceNotFoundException("Recipe not found with id: 99"));

            assertThatThrownBy(() -> reviewService.findByRecipeId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipe not found with id: 99");

            then(reviewRepository).should(never()).findByRecipe(any());
        }
    }

    // =========================================================
    // findById
    // =========================================================
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return review when ID exists")
        void shouldReturnReviewWhenIdExists() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));

            Review result = reviewService.findById(100L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review ID does not exist")
        void shouldThrowExceptionWhenIdNotFound() {
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.findById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found with id: 999");
        }
    }

    // =========================================================
    // createReview
    // =========================================================
    @Nested
    @DisplayName("createReview()")
    class CreateReview {

        @Test
        @DisplayName("Should create review successfully when user has not reviewed the recipe yet")
        void shouldCreateReviewSuccessfully() {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.existsByUserAndRecipe(testUser, testRecipe)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(testReview);

            Review result = reviewService.createReview(testReviewDto, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(4);
            assertThat(result.getComment()).isEqualTo("Very tasty!");
        }

        @Test
        @DisplayName("Should set correct user, recipe, rating and comment on new review")
        void shouldSetCorrectFieldsOnNewReview() {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.existsByUserAndRecipe(testUser, testRecipe)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            Review result = reviewService.createReview(testReviewDto, 1L);

            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getRecipe()).isEqualTo(testRecipe);
            assertThat(result.getRating()).isEqualTo(4);
            assertThat(result.getComment()).isEqualTo("Very tasty!");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user has already reviewed the recipe")
        void shouldThrowExceptionWhenUserAlreadyReviewed() {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.existsByUserAndRecipe(testUser, testRecipe)).willReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(testReviewDto, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You have already reviewed this recipe");

            then(reviewRepository).should(never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not exist")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userService.findById(99L)).willThrow(new ResourceNotFoundException("User not found with id: 99"));

            assertThatThrownBy(() -> reviewService.createReview(testReviewDto, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 99");

            then(reviewRepository).should(never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe does not exist")
        void shouldThrowExceptionWhenRecipeNotFound() {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willThrow(new ResourceNotFoundException("Recipe not found with id: 10"));

            assertThatThrownBy(() -> reviewService.createReview(testReviewDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipe not found with id: 10");

            then(reviewRepository).should(never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should create review with null comment (comment is optional)")
        void shouldCreateReviewWithNullComment() {
            testReviewDto.setComment(null);
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.existsByUserAndRecipe(testUser, testRecipe)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            Review result = reviewService.createReview(testReviewDto, 1L);

            assertThat(result.getComment()).isNull();
        }

        @ParameterizedTest(name = "Should accept rating = {0}")
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @DisplayName("Should create review for all valid rating values")
        void shouldCreateReviewForAllValidRatings(int rating) {
            testReviewDto.setRating(rating);
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.existsByUserAndRecipe(testUser, testRecipe)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> {
                Review r = inv.getArgument(0);
                return r;
            });

            Review result = reviewService.createReview(testReviewDto, 1L);

            assertThat(result.getRating()).isEqualTo(rating);
        }

        @Test
        @DisplayName("Should save review to repository")
        void shouldSaveReviewToRepository() {
            given(userService.findById(1L)).willReturn(testUser);
            given(recipeService.findById(10L)).willReturn(testRecipe);
            given(reviewRepository.existsByUserAndRecipe(testUser, testRecipe)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(testReview);

            reviewService.createReview(testReviewDto, 1L);

            then(reviewRepository).should(times(1)).save(any(Review.class));
        }
    }

    // =========================================================
    // updateReview
    // =========================================================
    @Nested
    @DisplayName("updateReview()")
    class UpdateReview {

        @Test
        @DisplayName("Should update review successfully when user is the owner")
        void shouldUpdateReviewSuccessfully() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));
            given(reviewRepository.save(any(Review.class))).willReturn(testReview);

            testReviewDto.setRating(5);
            testReviewDto.setComment("Even better!");

            Review result = reviewService.updateReview(100L, testReviewDto, 1L);

            assertThat(result).isNotNull();
            then(reviewRepository).should(times(1)).save(any(Review.class));
        }

        @Test
        @DisplayName("Should update rating and comment on existing review")
        void shouldUpdateRatingAndCommentOnExistingReview() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            testReviewDto.setRating(2);
            testReviewDto.setComment("Not great after all");

            Review result = reviewService.updateReview(100L, testReviewDto, 1L);

            assertThat(result.getRating()).isEqualTo(2);
            assertThat(result.getComment()).isEqualTo("Not great after all");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not the review owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));

            assertThatThrownBy(() -> reviewService.updateReview(100L, testReviewDto, 2L))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You can only update your own reviews");

            then(reviewRepository).should(never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review ID does not exist")
        void shouldThrowExceptionWhenReviewNotFound() {
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview(999L, testReviewDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found with id: 999");
        }

        @Test
        @DisplayName("Should allow updating comment to null")
        void shouldAllowUpdatingCommentToNull() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));
            given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

            testReviewDto.setComment(null);
            testReviewDto.setRating(3);

            Review result = reviewService.updateReview(100L, testReviewDto, 1L);

            assertThat(result.getComment()).isNull();
        }
    }

    // =========================================================
    // deleteReview
    // =========================================================
    @Nested
    @DisplayName("deleteReview()")
    class DeleteReview {

        @Test
        @DisplayName("Should delete review successfully when user is the owner")
        void shouldDeleteReviewSuccessfully() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));
            willDoNothing().given(reviewRepository).delete(testReview);

            reviewService.deleteReview(100L, 1L);

            then(reviewRepository).should(times(1)).delete(testReview);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not the owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));

            assertThatThrownBy(() -> reviewService.deleteReview(100L, 2L))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You can only delete your own reviews");

            then(reviewRepository).should(never()).delete(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review does not exist")
        void shouldThrowExceptionWhenReviewNotFound() {
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.deleteReview(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found with id: 999");

            then(reviewRepository).should(never()).delete(any(Review.class));
        }

        @Test
        @DisplayName("Should look up review before deleting")
        void shouldLookUpReviewBeforeDeleting() {
            given(reviewRepository.findById(100L)).willReturn(Optional.of(testReview));

            reviewService.deleteReview(100L, 1L);

            then(reviewRepository).should(times(1)).findById(100L);
            then(reviewRepository).should(times(1)).delete(testReview);
        }
    }

    // =========================================================
    // convertToDto
    // =========================================================
    @Nested
    @DisplayName("convertToDto()")
    class ConvertToDto {

        @Test
        @DisplayName("Should correctly map all Review fields to ReviewDto")
        void shouldCorrectlyMapAllFields() {
            ReviewDto dto = reviewService.convertToDto(testReview);

            assertThat(dto.getId()).isEqualTo(100L);
            assertThat(dto.getRecipeId()).isEqualTo(10L);
            assertThat(dto.getUserId()).isEqualTo(1L);
            assertThat(dto.getUserName()).isEqualTo("Alice");
            assertThat(dto.getRating()).isEqualTo(4);
            assertThat(dto.getComment()).isEqualTo("Very tasty!");
        }

        @Test
        @DisplayName("Should include createdAt timestamp in DTO")
        void shouldIncludeCreatedAtTimestamp() {
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
            testReview.setCreatedAt(timestamp);

            ReviewDto dto = reviewService.convertToDto(testReview);

            assertThat(dto.getCreatedAt()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should handle null comment in DTO")
        void shouldHandleNullComment() {
            testReview.setComment(null);

            ReviewDto dto = reviewService.convertToDto(testReview);

            assertThat(dto.getComment()).isNull();
        }

        @Test
        @DisplayName("Should return correct user name in DTO")
        void shouldReturnCorrectUserNameInDto() {
            ReviewDto dto = reviewService.convertToDto(testReview);

            assertThat(dto.getUserName()).isEqualTo("Alice");
        }
    }

    // =========================================================
    // convertToDtoList
    // =========================================================
    @Nested
    @DisplayName("convertToDtoList()")
    class ConvertToDtoList {

        @Test
        @DisplayName("Should convert list of reviews to list of DTOs")
        void shouldConvertListOfReviewsToDtos() {
            Review review2 = new Review();
            review2.setId(101L);
            review2.setUser(otherUser);
            review2.setRecipe(testRecipe);
            review2.setRating(5);
            review2.setComment("Loved it!");
            review2.setCreatedAt(LocalDateTime.now());

            List<Review> reviews = Arrays.asList(testReview, review2);

            List<ReviewDto> dtos = reviewService.convertToDtoList(reviews);

            assertThat(dtos).hasSize(2);
            assertThat(dtos.get(0).getId()).isEqualTo(100L);
            assertThat(dtos.get(1).getId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("Should return empty list when given an empty list")
        void shouldReturnEmptyListWhenGivenEmptyList() {
            List<ReviewDto> dtos = reviewService.convertToDtoList(new ArrayList<>());

            assertThat(dtos).isEmpty();
        }

        @Test
        @DisplayName("Should return list of same size as input")
        void shouldReturnListOfSameSizeAsInput() {
            List<Review> reviews = Arrays.asList(testReview, testReview, testReview);

            List<ReviewDto> dtos = reviewService.convertToDtoList(reviews);

            assertThat(dtos).hasSize(3);
        }

        @Test
        @DisplayName("Should preserve ordering of reviews when converting to DTOs")
        void shouldPreserveOrderingWhenConverting() {
            Review firstReview = new Review();
            firstReview.setId(1L);
            firstReview.setUser(testUser);
            firstReview.setRecipe(testRecipe);
            firstReview.setRating(1);
            firstReview.setCreatedAt(LocalDateTime.now());

            Review secondReview = new Review();
            secondReview.setId(2L);
            secondReview.setUser(otherUser);
            secondReview.setRecipe(testRecipe);
            secondReview.setRating(5);
            secondReview.setCreatedAt(LocalDateTime.now());

            List<ReviewDto> dtos = reviewService.convertToDtoList(Arrays.asList(firstReview, secondReview));

            assertThat(dtos.get(0).getId()).isEqualTo(1L);
            assertThat(dtos.get(1).getId()).isEqualTo(2L);
        }
    }
}
