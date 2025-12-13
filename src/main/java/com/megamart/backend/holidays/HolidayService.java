package com.megamart.backend.holidays;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class HolidayService {
    private final HolidayRepository repo;

    public Holiday create(String name, LocalDate date, String description) {
        Holiday h = Holiday.builder().name(name).holidayDate(date).description(description).build();
        return repo.save(h);
    }

    public Holiday get(@NonNull UUID id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Holiday not found"));
    }

    public List<Holiday> list() {
        return repo.findAll();
    }

    public Holiday update(@NonNull UUID id, String name, LocalDate date, String description) {
        Holiday h = get(id);
        h.setName(name);
        h.setHolidayDate(date);
        h.setDescription(description);
        return repo.save(h);
    }

    public void delete(@NonNull UUID id) {
        repo.deleteById(id);
    }

    public List<Holiday> between(LocalDate start, LocalDate end) {
        return repo.findByHolidayDateBetween(start, end);
    }
}
