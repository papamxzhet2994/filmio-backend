package com.papamxzhet.filmio.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserProfileResponse {
    private Long id;
    private String username;
    private String avatarUrl;

    @JsonProperty("socialLinks")
    private String socialLinksJson;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    public UserProfileResponse(Long id, String username, String avatarUrl, String socialLinksJson, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.socialLinksJson = socialLinksJson;
        this.createdAt = createdAt;
    }
}
