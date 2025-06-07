package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.payload.*;
import com.papamxzhet.filmio.security.jwt.JwtUtils;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private PasswordResetService passwordResetService;

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не найден");
            }

            User user = userOptional.get();

            if (user.isTwoFactorEnabled()) {
                return handleTwoFactorAuthentication(user);
            }

            String jwt = jwtUtils.generateJwtToken(authentication.getName());
            return ResponseEntity.ok(new JwtResponse(jwt, loginRequest.getUsername()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка: " + e.getMessage());
        }
    }

    public ResponseEntity<?> verifyCode(VerifyCodeRequest verifyRequest) {
        boolean isValid = twoFactorService.verifyCode(verifyRequest.getUsername(), verifyRequest.getCode());

        if (isValid) {
            String jwt = jwtUtils.generateJwtToken(verifyRequest.getUsername());
            return ResponseEntity.ok(new JwtResponse(jwt, verifyRequest.getUsername(), true));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Недействительный или просроченный код подтверждения");
        }
    }

    public ResponseEntity<?> resendVerificationEmail(Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Требуется адрес электронной почты");
        }

        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь не найден");
        }

        User user = userOptional.get();

        if (user.isEmailVerified()) {
            return ResponseEntity.ok().body("Электронная почта уже верифицирована");
        }

        try {
            twoFactorService.sendEmailVerificationCode(user);
            return ResponseEntity.ok().body("Письмо с подтверждением успешно отправлено");
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Не удалось отправить письмо с подтверждением: " + e.getMessage());
        }
    }

    public ResponseEntity<?> registerUser(RegisterRequest registerRequest) {
        ResponseEntity<?> validationError = validateUserRegistration(registerRequest);
        if (validationError != null) {
            return validationError;
        }

        User user = createUser(registerRequest);

        try {
            twoFactorService.sendEmailVerificationCode(user);
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Пользователь зарегистрирован, но произошла ошибка при отправке письма с подтверждением: " + e.getMessage());
        }

        return authenticateAfterRegistration(registerRequest);
    }

    public ResponseEntity<?> verifyEmail(VerifyCodeRequest verifyRequest) {
        boolean isVerified = twoFactorService.verifyEmail(verifyRequest.getUsername(), verifyRequest.getCode());

        if (isVerified) {
            String jwt = jwtUtils.generateJwtToken(verifyRequest.getUsername());
            return ResponseEntity.ok(new JwtResponse(jwt, verifyRequest.getUsername()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Недействительный или просроченный код подтверждения");
        }
    }

    public ResponseEntity<?> enable2FA(Authentication authentication) {
        String username = authentication.getName();
        User user = getUserByUsername(username);

        if (!user.isEmailVerified()) {
            return ResponseEntity.badRequest().body("Перед включением 2FA вам необходимо подтвердить свой адрес электронной почты.");
        }

        user.setTwoFactorEnabled(true);
        userService.saveUser(user);

        return ResponseEntity.ok().body("Двухфакторная аутентификация успешно включена");
    }

    public ResponseEntity<?> disable2FA(Authentication authentication) {
        String username = authentication.getName();
        User user = getUserByUsername(username);

        user.setTwoFactorEnabled(false);
        userService.saveUser(user);

        return ResponseEntity.ok().body("Двухфакторная аутентификация отключена");
    }

    public User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return getUserByUsername(username);
    }

    public ResponseEntity<?> forgotPassword(Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Требуется адрес электронной почты");
        }

        passwordResetService.initiatePasswordReset(email);
        return ResponseEntity.ok().body("Если учетная запись с таким адресом электронной почты существует, вам была отправлена ссылка для сброса пароля.");
    }

    public ResponseEntity<?> resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body("Требуются токен и новый пароль.");
        }

        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok().body("Пароль успешно сброшен");
        } else {
            return ResponseEntity.badRequest().body("Недействительный или просроченный токен сброса");
        }
    }

    public ResponseEntity<?> validateResetToken(String token) {
        boolean isValid = passwordResetService.validateResetToken(token);
        return ResponseEntity.ok().body(Map.of("valid", isValid));
    }


    private ResponseEntity<?> handleTwoFactorAuthentication(User user) {
        try {
            twoFactorService.generateAndSendVerificationCode(user);
            return ResponseEntity.ok(JwtResponse.requiresTwoFactor(user.getUsername()));
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Не удалось отправить код подтверждения.: " + e.getMessage());
        }
    }

    private ResponseEntity<?> validateUserRegistration(RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Ошибка: имя пользователя уже занято!");
        }

        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Ошибка: адрес электронной почты уже используется!");
        }

        return null;
    }

    private User createUser(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        return userService.saveUser(user);
    }

    private ResponseEntity<?> authenticateAfterRegistration(RegisterRequest registerRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            registerRequest.getUsername(),
                            registerRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication.getName());

            return ResponseEntity.ok(new JwtResponse(jwt, registerRequest.getUsername()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok().body("Пользователь успешно зарегистрирован! Пожалуйста, подтвердите свой адрес электронной почты и логин.");
        }
    }

    private User getUserByUsername(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
}