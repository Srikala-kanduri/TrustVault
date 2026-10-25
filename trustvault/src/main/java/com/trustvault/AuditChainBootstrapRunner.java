package com.trustvault;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AuditChainBootstrapRunner implements ApplicationRunner {

    private final AuditChainBootstrapService bootstrapService;

    public AuditChainBootstrapRunner(AuditChainBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.initializeAndVerify();
    }
}
