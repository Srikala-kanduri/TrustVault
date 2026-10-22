
package com.trustvault;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentFileValidator {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg");

    public void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String normalizedName = filename.toLowerCase(Locale.ROOT);
        int dot = normalizedName.lastIndexOf('.');

        if (dot < 0 || dot == normalizedName.length() - 1) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        String extension = normalizedName.substring(dot + 1);
        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        byte[] header = new byte[8];
        int bytesRead;
        try (InputStream input = file.getInputStream()) {
            bytesRead = input.read(header);
        }

        if (!matchesSignature(extension, header, bytesRead)) {
            throw new IllegalArgumentException(
                    "File content does not match its extension");
        }
    }

    private boolean matchesSignature(
            String extension, byte[] header, int length) {

        return switch (extension) {
            case "pdf" -> length >= 5
                    && header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F'
                    && header[4] == '-';

            case "png" -> length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 'P'
                    && header[2] == 'N'
                    && header[3] == 'G'
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;

            case "jpg", "jpeg" -> length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;

            default -> false;
        };
    }
}
