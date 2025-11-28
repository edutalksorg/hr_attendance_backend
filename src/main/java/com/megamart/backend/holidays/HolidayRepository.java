package com.megamart.backend.holidays;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    List<Holiday> findByHolidayDateBetween(LocalDate start, LocalDate end);
}
