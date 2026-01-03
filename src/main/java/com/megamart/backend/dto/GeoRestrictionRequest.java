package com.megamart.backend.dto;

import lombok.Data;

@Data
public class GeoRestrictionRequest {
    private boolean enabled;
    private Double latitude;
    private Double longitude;
    private Double radius;
}
