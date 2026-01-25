package com.btsearch.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "torrent_sync_record")
public class SyncRecord {

    @Id
    private Long id = 1L;  // 固定主键，始终为 1

    @Column(name = "max_synced_id")
    private Long maxSyncedId;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    public SyncRecord() {
    }

    public SyncRecord(Long maxSyncedId, LocalDateTime lastSyncTime) {
        this.id = 1L;
        this.maxSyncedId = maxSyncedId;
        this.lastSyncTime = lastSyncTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaxSyncedId() {
        return maxSyncedId;
    }

    public void setMaxSyncedId(Long maxSyncedId) {
        this.maxSyncedId = maxSyncedId;
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
}
