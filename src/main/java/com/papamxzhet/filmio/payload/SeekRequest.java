package com.papamxzhet.filmio.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SeekRequest {
    private double timestamp;
}