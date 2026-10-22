
package com.trustvault;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentFileStorageService {

    private final Path storageDirectory;

    public DocumentFileStorageService(
            @Value("${trustvault.storage.directory}") String directory) {
        this.storageDirectory = Path.of(directory)
                .toAbsolutePath()
                .normalize();
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        Files.createDirectories(storageDirectory);

        String storageKey = UUID.randomUUID().toString();
        Path destination = storageDirectory.resolve(storageKey).normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new SecurityException("Invalid storage path");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return storageKey;
    }

    public Path resolve(String storageKey) {
        if (storageKey == null
                || !storageKey.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        Path resolved = storageDirectory.resolve(storageKey).normalize();

        if (!resolved.startsWith(storageDirectory)) {
            throw new SecurityException("Invalid storage path");
        }

        return resolved;
    }

    
public Path quarantine(String storageKey) throws IOException {
    Path original = resolve(storageKey);

    if (!Files.isRegularFile(original)) {
        throw new IOException("Stored document is unavailable");
    }

    Path quarantineDirectory = storageDirectory.resolve(".quarantine")
            .normalize();

    if (!quarantineDirectory.startsWith(storageDirectory)) {
        throw new SecurityException("Invalid quarantine path");
    }

    Files.createDirectories(quarantineDirectory);

    Path quarantined = quarantineDirectory
            .resolve(UUID.randomUUID().toString())
            .normalize();

    if (!quarantined.startsWith(quarantineDirectory)) {
        throw new SecurityException("Invalid quarantine path");
    }

    Files.move(original, quarantined, StandardCopyOption.ATOMIC_MOVE);

    return quarantined;
}

public void restore(String storageKey, Path quarantined) throws IOException {
    Path original = resolve(storageKey);
    Path quarantineDirectory = storageDirectory.resolve(".quarantine")
            .normalize();
    Path expectedQuarantine = quarantined.toAbsolutePath().normalize();

    if (!expectedQuarantine.startsWith(quarantineDirectory)) {
        throw new SecurityException("Invalid quarantine path");
    }

    if (!Files.isRegularFile(expectedQuarantine)) {
        throw new IOException("Quarantined document is unavailable");
    }

    Files.move(
            expectedQuarantine,
            original,
            StandardCopyOption.REPLACE_EXISTING);
}



}
