package com.megamart.backend.helpdesk;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class HelpdeskService {

    private final SupportTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;

    private final com.megamart.backend.notification.NotificationService notificationService;

    public SupportTicket createTicket(UUID requesterId, SupportTicket ticket) {
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new RuntimeException("User not found"));
        ticket.setRequester(requester);
        SupportTicket saved = ticketRepository.save(ticket);
        return saved;
    }

    public SupportTicket createTicketByEmail(String email, SupportTicket ticket) {
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        ticket.setRequester(requester);
        return ticketRepository.save(ticket);
    }

    public List<SupportTicket> getUserTickets(UUID userId) {
        return ticketRepository.findByRequesterId(userId);
    }

    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public SupportTicket updateTicketStatus(UUID ticketId, String status, UUID assignedToId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        ticket.setUpdatedAt(OffsetDateTime.now());

        if (assignedToId != null) {
            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            ticket.setAssignedTo(assignee);
            // Notify Assignee
            notificationService.send(
                    assignee.getId(),
                    "Ticket Assigned",
                    "Ticket #" + ticket.getId().toString().substring(0, 8) + " has been assigned to you.",
                    "TICKET_ASSIGNED");
        }

        // Notify Requester
        notificationService.send(
                ticket.getRequester().getId(),
                "Ticket Update",
                "Your ticket #" + ticket.getId().toString().substring(0, 8) + " status is now: " + status,
                "TICKET_UPDATE");

        return ticketRepository.save(ticket);
    }

    public TicketComment addComment(UUID ticketId, UUID authorId, String content) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        User author = userRepository.findById(authorId).orElseThrow(() -> new RuntimeException("User not found"));

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(author)
                .content(content)
                .build();

        TicketComment saved = commentRepository.save(comment);

        // Notification Logic
        if (!author.getId().equals(ticket.getRequester().getId())) {
            // Admin commented -> Notify Requester
            notificationService.send(
                    ticket.getRequester().getId(),
                    "New Comment on Ticket",
                    "New comment from " + author.getUsername() + ": " + content,
                    "TICKET_COMMENT");
        } else {
            // Requester commented -> Notify Assignee or Admins
            if (ticket.getAssignedTo() != null) {
                notificationService.send(
                        ticket.getAssignedTo().getId(),
                        "New Reply on Ticket",
                        "Requester replied: " + content,
                        "TICKET_REPLY");
            }
        }

        return saved;
    }

    public List<TicketComment> getComments(UUID ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}
