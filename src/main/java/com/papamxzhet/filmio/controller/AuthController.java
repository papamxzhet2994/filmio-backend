package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.payload.JwtResponse;
import com.papamxzhet.filmio.payload.LoginRequest;
import com.papamxzhet.filmio.security.jwt.JwtUtils;
import com.papamxzhet.filmio.service.CustomUserDetailsService;
import com.papamxzhet.filmio.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication.getName()); // Генерация JWT токена

            return ResponseEntity.ok(new JwtResponse(jwt, loginRequest.getUsername()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        if (userService.existsByUsername(user.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is already taken!");
        }

        if (userService.existsByEmail(user.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveUser(user);

        return ResponseEntity.ok("User registered successfully!");
    }


    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
