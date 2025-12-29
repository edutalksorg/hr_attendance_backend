package com.megamart.backend.helpdesk;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/helpdesk")
@RequiredArgsConstructor
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    @PostMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<?> createTicket(@RequestBody SupportTicket ticket) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            return ResponseEntity.ok(helpdeskService.createTicketByEmail(email, ticket));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating ticket: " + e.getMessage());
        }
    }

    @GetMapping("/tickets/my/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<SupportTicket>> getMyTickets(@PathVariable UUID userId) {
        return ResponseEntity.ok(helpdeskService.getUserTickets(userId));
    }

    @GetMapping("/tickets/all")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<SupportTicket>> getAllTickets() {
        return ResponseEntity.ok(helpdeskService.getAllTickets());
    }

    @PutMapping("/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<SupportTicket> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) UUID assignedToId) {
        return ResponseEntity.ok(helpdeskService.updateTicketStatus(id, status, assignedToId));
    }

    @PostMapping("/tickets/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<TicketComment> addComment(
            @PathVariable UUID id,
            @RequestBody com.fasterxml.jackson.databind.JsonNode payload) {
        UUID authorId = UUID.fromString(payload.get("authorId").asText());
        String content = payload.get("content").asText();
        return ResponseEntity.ok(helpdeskService.addComment(id, authorId, content));
    }

    @GetMapping("/tickets/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<TicketComment>> getTicketComments(@PathVariable UUID id) {
        return ResponseEntity.ok(helpdeskService.getComments(id));
    }

    @DeleteMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<?> deleteTicket(@PathVariable UUID id) {
        try {
            helpdeskService.deleteTicket(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting ticket: " + e.getMessage());
        }
    }
}
