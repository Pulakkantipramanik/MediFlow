package com.mediflow.user.controller;
import com.mediflow.user.dto.LoginResponseDto;
import com.mediflow.user.dto.UserLoginRequestDto;
import com.mediflow.user.dto.UserRequestDto;
import com.mediflow.user.dto.UserResponseDto;
import com.mediflow.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(
            @Valid @RequestBody UserRequestDto request) {


       /* public UserController(UserService userService) {
            this.userService = userService;
            // we wrote the same line in below and
            //শুধু আগে injected হওয়া Service object-এর method call করছে।
        }*/
        UserResponseDto response =
                userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> loginUser(
            @Valid @RequestBody UserLoginRequestDto request) {

        LoginResponseDto response =
                userService.loginUser(request);

        return ResponseEntity.ok(response);
    }


}