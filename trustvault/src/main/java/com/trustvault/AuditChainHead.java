package com.trustvault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_chain_head")
public class AuditChainHead {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "last_entry_hash", nullable = false, length = 64)
    private String lastEntryHash;

    @Column(name = "entry_count", nullable = false)
    private long entryCount;

    @Column(name = "head_hash", nullable = false, length = 64)
    private String headHash;

    @Column(name = "initialized", nullable = false)
    private boolean initialized;

    protected AuditChainHead() {
    }

    public AuditChainHead(String genesisHash, String genesisHeadHash) {
        this.id = SINGLETON_ID;
        this.lastEntryHash = genesisHash;
        this.entryCount = 0;
        this.headHash = genesisHeadHash;
        this.initialized = false;
    }

    public String getLastEntryHash() {
        return lastEntryHash;
    }

    public long getEntryCount() {
        return entryCount;
    }

    public String getHeadHash() {
        return headHash;
    }

    public boolean isInitialized() {
        return initialized;
    }

    void advance(String entryHash, long newCount, String newHeadHash) {
        this.lastEntryHash = entryHash;
        this.entryCount = newCount;
        this.headHash = newHeadHash;
        this.initialized = true;
    }

    void initialize(String entryHash, long count, String newHeadHash) {
        advance(entryHash, count, newHeadHash);
    }
}
