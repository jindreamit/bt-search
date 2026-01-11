package com.btsearch.model.dto;

import java.util.List;

/**
 * 搜索结果DTO
 */
public class SearchResult {

    /**
     * 总结果数
     */
    private long total;

    /**
     * 查询耗时（毫秒）
     */
    private long took;

    /**
     * 当前页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 结果列表
     */
    private List<TorrentItem> results;

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<TorrentItem> getResults() {
        return results;
    }

    public void setResults(List<TorrentItem> results) {
        this.results = results;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long total;
        private long took;
        private int page;
        private int size;
        private int totalPages;
        private List<TorrentItem> results;

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public Builder took(long took) {
            this.took = took;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder results(List<TorrentItem> results) {
            this.results = results;
            return this;
        }

        public SearchResult build() {
            SearchResult result = new SearchResult();
            result.total = this.total;
            result.took = this.took;
            result.page = this.page;
            result.size = this.size;
            result.totalPages = this.totalPages;
            result.results = this.results;
            return result;
        }
    }

    /**
     * 种子项
     */
    public static class TorrentItem {
        /**
         * InfoHash
         */
        private String infoHash;

        /**
         * 种子名称
         */
        private String name;

        /**
         * 总大小（字节）
         */
        private Long size;

        /**
         * 格式化大小（如 1.5 GB）
         */
        private String sizeFormatted;

        /**
         * 做种者数量
         */
        private Integer seeders;

        /**
         * 下载者数量
         */
        private Integer leechers;

        /**
         * 连接数
         */
        private Integer peers;

        /**
         * 文件数量
         */
        private Integer files;

        /**
         * 创建时间（Unix时间戳，毫秒）
         */
        private Long createTime;

        /**
         * 格式化时间
         */
        private String createTimeFormatted;

        /**
         * 检测到的语言
         */
        private List<String> detectedLanguages;

        /**
         * 磁力链接
         */
        private String magnetUri;

        /**
         * 文件列表
         */
        private List<FileItem> fileList;

        /**
         * 高亮的名称
         */
        private String highlightedName;

        /**
         * 高亮的文件列表
         */
        private List<FileItem> highlightedFileList;

        // Getters and Setters
        public String getInfoHash() {
            return infoHash;
        }

        public void setInfoHash(String infoHash) {
            this.infoHash = infoHash;
        }

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

        public String getSizeFormatted() {
            return sizeFormatted;
        }

        public void setSizeFormatted(String sizeFormatted) {
            this.sizeFormatted = sizeFormatted;
        }

        public Integer getSeeders() {
            return seeders;
        }

        public void setSeeders(Integer seeders) {
            this.seeders = seeders;
        }

        public Integer getLeechers() {
            return leechers;
        }

        public void setLeechers(Integer leechers) {
            this.leechers = leechers;
        }

        public Integer getPeers() {
            return peers;
        }

        public void setPeers(Integer peers) {
            this.peers = peers;
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

        public String getCreateTimeFormatted() {
            return createTimeFormatted;
        }

        public void setCreateTimeFormatted(String createTimeFormatted) {
            this.createTimeFormatted = createTimeFormatted;
        }

        public List<String> getDetectedLanguages() {
            return detectedLanguages;
        }

        public void setDetectedLanguages(List<String> detectedLanguages) {
            this.detectedLanguages = detectedLanguages;
        }

        public String getMagnetUri() {
            return magnetUri;
        }

        public void setMagnetUri(String magnetUri) {
            this.magnetUri = magnetUri;
        }

        public List<FileItem> getFileList() {
            return fileList;
        }

        public void setFileList(List<FileItem> fileList) {
            this.fileList = fileList;
        }

        public String getHighlightedName() {
            return highlightedName;
        }

        public void setHighlightedName(String highlightedName) {
            this.highlightedName = highlightedName;
        }

        public List<FileItem> getHighlightedFileList() {
            return highlightedFileList;
        }

        public void setHighlightedFileList(List<FileItem> highlightedFileList) {
            this.highlightedFileList = highlightedFileList;
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String infoHash;
            private String name;
            private Long size;
            private String sizeFormatted;
            private Integer seeders;
            private Integer leechers;
            private Integer peers;
            private Integer files;
            private Long createTime;
            private String createTimeFormatted;
            private List<String> detectedLanguages;
            private String magnetUri;
            private List<FileItem> fileList;
            private String highlightedName;
            private List<FileItem> highlightedFileList;

            public Builder infoHash(String infoHash) {
                this.infoHash = infoHash;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder size(Long size) {
                this.size = size;
                return this;
            }

            public Builder sizeFormatted(String sizeFormatted) {
                this.sizeFormatted = sizeFormatted;
                return this;
            }

            public Builder seeders(Integer seeders) {
                this.seeders = seeders;
                return this;
            }

            public Builder leechers(Integer leechers) {
                this.leechers = leechers;
                return this;
            }

            public Builder peers(Integer peers) {
                this.peers = peers;
                return this;
            }

            public Builder files(Integer files) {
                this.files = files;
                return this;
            }

            public Builder createTime(Long createTime) {
                this.createTime = createTime;
                return this;
            }

            public Builder createTimeFormatted(String createTimeFormatted) {
                this.createTimeFormatted = createTimeFormatted;
                return this;
            }

            public Builder detectedLanguages(List<String> detectedLanguages) {
                this.detectedLanguages = detectedLanguages;
                return this;
            }

            public Builder magnetUri(String magnetUri) {
                this.magnetUri = magnetUri;
                return this;
            }

            public Builder fileList(List<FileItem> fileList) {
                this.fileList = fileList;
                return this;
            }

            public Builder highlightedName(String highlightedName) {
                this.highlightedName = highlightedName;
                return this;
            }

            public Builder highlightedFileList(List<FileItem> highlightedFileList) {
                this.highlightedFileList = highlightedFileList;
                return this;
            }

            public TorrentItem build() {
                TorrentItem item = new TorrentItem();
                item.infoHash = this.infoHash;
                item.name = this.name;
                item.size = this.size;
                item.sizeFormatted = this.sizeFormatted;
                item.seeders = this.seeders;
                item.leechers = this.leechers;
                item.peers = this.peers;
                item.files = this.files;
                item.createTime = this.createTime;
                item.createTimeFormatted = this.createTimeFormatted;
                item.detectedLanguages = this.detectedLanguages;
                item.magnetUri = this.magnetUri;
                item.fileList = this.fileList;
                item.highlightedName = this.highlightedName;
                item.highlightedFileList = this.highlightedFileList;
                return item;
            }
        }
    }

    /**
     * 空结果
     */
    public static SearchResult empty() {
        return SearchResult.builder()
                .total(0)
                .took(0)
                .page(1)
                .size(20)
                .totalPages(0)
                .results(List.of())
                .build();
    }

    /**
     * 文件项
     */
    public static class FileItem {
        /**
         * 文件路径
         */
        private String path;

        /**
         * 文件大小（字节）
         */
        private Long size;

        public FileItem() {
        }

        public FileItem(String path, Long size) {
            this.path = path;
            this.size = size;
        }

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
