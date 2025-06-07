package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.payload.*;
import com.papamxzhet.filmio.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest verifyRequest) {
        return authService.verifyCode(verifyRequest);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody Map<String, String> request) {
        return authService.resendVerificationEmail(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.registerUser(registerRequest);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyCodeRequest verifyRequest) {
        return authService.verifyEmail(verifyRequest);
    }

    @PostMapping("/enable-2fa")
    public ResponseEntity<?> enable2FA(Authentication authentication) {
        return authService.enable2FA(authentication);
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<?> disable2FA(Authentication authentication) {
        return authService.disable2FA(authentication);
    }

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        return authService.validateResetToken(token);
    }
}