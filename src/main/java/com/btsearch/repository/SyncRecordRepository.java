package com.btsearch.repository;

import com.btsearch.model.entity.SyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {

    @Query(value = "SELECT * FROM torrent_sync_record ORDER BY last_sync_time DESC LIMIT 1", nativeQuery = true)
    SyncRecord getSyncRecord();

    @Modifying
    @Transactional
    @Query("UPDATE SyncRecord s SET s.maxSyncedId = :maxId, s.lastSyncTime = CURRENT_TIMESTAMP")
    void updateSyncRecord(@Param("maxId") Long maxId);
}
