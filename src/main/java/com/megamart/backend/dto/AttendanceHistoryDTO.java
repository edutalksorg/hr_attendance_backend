package com.megamart.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceHistoryDTO {
    private LocalDate date;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private String ipAddress;
    private String remark;
    private String status; // Present, Absent, Holiday
}
