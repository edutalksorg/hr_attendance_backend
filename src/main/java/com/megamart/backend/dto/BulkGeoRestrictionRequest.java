package com.megamart.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BulkGeoRestrictionRequest {
    private List<UUID> userIds;
    private GeoRestrictionRequest data;
}
