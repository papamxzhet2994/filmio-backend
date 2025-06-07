package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 1;

    public void initiatePasswordReset(String email) {
        try {
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return;
            }

            String resetToken = generateResetToken();
            user.get().setPasswordResetToken(resetToken);
            user.get().setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
            userRepository.save(user.get());

            emailService.sendPasswordResetEmail(email, resetToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean resetPassword(String token, String newPassword) {
        try {
            User user = userRepository.findByPasswordResetToken(token);

            if (user == null || user.getPasswordResetTokenExpiry() == null ||
                    user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
                return false;
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateResetToken(String token) {
        User user = userRepository.findByPasswordResetToken(token);
        return user != null && user.getPasswordResetTokenExpiry() != null &&
                user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now());
    }

    private String generateResetToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}