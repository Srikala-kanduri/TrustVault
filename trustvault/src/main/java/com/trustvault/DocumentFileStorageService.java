package com.trustvault;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class DocumentFileStorageService {

    private final Path storageDirectory;
    private final S3Client s3Client;
    private final String bucket;

    @Autowired
    public DocumentFileStorageService(
            @Value("${trustvault.storage.directory}") String directory,
            @Value("${trustvault.storage.s3.required:false}") boolean s3Required,
            @Value("${trustvault.storage.s3.endpoint:}") String endpoint,
            @Value("${trustvault.storage.s3.bucket:}") String bucket,
            @Value("${trustvault.storage.s3.access-key:}") String accessKey,
            @Value("${trustvault.storage.s3.secret-key:}") String secretKey,
            @Value("${trustvault.storage.s3.region:}") String region) {
        this.storageDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.bucket = bucket;

        boolean endpointConfigured = endpoint != null && !endpoint.isBlank();
        boolean anyStorageSetting = endpointConfigured
                || (bucket != null && !bucket.isBlank())
                || (accessKey != null && !accessKey.isBlank())
                || (secretKey != null && !secretKey.isBlank())
                || (region != null && !region.isBlank());

        if (!endpointConfigured) {
            if (s3Required || anyStorageSetting) {
                throw new IllegalStateException(
                    "Configure all Neon Object Storage settings before startup"
                );
            }
            this.s3Client = null;
            return;
        }

        if (bucket.isBlank() || accessKey.isBlank()
                || secretKey.isBlank() || region.isBlank()) {
            throw new IllegalStateException(
                "Neon Object Storage requires endpoint, bucket, access key, secret key, and region"
            );
        }

        URI storageEndpoint = URI.create(endpoint);
        if (!"https".equalsIgnoreCase(storageEndpoint.getScheme())
                || storageEndpoint.getHost() == null
                || storageEndpoint.getUserInfo() != null) {
            throw new IllegalArgumentException(
                "Neon Object Storage endpoint must be an HTTPS URL"
            );
        }

        this.s3Client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .region(Region.of(region))
            .endpointOverride(storageEndpoint)
            .forcePathStyle(true)
            .build();
    }

    public DocumentFileStorageService(String directory) {
        this.storageDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.s3Client = null;
        this.bucket = "";
    }

    public boolean isRemoteStorageEnabled() {
        return s3Client != null;
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String storageKey = UUID.randomUUID().toString();
        if (isRemoteStorageEnabled()) {
            try {
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .contentType(file.getContentType() == null
                            ? "application/octet-stream" : file.getContentType())
                        .build(),
                    RequestBody.fromBytes(file.getBytes())
                );
            } catch (SdkException exception) {
                throw new IOException("Could not store document in object storage", exception);
            }
            return storageKey;
        }

        Files.createDirectories(storageDirectory);
        Path destination = resolve(storageKey);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return storageKey;
    }

    public byte[] read(String storageKey) throws IOException {
        validateStorageKey(storageKey);
        if (isRemoteStorageEnabled()) {
            try {
                return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build()
                ).asByteArray();
            } catch (SdkException exception) {
                throw new IOException("Could not read document from object storage", exception);
            }
        }
        return Files.readAllBytes(resolve(storageKey));
    }

    public void delete(String storageKey) throws IOException {
        validateStorageKey(storageKey);
        if (isRemoteStorageEnabled()) {
            try {
                s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build()
                );
            } catch (SdkException exception) {
                throw new IOException("Could not delete document from object storage", exception);
            }
            return;
        }
        Files.deleteIfExists(resolve(storageKey));
    }

    public Path resolve(String storageKey) {
        validateStorageKey(storageKey);
        if (isRemoteStorageEnabled()) {
            throw new IllegalStateException(
                "Remote documents must be read or deleted through the storage service"
            );
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

        Path quarantineDirectory = storageDirectory.resolve(".quarantine").normalize();
        if (!quarantineDirectory.startsWith(storageDirectory)) {
            throw new SecurityException("Invalid quarantine path");
        }
        Files.createDirectories(quarantineDirectory);

        Path quarantined = quarantineDirectory.resolve(UUID.randomUUID().toString()).normalize();
        if (!quarantined.startsWith(quarantineDirectory)) {
            throw new SecurityException("Invalid quarantine path");
        }
        Files.move(original, quarantined, StandardCopyOption.ATOMIC_MOVE);
        return quarantined;
    }

    public void restore(String storageKey, Path quarantined) throws IOException {
        Path original = resolve(storageKey);
        Path quarantineDirectory = storageDirectory.resolve(".quarantine").normalize();
        Path expectedQuarantine = quarantined.toAbsolutePath().normalize();
        if (!expectedQuarantine.startsWith(quarantineDirectory)) {
            throw new SecurityException("Invalid quarantine path");
        }
        if (!Files.isRegularFile(expectedQuarantine)) {
            throw new IOException("Quarantined document is unavailable");
        }
        Files.move(expectedQuarantine, original, StandardCopyOption.REPLACE_EXISTING);
    }

    @PreDestroy
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
    }
}
