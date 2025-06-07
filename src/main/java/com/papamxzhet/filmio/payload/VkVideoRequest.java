package com.papamxzhet.filmio.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VkVideoRequest {
    private String videoUrl;
    private String accessToken;
}
