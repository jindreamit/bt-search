package com.btsearch.model.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * MySQL种子实体
 * 对应 info_dict 表
 */
@Data
@Entity
@Table(name = "info_dict", schema = "forge")
public class TorrentEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * InfoHash（二进制，20字节）
     */
    @Column(name = "infohash", columnDefinition = "binary(20)", nullable = false, unique = true)
    private byte[] infohash;

    /**
     * 种子字典数据（bencode格式）
     */
    @Column(name = "infohashDictionary", columnDefinition = "longblob", nullable = false)
    private byte[] infohashDictionary;

    /**
     * 发现时间（Unix时间戳，毫秒）
     */
    @Column(name = "find_time")
    private Long findTime;

    /**
     * 做种者数量
     */
    @Column(name = "seeders")
    private Integer seeders = 0;

    /**
     * 下载者数量
     */
    @Column(name = "leechers")
    private Integer leechers = 0;

    /**
     * 连接数
     */
    @Column(name = "peers")
    private Integer peers = 0;

    /**
     * 语言标识
     * 1=含中文，0=其他
     */
    @Column(name = "lang")
    private Byte lang = 0;
}
