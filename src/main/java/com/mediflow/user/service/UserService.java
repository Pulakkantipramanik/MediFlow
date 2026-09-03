package com.mediflow.user.service;

import com.mediflow.securityconfig.JwtService;
import com.mediflow.user.dto.LoginResponseDto;
import com.mediflow.user.dto.UserLoginRequestDto;
import com.mediflow.user.dto.UserRequestDto;
import com.mediflow.user.dto.UserResponseDto;
import com.mediflow.user.entity.User;
import com.mediflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponseDto registerUser(
            UserRequestDto request) {

        // Check whether email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        // DTO → Entity
        User user = new User();

        user.setName(request.getName());

        user.setEmail(request.getEmail());

        // Encrypt password
        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Default role
        user.setRole("ROLE_USER");

        // Save Entity
        User savedUser =
                userRepository.save(user);

        // Entity → Response DTO
        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
    public LoginResponseDto  loginUser(UserLoginRequestDto request) {

        // Find user by email
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        // Check password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

       /* // Entity → Response DTO
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()*/
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponseDto(
                token,
                "Bearer"
        );
    }
}