package com.traderdeck.backend.controllers;

import com.traderdeck.backend.dto.AuthRequest;
import com.traderdeck.backend.dto.AuthResponse;
import com.traderdeck.backend.dto.RegisterRequest;
import com.traderdeck.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.traderdeck.backend.models.User;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        String token = userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token, request.getUsername(), request.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String token = userService.authenticateUser(request.getEmail(), request.getPassword());
        // Retrieve the user details from your service or repository
        User user = userService.getUserByEmail(request.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail()));
    }

}
