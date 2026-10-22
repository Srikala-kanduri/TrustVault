
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class DocumentFileStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesFileUsingGeneratedStorageKey() throws IOException {
        DocumentFileStorageService service =
                new DocumentFileStorageService(tempDirectory.toString());

        MultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                "text/plain",
                "TrustVault test content".getBytes());

        String storageKey = service.store(file);
        Path storedPath = service.resolve(storageKey);

        assertTrue(Files.exists(storedPath));
        assertEquals(
                "TrustVault test content",
                Files.readString(storedPath));
        assertFalse(storageKey.contains("report.txt"));
    }

    @Test
    void rejectsEmptyFile() {
        DocumentFileStorageService service =
                new DocumentFileStorageService(tempDirectory.toString());

        MultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.store(file));
    }

    @Test
    void rejectsInvalidStorageKey() {
        DocumentFileStorageService service =
                new DocumentFileStorageService(tempDirectory.toString());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolve("../secret.txt"));
    }
}
