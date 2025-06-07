package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.User;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class TwoFactorService {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Value("${app.verification-code.expiry-minutes:10}")
    private int codeExpiryMinutes;

    private final Random random = new SecureRandom();

    public void generateAndSendVerificationCode(User user) throws MessagingException {
        String verificationCode = generateVerificationCode();

        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        userService.saveUser(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
    }

    public boolean verifyCode(String username, String code) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerificationCodeValid() &&
                user.getVerificationCode().equals(code)) {
            user.setVerificationCode(null);
            user.setVerificationCodeExpiry(null);
            userService.saveUser(user);
            return true;
        }

        return false;
    }

    public void sendEmailVerificationCode(User user) throws MessagingException {
        String verificationCode = generateVerificationCode();

        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        userService.saveUser(user);

        emailService.sendEmailVerification(user.getEmail(), verificationCode);
    }

    public boolean verifyEmail(String username, String code) {
        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isVerificationCodeValid() && user.getVerificationCode().equals(code)) {
                user.setEmailVerified(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiry(null);
                userService.saveUser(user);

                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error in verifyEmail: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String generateVerificationCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}