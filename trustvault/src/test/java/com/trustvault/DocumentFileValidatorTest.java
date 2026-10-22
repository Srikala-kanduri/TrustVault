package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class DocumentFileValidatorTest {

    private final DocumentFileValidator validator = new DocumentFileValidator();

    @Test
    void acceptsValidPdf() {
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf",
                "%PDF-1.7 test".getBytes());
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void acceptsValidPng() {
        byte[] signature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
        MultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", signature);
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void acceptsValidJpeg() {
        byte[] signature = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", signature);
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void acceptsValidJpegWithJpegExtension() {
        byte[] signature = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MultipartFile file = new MockMultipartFile(
                "file", "image.jpeg", "image/jpeg", signature);
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void rejectsUnsupportedExtension() {
        MultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream",
                "MZ content".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(file));
    }

    @Test
    void rejectsMismatchedSignature() {
        MultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf",
                "not a pdf".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(file));
    }

    @Test
    void rejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(file));
    }

    @Test
    void rejectsFilenameWithoutExtension() {
        MultipartFile file = new MockMultipartFile(
                "file", "report", "application/pdf",
                "%PDF-1.7 test".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(file));
    }

    @Test
    void rejectsMissingFilename() {
        MultipartFile file = new MockMultipartFile(
                "file", null, "application/pdf",
                "%PDF-1.7 test".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(file));
    }
}
