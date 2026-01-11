package com.btsearch.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 多语言检测服务
 * 使用正则快速检测常见语言（中日韩俄），支持混合语言
 */
@Service
public class LanguageDetectionService {

    private static final Logger log = LoggerFactory.getLogger(LanguageDetectionService.class);

    // 语言检测结果缓存（种子名称 -> 语言列表）
    private final Cache<String, Set<Language>> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    /**
     * 检测种子名称的语言类型（支持混合语言）
     *
     * @param torrentName 种子名称
     * @return 检测到的所有语言集合
     */
    public Set<Language> detectLanguages(String torrentName) {
        if (torrentName == null || torrentName.isEmpty()) {
            return Set.of(Language.ENGLISH);
        }

        // 先从缓存获取
        return cache.get(torrentName, this::detectLanguagesInternal);
    }

    /**
     * 内部语言检测逻辑
     */
    private Set<Language> detectLanguagesInternal(String torrentName) {
        Set<Language> languages = new HashSet<>();

        // 1. 使用正则快速判断中文（优先检测，范围大）
        if (containsChinese(torrentName)) {
            languages.add(Language.CHINESE);
        }

        // 2. 检测日文（平假名/片假名）
        if (containsJapanese(torrentName)) {
            languages.add(Language.JAPANESE);
        }

        // 3. 检测韩文（Hangul）
        if (containsKorean(torrentName)) {
            languages.add(Language.KOREAN);
        }

        // 4. 检测西里尔字母（俄语等）
        if (containsCyrillic(torrentName)) {
            languages.add(Language.RUSSIAN);
        }

        // 5. 检测阿拉伯文
        if (containsArabic(torrentName)) {
            languages.add(Language.ARABIC);
        }

        // 6. 检测泰文
        if (containsThai(torrentName)) {
            languages.add(Language.THAI);
        }

        // 7. 检测越南文
        if (containsVietnamese(torrentName)) {
            languages.add(Language.VIETNAMESE);
        }

        // 如果没有检测到特定语言，默认英语
        if (languages.isEmpty()) {
            languages.add(Language.ENGLISH);
        }

        log.debug("Language detection for '{}': {}", torrentName, languages);
        return languages;
    }

    // ========== 正则快速检测方法 ==========

    /**
     * 检测是否包含中文字符
     * 中文范围：U+4E00-U+9FFF (CJK统一汉字)
     */
    private boolean containsChinese(String text) {
        return text.matches(".*\\p{script=Han}.*");
    }

    /**
     * 检测是否包含日文（平假名/片假名）
     * 平假名：U+3040-U+309F
     * 片假名：U+30A0-U+30FF
     */
    private boolean containsJapanese(String text) {
        // 检测平假名或片假名
        return text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF].*");
    }

    /**
     * 检测是否包含韩文（Hangul）
     * 韩文范围：U+AC00-U+D7AF
     */
    private boolean containsKorean(String text) {
        return text.matches(".*[\\uAC00-\\uD7AF].*");
    }

    /**
     * 检测是否包含西里尔字母（俄语等）
     * 西里尔字母：U+0400-U+04FF
     */
    private boolean containsCyrillic(String text) {
        return text.matches(".*\\p{script=Cyrillic}.*");
    }

    /**
     * 检测是否包含阿拉伯文
     * 阿拉伯文：U+0600-U+06FF
     */
    private boolean containsArabic(String text) {
        return text.matches(".*[\\u0600-\\u06FF].*");
    }

    /**
     * 检测是否包含泰文
     * 泰文：U+0E00-U+0E7F
     */
    private boolean containsThai(String text) {
        return text.matches(".*[\\u0E00-\\u0E7F].*");
    }

    /**
     * 检测是否包含越南文（包含带声调的拉丁字母）
     */
    private boolean containsVietnamese(String text) {
        // 越南文使用拉丁字母扩展，检测特殊越南文字符
        return text.matches(".*[ăâêôơưăâêôơưÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơĂÂĐÊÔƠàâêô].*");
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.invalidateAll();
        log.info("Language detection cache cleared");
    }

    /**
     * 获取缓存大小
     */
    public long getCacheSize() {
        return cache.estimatedSize();
    }

    // ========== 语言枚举 ==========

    /**
     * 支持的语言类型
     */
    public enum Language {
        CHINESE("zh", "chinese", "chinese"),
        JAPANESE("ja", "japanese", "japanese"),
        KOREAN("ko", "korean", "korean"),
        RUSSIAN("ru", "russian", "russian"),
        ENGLISH("en", "english", "standard_analyzer"),
        ARABIC("ar", "arabic", "standard_analyzer"),
        THAI("th", "thai", "standard_analyzer"),
        VIETNAMESE("vi", "vietnamese", "standard_analyzer");

        private final String code;          // ISO 639-1 语言代码
        private final String esAnalyzer;    // Elasticsearch 分析器名称
        private final String fieldName;     // ES 多语言字段后缀

        Language(String code, String esAnalyzer, String fieldName) {
            this.code = code;
            this.esAnalyzer = esAnalyzer;
            this.fieldName = fieldName;
        }

        public String getCode() {
            return code;
        }

        public String getEsAnalyzer() {
            return esAnalyzer;
        }

        public String getFieldName() {
            return fieldName;
        }

        /**
         * 根据代码获取语言枚举
         */
        public static Language fromCode(String code) {
            for (Language lang : values()) {
                if (lang.code.equalsIgnoreCase(code)) {
                    return lang;
                }
            }
            return ENGLISH; // 默认返回英语
        }
    }
}
