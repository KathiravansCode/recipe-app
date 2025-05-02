
package com.tastyrecipes.application.controller;

import com.tastyrecipes.application.dto.ApiResponse;
import com.tastyrecipes.application.dto.AuthRequest;
import com.tastyrecipes.application.dto.AuthResponse;
import com.tastyrecipes.application.dto.UserDto;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.security.JwtUtil;
import com.tastyrecipes.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody UserDto userDto) {
        User user = userService.register(userDto);
        UserDto registeredUser = userService.convertToDto(user);

        return new ResponseEntity<>(
                new ApiResponse(true, "User registered successfully", registeredUser),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(userDetails);
        User user = userService.findByEmail(userDetails.getUsername());

        AuthResponse authResponse = new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );

        return ResponseEntity.ok(new ApiResponse(true, "Login successful", authResponse));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse> deleteAccount(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);

        userService.deleteAccount(user.getId());

        return ResponseEntity.ok(new ApiResponse(true, "Account deleted successfully"));
    }
}