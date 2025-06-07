package com.papamxzhet.filmio.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String username;
    private boolean requires2FA;

    public JwtResponse(String token, String username) {
        this.token = token;
        this.username = username;
        this.requires2FA = false;
    }

    public static JwtResponse requiresTwoFactor(String username) {
        JwtResponse response = new JwtResponse();
        response.setUsername(username);
        response.setRequires2FA(true);
        return response;
    }
}