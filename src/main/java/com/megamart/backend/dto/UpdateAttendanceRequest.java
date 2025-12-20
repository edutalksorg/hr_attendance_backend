package com.megamart.backend.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UpdateAttendanceRequest {
    private String status;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private String remark;
    private java.util.UUID userId; // Added to support manual creation where userId is needed
}
