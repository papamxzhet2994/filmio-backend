package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.SocialLink;
import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.repository.SocialLinkRepository;
import com.papamxzhet.filmio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SocialLinkRepository socialLinkRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Delete associated social links
        socialLinkRepository.deleteAllByUserId(userId);

        // Remove the avatar file if it exists
        if (user.getAvatarUrl() != null) {
            String avatarPath = user.getAvatarUrl().replace("/uploads/", "uploads/");
            Path filePath = Paths.get(avatarPath);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при удалении аватара: " + e.getMessage());
            }
        }

        // Delete the user record
        userRepository.delete(user);
    }

    public boolean verifyPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return passwordEncoder.matches(password, user.getPassword());
    }


    public void updateAvatar(Long userId, String avatarUrl) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
        });
    }

    public void removeAvatar(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setAvatarUrl(null);
            userRepository.save(user);
        });
    }

    public SocialLink addSocialLink(Long userId, SocialLink link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        link.setUser(user);
        return socialLinkRepository.save(link);
    }

    @Transactional
    public void removeSocialLink(Long userId, Long linkId) {
        socialLinkRepository.deleteByIdAndUserId(linkId, userId);
    }

    public SocialLink updateSocialLink(Long userId, Long linkId, SocialLink updatedLink) {
        SocialLink link = socialLinkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));
        if (!link.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        link.setName(updatedLink.getName());
        link.setUrl(updatedLink.getUrl());
        return socialLinkRepository.save(link);
    }

    public List<SocialLink> getSocialLinks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return socialLinkRepository.findByUserId(user.getId());
    }

}