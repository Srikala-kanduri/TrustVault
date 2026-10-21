
package com.trustvault;

import java.time.Instant;

public record DocumentSearchRequest(
        String title,
        String category,
        String ownerEmail,
        Instant fromDate,
        Instant toDate) {
}
