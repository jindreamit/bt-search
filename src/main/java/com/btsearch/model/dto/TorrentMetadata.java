package com.btsearch.model.dto;

import com.btsearch.service.LanguageDetectionService;

import java.util.List;
import java.util.Set;

/**
 * 种子元数据（从bencode解析后）
 */
public class TorrentMetadata {

    /**
     * 种子名称
     */
    private String name;

    /**
     * 总大小（字节）
     */
    private Long size;

    /**
     * 文件数量
     */
    private Integer files;

    /**
     * 创建时间（Unix时间戳，秒）
     */
    private Long createTime;

    /**
     * 文件列表（多文件种子）
     */
    private List<FileInfo> fileList;

    /**
     * 检测到的语言
     */
    private Set<LanguageDetectionService.Language> detectedLanguages;

    /**
     * Piece长度
     */
    private Long pieceLength;

    /**
     * Piece数量
     */
    private Integer pieces;

    /**
     * 评论
     */
    private String comment;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 编码
     */
    private String encoding;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getFiles() {
        return files;
    }

    public void setFiles(Integer files) {
        this.files = files;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public List<FileInfo> getFileList() {
        return fileList;
    }

    public void setFileList(List<FileInfo> fileList) {
        this.fileList = fileList;
    }

    public Set<LanguageDetectionService.Language> getDetectedLanguages() {
        return detectedLanguages;
    }

    public void setDetectedLanguages(Set<LanguageDetectionService.Language> detectedLanguages) {
        this.detectedLanguages = detectedLanguages;
    }

    public Long getPieceLength() {
        return pieceLength;
    }

    public void setPieceLength(Long pieceLength) {
        this.pieceLength = pieceLength;
    }

    public Integer getPieces() {
        return pieces;
    }

    public void setPieces(Integer pieces) {
        this.pieces = pieces;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 文件信息
     */
    public static class FileInfo {
        /**
         * 文件路径
         */
        private String path;

        /**
         * 文件大小（字节）
         */
        private Long size;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }
    }
}
