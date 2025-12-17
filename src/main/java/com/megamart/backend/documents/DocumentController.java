package com.megamart.backend.documents;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentController {

    private final DocumentService service;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Document> upload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "targetUserId", required = false) UUID targetUserId,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            org.springframework.security.core.Authentication authentication) {

        com.megamart.backend.security.CustomUserDetails principal = (com.megamart.backend.security.CustomUserDetails) authentication
                .getPrincipal();
        UUID uploaderId = principal.getUser().getId();
        String role = principal.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "");
        String uploaderName = principal.getUser().getFullName() + " (" + principal.getUser().getEmployeeId() + ")";

        // Determine Owner (userId of the document)
        UUID ownerId = uploaderId; // Default to self

        if (targetUserId != null) {
            // Only Admin/HR can upload for others
            if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("HR")) {
                ownerId = targetUserId;
            } else {
                // Return 403 if normal user tries to upload for others
                return ResponseEntity.status(403).build();
            }
        }

        return ResponseEntity.status(201)
                .body(service.create(ownerId, type, file, uploaderId, role, uploaderName, null));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<Document>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')") // Check ownership logic inside service
                                                                               // or here?
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable UUID id,
            org.springframework.security.core.Authentication auth) {
        Document doc = service.get(id);

        // Access check: Admin/HR can download anything. Users only their own.
        com.megamart.backend.security.CustomUserDetails principal = (com.megamart.backend.security.CustomUserDetails) auth
                .getPrincipal();
        String role = principal.getAuthorities().stream().findFirst().get().getAuthority();

        if (!role.contains("ADMIN") && !role.contains("HR") && !doc.getUserId().equals(principal.getUser().getId())) {
            return ResponseEntity.status(403).build();
        }

        org.springframework.core.io.Resource resource = service.loadFileAsResource(doc);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Document> get(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<Document>> listForUser(@PathVariable @NonNull UUID userId,
            org.springframework.security.core.Authentication auth) {
        com.megamart.backend.security.CustomUserDetails principal = (com.megamart.backend.security.CustomUserDetails) auth
                .getPrincipal(); // FIX: Explicit cast
        String role = principal.getAuthorities().stream().findFirst().get().getAuthority();

        if (!role.contains("ADMIN") && !role.contains("HR") && !userId.equals(principal.getUser().getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(service.listForUser(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
