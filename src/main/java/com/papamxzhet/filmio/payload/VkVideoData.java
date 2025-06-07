package com.papamxzhet.filmio.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VkVideoData {
    private String title;
    private String description;
    private String thumbnailUrl;
    private String directUrl;
    private int duration;
    private String ownerName;
}