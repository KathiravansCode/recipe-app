package com.tastyrecipes.application;



import com.tastyrecipes.application.dto.UserDto;
import com.tastyrecipes.application.exception.ResourceNotFoundException;
import com.tastyrecipes.application.model.Recipe;
import com.tastyrecipes.application.model.Review;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRecipes(new ArrayList<>());
        testUser.setReviews(new ArrayList<>());

        testUserDto = new UserDto();
        testUserDto.setName("John Doe");
        testUserDto.setEmail("john@example.com");
        testUserDto.setPassword("plainPassword");
    }

    // =========================================================
    // findById
    // =========================================================
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return user when ID exists")
        void shouldReturnUserWhenIdExists() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            User result = userService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowExceptionWhenIdNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 99");
        }

        @Test
        @DisplayName("Should call repository exactly once with correct ID")
        void shouldCallRepositoryOnce() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            userService.findById(1L);

            then(userRepository).should(times(1)).findById(1L);
        }
    }

    // =========================================================
    // findByEmail
    // =========================================================
    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("Should return user when email exists")
        void shouldReturnUserWhenEmailExists() {
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(testUser));

            User result = userService.findByEmail("john@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when email does not exist")
        void shouldThrowExceptionWhenEmailNotFound() {
            given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with email: unknown@example.com");
        }

        @Test
        @DisplayName("Should handle email with different casing")
        void shouldHandleEmailQuery() {
            given(userRepository.findByEmail("JOHN@EXAMPLE.COM")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("JOHN@EXAMPLE.COM"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================
    // existsByEmail
    // =========================================================
    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmail {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            given(userRepository.existsByEmail("john@example.com")).willReturn(true);

            boolean result = userService.existsByEmail("john@example.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);

            boolean result = userService.existsByEmail("new@example.com");

            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // register
    // =========================================================
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Should register a new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            given(userRepository.existsByEmail("john@example.com")).willReturn(false);
            given(passwordEncoder.encode("plainPassword")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);

            User result = userService.register(testUserDto);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode("plainPassword")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);

            userService.register(testUserDto);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email already in use")
        void shouldThrowExceptionWhenEmailAlreadyInUse() {
            given(userRepository.existsByEmail("john@example.com")).willReturn(true);

            assertThatThrownBy(() -> userService.register(testUserDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already in use");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should set correct name and email on new user")
        void shouldSetCorrectNameAndEmailOnNewUser() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            User result = userService.register(testUserDto);

            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should save the user to the repository")
        void shouldSaveUserToRepository() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded");
            given(userRepository.save(any(User.class))).willReturn(testUser);

            userService.register(testUserDto);

            then(userRepository).should(times(1)).save(any(User.class));
        }
    }

    // =========================================================
    // updateUser
    // =========================================================
    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("Should update and return saved user")
        void shouldUpdateAndReturnSavedUser() {
            testUser.setName("Updated Name");
            given(userRepository.save(testUser)).willReturn(testUser);

            User result = userService.updateUser(testUser);

            assertThat(result.getName()).isEqualTo("Updated Name");
            then(userRepository).should(times(1)).save(testUser);
        }

        @Test
        @DisplayName("Should persist all user field changes")
        void shouldPersistAllUserFieldChanges() {
            testUser.setName("New Name");
            testUser.setEmail("new@example.com");
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(testUser);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
        }
    }

    // =========================================================
    // deleteAccount
    // =========================================================
    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccount {

        @Test
        @DisplayName("Should delete user when ID exists")
        void shouldDeleteUserWhenIdExists() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            willDoNothing().given(userRepository).delete(testUser);

            userService.deleteAccount(1L);

            then(userRepository).should(times(1)).delete(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteAccount(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 99");

            then(userRepository).should(never()).delete(any(User.class));
        }

        @Test
        @DisplayName("Should find user before deleting")
        void shouldFindUserBeforeDeleting() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            userService.deleteAccount(1L);

            then(userRepository).should(times(1)).findById(1L);
            then(userRepository).should(times(1)).delete(testUser);
        }
    }

    // =========================================================
    // convertToDto
    // =========================================================
    @Nested
    @DisplayName("convertToDto()")
    class ConvertToDto {

        @Test
        @DisplayName("Should correctly map user fields to DTO")
        void shouldCorrectlyMapUserFieldsToDto() {
            UserDto dto = userService.convertToDto(testUser);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("John Doe");
            assertThat(dto.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should not expose password in DTO")
        void shouldNotExposePasswordInDto() {
            UserDto dto = userService.convertToDto(testUser);

            assertThat(dto.getPassword()).isNull();
        }

        @Test
        @DisplayName("Should return DTO with non-null fields for valid user")
        void shouldReturnDtoWithNonNullFieldsForValidUser() {
            UserDto dto = userService.convertToDto(testUser);

            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getName()).isNotNull();
            assertThat(dto.getEmail()).isNotNull();
        }

        @Test
        @DisplayName("Should handle user with no recipes or reviews")
        void shouldHandleUserWithNoRecipesOrReviews() {
            testUser.setRecipes(new ArrayList<>());
            testUser.setReviews(new ArrayList<>());

            UserDto dto = userService.convertToDto(testUser);

            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(1L);
        }
    }
}