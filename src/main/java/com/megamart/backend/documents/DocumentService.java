package com.megamart.backend.documents;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class DocumentService {
    private final DocumentRepository repository;
    private final com.megamart.backend.notification.NotificationService notificationService;
    private final java.nio.file.Path fileStorageLocation;

    public DocumentService(DocumentRepository repository,
            com.megamart.backend.notification.NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.fileStorageLocation = java.nio.file.Paths.get("uploads").toAbsolutePath().normalize();
        try {
            java.nio.file.Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Document create(@NonNull UUID userId, String type, org.springframework.web.multipart.MultipartFile file,
            @NonNull UUID generatedBy, String role, String uploaderName, OffsetDateTime expiresAt) {

        String fileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Using UUID to prevent overwrite
            String storedFileName = UUID.randomUUID().toString() + "_" + fileName;
            java.nio.file.Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            java.nio.file.Files.copy(file.getInputStream(), targetLocation,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Document d = Document.builder().userId(userId).type(type).fileName(fileName)
                    .filePath(targetLocation.toString()).generatedBy(generatedBy).role(role).uploadedBy(uploaderName)
                    .generatedAt(OffsetDateTime.now()).expiresAt(expiresAt).createdAt(OffsetDateTime.now()).build();
            Document saved = repository.save(d);

            // Notify User
            notificationService.send(userId, "New Document Uploaded",
                    "A new document of type " + type + " has been uploaded for you.", "DOCUMENT");

            return saved;
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public java.util.List<Document> listAll() {
        return repository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public org.springframework.core.io.Resource loadFileAsResource(String fileName) {
        try {
            // We need to find the file path from DB first ideally, but here we can try to
            // resolve if we had the full path.
            // However, controller will pass the ID, service fetch entity, get path, then
            // load resource.
            // Let's change this method to take the filePath or add a helper
            return null;
        } catch (Exception e) {
            throw new RuntimeException("File not found " + fileName, e);
        }
    }

    public org.springframework.core.io.Resource loadFileAsResource(Document document) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(document.getFilePath());
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(
                    filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + document.getFileName());
            }
        } catch (java.net.MalformedURLException ex) {
            throw new RuntimeException("File not found " + document.getFileName(), ex);
        }
    }

    public Document get(@NonNull UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public List<Document> listForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(@NonNull UUID id) {
        repository.deleteById(id);
    }
}
