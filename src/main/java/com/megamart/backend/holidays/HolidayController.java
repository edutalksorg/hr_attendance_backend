package com.megamart.backend.holidays;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class HolidayController {
    private final HolidayService service;

    public static record CreateReq(@NotBlank String name,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate holidayDate, String description) {
    }

    public static record UpdateReq(@NotBlank String name,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate holidayDate, String description) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Holiday> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(201).body(service.create(req.name(), req.holidayDate(), req.description()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Holiday> get(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<Holiday>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Holiday> update(@PathVariable @NonNull UUID id, @Valid @RequestBody UpdateReq req) {
        return ResponseEntity.ok(service.update(id, req.name(), req.holidayDate(), req.description()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<Holiday>> between(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.between(start, end));
    }
}
