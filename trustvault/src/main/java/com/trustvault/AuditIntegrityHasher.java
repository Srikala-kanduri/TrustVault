package com.trustvault;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuditIntegrityHasher {

    public static final String GENESIS_HASH = "0".repeat(64);

    private final byte[] secret;

    public AuditIntegrityHasher(
            @Value("${trustvault.audit.hmac-key}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException(
                    "The audit HMAC key must be at least 32 bytes");
        }
    }

    public String entryHash(
            String previousHash,
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome,
            Instant occurredAt) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(bytes)) {
                data.writeUTF("TRUSTVAULT_AUDIT_V1");
                data.writeUTF(previousHash);
                writeNullable(data, actorEmail);
                writeNullable(data, action);
                data.writeBoolean(documentId != null);
                if (documentId != null) data.writeLong(documentId);
                writeNullable(data, details);
                writeNullable(data, outcome);
                data.writeLong(occurredAt.getEpochSecond());
                data.writeInt(occurredAt.getNano());
            }
            return sign(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode audit event", exception);
        }
    }

    public String headHash(String lastEntryHash, long entryCount) {
        return sign(("TRUSTVAULT_AUDIT_HEAD_V1\n" + lastEntryHash + "\n" + entryCount)
                .getBytes(StandardCharsets.UTF_8));
    }

    public boolean matches(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private String sign(byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign audit data", exception);
        }
    }

    private void writeNullable(DataOutputStream data, String value)
            throws IOException {
        data.writeBoolean(value != null);
        if (value != null) data.writeUTF(value);
    }
}
