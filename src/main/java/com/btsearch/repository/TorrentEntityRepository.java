package com.btsearch.repository;

import com.btsearch.model.entity.TorrentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MySQL种子Repository
 */
@Repository
public interface TorrentEntityRepository extends JpaRepository<TorrentEntity, Integer> {

    /**
     * 根据infoHash查找（需要转为十六进制字符串比较）
     * 注意：由于MySQL存储的是binary(20)，需要使用函数查询
     */
    // Optional<TorrentEntity> findByInfohash(byte[] infohash);

    /**
     * 查询总数
     */
    // Long countByInfohashIsNull();
}
