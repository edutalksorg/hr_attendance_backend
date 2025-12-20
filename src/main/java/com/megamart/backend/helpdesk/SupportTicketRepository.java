package com.megamart.backend.helpdesk;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findByRequesterId(UUID requesterId);

    List<SupportTicket> findByAssignedToId(UUID assignedToId);
}
