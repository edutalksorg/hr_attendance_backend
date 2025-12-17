package com.megamart.backend.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UpdateAttendanceRequest {
    private String status;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private String remark;
}
