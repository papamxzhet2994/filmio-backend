package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.SocialLink;
import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.payload.UserProfileResponse;
import com.papamxzhet.filmio.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/{id}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Both 'currentPassword' and 'newPassword' are required");
        }

        boolean success = userService.changePassword(id, currentPassword, newPassword);
        if (success) {
            return ResponseEntity.ok("Пароль успешно изменён");
        } else {
            return ResponseEntity.badRequest().body("Текущий пароль неверен или новый пароль недействителен");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        try {
            userService.deleteAccount(id);
            return ResponseEntity.ok("Аккаунт и все связанные данные успешно удалены");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при удалении аккаунта: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/verify-password")
    public ResponseEntity<Void> verifyPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        if (password == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean isValid = userService.verifyPassword(id, password);
        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }



    @PostMapping("/{id}/upload-avatar")
    public ResponseEntity<String> uploadAvatar(@PathVariable Long id,
                                               @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не может быть пустым");
        }

        try {
            String uploadDir = "uploads/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);

            Files.createDirectories(filePath.getParent());

            Files.copy(file.getInputStream(), filePath);

            String avatarUrl = "/uploads/" + fileName;

            userService.updateAvatar(id, avatarUrl);

            return ResponseEntity.ok(avatarUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при загрузке файла: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<String> deleteAvatar(@PathVariable Long id) {
        userService.removeAvatar(id);
        return ResponseEntity.ok("Аватар удалён");
    }

    @PostMapping("/{id}/social-links")
    public ResponseEntity<SocialLink> addSocialLink(@PathVariable Long id, @RequestBody SocialLink link) {
        SocialLink createdLink = userService.addSocialLink(id, link);
        return ResponseEntity.ok(createdLink);
    }

    @DeleteMapping("/{id}/social-links/{linkId}")
    public ResponseEntity<Void> deleteSocialLink(@PathVariable Long id, @PathVariable Long linkId) {
        userService.removeSocialLink(id, linkId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/social-links")
    public ResponseEntity<List<SocialLink>> getSocialLinks(@PathVariable Long id) {
        List<SocialLink> links = userService.getSocialLinks(id);
        return ResponseEntity.ok(links);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String username) {
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String socialLinksJson = user.getSocialLinks()
                .stream()
                .map(link -> String.format("{\"name\": \"%s\", \"url\": \"%s\"}", link.getName(), link.getUrl()))
                .reduce("[", (acc, link) -> acc.equals("[") ? acc + link : acc + "," + link) + "]";

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                socialLinksJson,
                user.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

}
