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
    private java.util.UUID id;
    private LocalDate date;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private String ipAddress;
    private String logoutIpAddress;
    private String remark;
    private String status; // Present, Absent, Holiday
    private boolean canCheckOut;
    private java.util.List<java.util.Map<String, String>> ipHistory;
}
