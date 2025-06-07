package com.papamxzhet.filmio.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VkVideoResponse {
    private boolean success;
    private String message;
    private VkVideoData data;

    public VkVideoResponse(VkVideoData data) {
        this.success = true;
        this.data = data;
    }

    public VkVideoResponse(String errorMessage) {
        this.success = false;
        this.message = errorMessage;
        this.data = null;
    }
}