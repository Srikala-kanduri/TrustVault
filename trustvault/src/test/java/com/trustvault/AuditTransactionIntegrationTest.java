
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class AuditTransactionIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void auditRecordSurvivesOuterTransactionRollback() {
        String actor = "audit-transaction-test@example.com";
        String action = "AUDIT_TRANSACTION_TEST";

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                auditService.recordSuccess(
                        actor,
                        action,
                        null,
                        "Verify independent audit transaction");

                status.setRollbackOnly();
            });

            var records =
                    auditLogRepository.findByActorEmailOrderByOccurredAtDesc(
                            actor);

            assertTrue(records.stream().anyMatch(log ->
                    action.equals(log.getAction())
                            && "SUCCESS".equals(log.getOutcome())));

        } finally {
            auditLogRepository.findByActorEmailOrderByOccurredAtDesc(actor)
                    .stream()
                    .filter(log -> action.equals(log.getAction()))
                    .forEach(auditLogRepository::delete);
        }
    }
}
