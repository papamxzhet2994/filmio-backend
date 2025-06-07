package com.papamxzhet.filmio.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VideoControlRequest {
    private String videoUrl;
    private double timestamp;
    private String type;
}