package com.tastyrecipes.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;

    // Make name optional for updates (only required for registration)
    private String name;

    // Make email optional for updates
    @Email(message = "Email should be valid")
    private String email;

    // Make password optional (not needed for profile updates)
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // Constructor for responses (without password)
    public UserDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}