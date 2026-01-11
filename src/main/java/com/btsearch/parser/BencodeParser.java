package com.btsearch.parser;

import com.btsearch.model.dto.TorrentMetadata;
import com.btsearch.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bencode格式解析器
 * <p>
 * Bencode格式规范：
 * - 整数: i123e
 * - 字符串: 4:spam
 * - 列表: l4:spam4:eggse
 * - 字典: d3:cow3:moo4:spam4:eggse
 * <p>
 * 支持解析BitTorrent种子文件并提取元数据
 */
@Component
public class BencodeParser {

    private static final Logger log = LoggerFactory.getLogger(BencodeParser.class);

    @Autowired
    private LanguageDetectionService languageDetectionService;

    private byte[] data;
    private int pos;

    /**
     * 解析bencode字节数组
     *
     * @param bencodeData bencode格式的字节数组
     * @return 解析后的种子元数据
     * @throws BencodeParseException 解析失败时抛出
     */
    public TorrentMetadata parse(byte[] bencodeData) {
        if (bencodeData == null || bencodeData.length == 0) {
            throw new BencodeParseException("Empty bencode data");
        }

        this.data = bencodeData;
        this.pos = 0;

        try {
            // 解析根字典
            @SuppressWarnings("unchecked")
            Map<String, Object> rootDict = (Map<String, Object>) decode();

            // 提取元数据
            return extractMetadata(rootDict);
        } catch (Exception e) {
            log.error("Failed to parse bencode data", e);
            throw new BencodeParseException("Parse failed: " + e.getMessage(), e);
        }
    }

    /**
     * 提取种子元数据
     */
    private TorrentMetadata extractMetadata(Map<String, Object> rootDict) {
        TorrentMetadata metadata = new TorrentMetadata();

        // 检查根字典是否直接包含torrent字段（说明data就是info字典）
        Map<String, Object> dict = rootDict;
        if (rootDict.containsKey("name") || rootDict.containsKey("length") || rootDict.containsKey("files")) {
            // 根字典本身就是info字典
            log.debug("Root dict appears to be info dict directly");
        } else {
            // 根字典包含info键
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) rootDict.get("info");
            if (info == null) {
                throw new BencodeParseException("Missing 'info' dictionary");
            }
            dict = info;
        }

        // 提取名称
        if (dict.get("name") instanceof byte[]) {
            byte[] nameBytes = (byte[]) dict.get("name");
            metadata.setName(decodeString(nameBytes));
        } else if (dict.get("name") instanceof String) {
            metadata.setName((String) dict.get("name"));
        }

        // 提取长度（单文件）
        if (dict.containsKey("length")) {
            Object length = dict.get("length");
            if (length instanceof Long) {
                metadata.setSize((Long) length);
            } else if (length instanceof Integer) {
                metadata.setSize(((Integer) length).longValue());
            }
        }

        // 提取文件列表（多文件）
        if (dict.containsKey("files")) {
            Object files = dict.get("files");
            if (files instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> filesList = (List<Map<String, Object>>) files;
                List<TorrentMetadata.FileInfo> fileList = new ArrayList<>();
                long totalSize = 0;

                for (Map<String, Object> fileDict : filesList) {
                    TorrentMetadata.FileInfo fileInfo = new TorrentMetadata.FileInfo();

                    // 提取路径
                    if (fileDict.containsKey("path")) {
                        Object path = fileDict.get("path");
                        if (path instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> pathList = (List<Object>) path;
                            String pathStr = pathList.stream()
                                    .map(p -> {
                                        if (p instanceof byte[]) {
                                            return decodeString((byte[]) p);
                                        } else if (p instanceof String) {
                                            return (String) p;
                                        } else {
                                            return String.valueOf(p);
                                        }
                                    })
                                    .collect(Collectors.joining("/"));
                            fileInfo.setPath(pathStr);
                        }
                    }

                    // 提取文件大小
                    if (fileDict.containsKey("length")) {
                        Object length = fileDict.get("length");
                        if (length instanceof Long) {
                            fileInfo.setSize((Long) length);
                            totalSize += (Long) length;
                        } else if (length instanceof Integer) {
                            long len = ((Integer) length).longValue();
                            fileInfo.setSize(len);
                            totalSize += len;
                        }
                    }

                    fileList.add(fileInfo);
                }

                metadata.setFileList(fileList);
                metadata.setFiles(fileList.size());
                metadata.setSize(totalSize);
            }
        }

        // 提取piece长度
        if (dict.containsKey("piece length")) {
            Object pieceLength = dict.get("piece length");
            if (pieceLength instanceof Long) {
                metadata.setPieceLength((Long) pieceLength);
            } else if (pieceLength instanceof Integer) {
                metadata.setPieceLength(((Integer) pieceLength).longValue());
            }
        }

        // 提取pieces数量
        if (dict.containsKey("pieces")) {
            byte[] pieces = (byte[]) dict.get("pieces");
            if (pieces != null) {
                metadata.setPieces(pieces.length / 20); // SHA1 hash is 20 bytes
            }
        }

        // 提取创建时间
        if (rootDict.containsKey("creation date")) {
            Object creationDate = rootDict.get("creation date");
            if (creationDate instanceof Long) {
                metadata.setCreateTime((Long) creationDate);
            }
        }

        // 提取评论
        if (rootDict.get("comment") instanceof byte[]) {
            metadata.setComment(decodeString((byte[]) rootDict.get("comment")));
        }

        // 提取创建者
        if (rootDict.get("created by") instanceof byte[]) {
            metadata.setCreatedBy(decodeString((byte[]) rootDict.get("created by")));
        }

        // 提取编码
        if (rootDict.get("encoding") instanceof byte[]) {
            metadata.setEncoding(decodeString((byte[]) rootDict.get("encoding")));
        }

        // 实时检测语言
        if (metadata.getName() != null) {
            Set<LanguageDetectionService.Language> languages =
                    languageDetectionService.detectLanguages(metadata.getName());
            metadata.setDetectedLanguages(languages);
        }

        log.debug("Parsed torrent: {}, size: {}, files: {}, languages: {}",
                metadata.getName(), metadata.getSize(), metadata.getFiles(),
                metadata.getDetectedLanguages());

        return metadata;
    }

    /**
     * 解码bencode对象
     */
    private Object decode() {
        if (pos >= data.length) {
            throw new BencodeParseException("Unexpected end of data");
        }

        byte c = data[pos++];

        if (c == 'i') {
            // 整数: i123e
            return decodeInt();
        } else if (c == 'l') {
            // 列表: l4:spam4:eggse
            return decodeList();
        } else if (c == 'd') {
            // 字典: d3:cow3:moo4:spam4:eggse
            return decodeDict();
        } else if (c >= '0' && c <= '9') {
            // 字符串: 4:spam
            pos--;  // 回退，让decodeString处理
            return decodeString();
        }

        throw new BencodeParseException("Invalid bencode format at position " + (pos - 1));
    }

    /**
     * 解码整数: i123e
     */
    private Long decodeInt() {
        StringBuilder sb = new StringBuilder();

        while (pos < data.length && data[pos] != 'e') {
            sb.append((char) data[pos++]);
        }

        if (pos >= data.length) {
            throw new BencodeParseException("Unterminated integer");
        }

        pos++;  // 跳过'e'

        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            throw new BencodeParseException("Invalid integer: " + sb, e);
        }
    }

    /**
     * 解码字符串: 4:spam
     */
    private byte[] decodeStringBytes() {
        // 读取长度
        StringBuilder length = new StringBuilder();
        while (pos < data.length && data[pos] != ':') {
            length.append((char) data[pos++]);
        }

        if (pos >= data.length) {
            throw new BencodeParseException("Unterminated string length");
        }

        pos++;  // 跳过':'

        int len;
        try {
            len = Integer.parseInt(length.toString());
        } catch (NumberFormatException e) {
            throw new BencodeParseException("Invalid string length: " + length, e);
        }

        if (len < 0 || len > 10 * 1024 * 1024) {  // 限制最大10MB
            throw new BencodeParseException("String length out of bounds: " + len);
        }

        if (pos + len > data.length) {
            throw new BencodeParseException("String exceeds data length");
        }

        byte[] str = new byte[len];
        System.arraycopy(data, pos, str, 0, len);
        pos += len;

        return str;
    }

    /**
     * 解码字符串为String对象
     */
    private String decodeString() {
        byte[] bytes = decodeStringBytes();
        return decodeString(bytes);
    }

    /**
     * 将字节数组解码为字符串
     */
    private String decodeString(byte[] bytes) {
        try {
            // 尝试UTF-8解码
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果UTF-8失败，使用ISO-8859-1
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * 解码列表: l4:spam4:eggse
     */
    private List<Object> decodeList() {
        List<Object> list = new ArrayList<>();

        while (pos < data.length && data[pos] != 'e') {
            list.add(decode());
        }

        if (pos >= data.length) {
            throw new BencodeParseException("Unterminated list");
        }

        pos++;  // 跳过'e'

        return list;
    }

    /**
     * 解码字典: d3:cow3:moo4:spam4:eggse
     */
    private Map<String, Object> decodeDict() {
        Map<String, Object> dict = new HashMap<>();

        while (pos < data.length && data[pos] != 'e') {
            // Key必须是字符串
            byte[] keyBytes = decodeStringBytes();
            String key = decodeString(keyBytes);

            // Value
            Object value = decode();

            dict.put(key, value);
        }

        if (pos >= data.length) {
            throw new BencodeParseException("Unterminated dictionary");
        }

        pos++;  // 跳过'e'

        return dict;
    }

    /**
     * Bencode解析异常
     */
    public static class BencodeParseException extends RuntimeException {
        public BencodeParseException(String message) {
            super(message);
        }

        public BencodeParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
