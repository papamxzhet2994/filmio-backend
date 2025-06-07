package com.papamxzhet.filmio.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String code;
}