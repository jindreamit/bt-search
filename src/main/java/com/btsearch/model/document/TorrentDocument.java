package com.btsearch.model.document;

import com.btsearch.model.dto.TorrentMetadata;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

/**
 * Elasticsearch种子文档
 * 支持多语言搜索
 */
@Document(indexName = "torrents")
@Setting(settingPath = "elasticsearch/mapping.json", refreshInterval = "30s")
public class TorrentDocument {

    /**
     * 文档ID（使用infoHash）
     */
    @Id
    private String id;

    /**
     * InfoHash（种子唯一标识）
     */
    @Field(type = FieldType.Keyword)
    private String infoHash;

    /**
     * 种子名称
     * 支持多语言分词（中文IK、日文Kuromoji、韩文Nori、俄文Russian）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    /**
     * 检测到的语言列表
     */
    @Field(type = FieldType.Keyword)
    private List<String> detectedLanguages;

    /**
     * 总大小（字节）
     */
    @Field(type = FieldType.Long)
    private Long size;

    /**
     * 做种者数量
     */
    @Field(type = FieldType.Integer)
    private Integer seeders;

    /**
     * 下载者数量
     */
    @Field(type = FieldType.Integer)
    private Integer leechers;

    /**
     * 连接数
     */
    @Field(type = FieldType.Integer)
    private Integer peers;

    /**
     * 文件数量
     */
    @Field(type = FieldType.Integer)
    private Integer files;

    /**
     * 创建时间（Unix时间戳，毫秒）
     */
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Long createTime;

    /**
     * 语言标识（MySQL原始字段）
     * 1=含中文，0=其他
     */
    @Field(type = FieldType.Byte)
    private Byte lang;

    /**
     * 文件列表
     */
    @Field(type = FieldType.Nested)
    private List<FileInfo> fileList;

    /**
     * 磁力链接URI
     */
    @Field(type = FieldType.Keyword, index = false)
    private String magnetUri;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public List<String> getDetectedLanguages() {
        return detectedLanguages;
    }

    public void setDetectedLanguages(List<String> detectedLanguages) {
        this.detectedLanguages = detectedLanguages;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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

    public Byte getLang() {
        return lang;
    }

    public void setLang(Byte lang) {
        this.lang = lang;
    }

    public List<FileInfo> getFileList() {
        return fileList;
    }

    public void setFileList(List<FileInfo> fileList) {
        this.fileList = fileList;
    }

    public String getMagnetUri() {
        return magnetUri;
    }

    public void setMagnetUri(String magnetUri) {
        this.magnetUri = magnetUri;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String infoHash;
        private String name;
        private List<String> detectedLanguages;
        private Long size;
        private Integer seeders;
        private Integer leechers;
        private Integer peers;
        private Integer files;
        private Long createTime;
        private Byte lang;
        private List<FileInfo> fileList;
        private String magnetUri;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder infoHash(String infoHash) {
            this.infoHash = infoHash;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder detectedLanguages(List<String> detectedLanguages) {
            this.detectedLanguages = detectedLanguages;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
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

        public Builder lang(Byte lang) {
            this.lang = lang;
            return this;
        }

        public Builder fileList(List<FileInfo> fileList) {
            this.fileList = fileList;
            return this;
        }

        public Builder magnetUri(String magnetUri) {
            this.magnetUri = magnetUri;
            return this;
        }

        public TorrentDocument build() {
            TorrentDocument doc = new TorrentDocument();
            doc.id = this.id;
            doc.infoHash = this.infoHash;
            doc.name = this.name;
            doc.detectedLanguages = this.detectedLanguages;
            doc.size = this.size;
            doc.seeders = this.seeders;
            doc.leechers = this.leechers;
            doc.peers = this.peers;
            doc.files = this.files;
            doc.createTime = this.createTime;
            doc.lang = this.lang;
            doc.fileList = this.fileList;
            doc.magnetUri = this.magnetUri;
            return doc;
        }
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

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String path;
            private Long size;

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public Builder size(Long size) {
                this.size = size;
                return this;
            }

            public FileInfo build() {
                FileInfo info = new FileInfo();
                info.path = this.path;
                info.size = this.size;
                return info;
            }
        }
    }

    /**
     * 从TorrentMetadata构建TorrentDocument
     */
    public static TorrentDocument fromMetadata(TorrentMetadata metadata, String infoHash) {
        TorrentDocument.Builder builder = TorrentDocument.builder()
                .id(infoHash)
                .infoHash(infoHash)
                .name(metadata.getName())
                .size(metadata.getSize())
                .files(metadata.getFiles())
                .createTime(metadata.getCreateTime() != null
                        ? metadata.getCreateTime() * 1000  // 转换为毫秒
                        : null);

        // 设置语言列表
        if (metadata.getDetectedLanguages() != null) {
            builder.detectedLanguages(
                    metadata.getDetectedLanguages().stream()
                            .map(lang -> lang.getCode())
                            .toList()
            );
        }

        // 设置文件列表
        if (metadata.getFileList() != null) {
            List<FileInfo> files = metadata.getFileList().stream()
                    .map(f -> FileInfo.builder()
                            .path(f.getPath())
                            .size(f.getSize())
                            .build())
                    .toList();
            builder.fileList(files);
        }

        // 生成磁力链接
        builder.magnetUri(buildMagnetUri(infoHash, metadata.getName()));

        return builder.build();
    }

    /**
     * 构建磁力链接
     */
    private static String buildMagnetUri(String infoHash, String name) {
        StringBuilder magnet = new StringBuilder("magnet:?xt=urn:btih:");
        magnet.append(infoHash);

        if (name != null && !name.isEmpty()) {
            try {
                magnet.append("&dn=").append(java.net.URLEncoder.encode(name, "UTF-8"));
            } catch (Exception e) {
                // 忽略编码错误
            }
        }

        return magnet.toString();
    }
}
