package com.tastyrecipes.application.service;

import com.tastyrecipes.application.dto.UserDto;
import com.tastyrecipes.application.exception.ResourceNotFoundException;
import com.tastyrecipes.application.model.User;
import com.tastyrecipes.application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User register(UserDto userDto) {
        if (existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        return userRepository.save(user);
    }
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findById(userId);
        userRepository.delete(user);
    }

    public UserDto convertToDto(User user) {

        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}
