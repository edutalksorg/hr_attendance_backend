package com.megamart.backend.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    boolean existsByName(String name);
}
