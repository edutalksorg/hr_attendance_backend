package com.megamart.backend.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDto {
    private long daysPresent;
    private double totalHours;
    private double averageHoursPerDay;
    private long holidayCount;
    private long absentDays;
}
