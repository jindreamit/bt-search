package com.btsearch.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import com.btsearch.model.document.TorrentDocument;
import com.btsearch.model.dto.SearchRequest;
import com.btsearch.model.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 多语言搜索服务
 * 根据查询关键词的语言类型，自动使用对应语言的分词器进行搜索
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    /**
     * 多语言搜索
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    public SearchResult search(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 检测搜索关键词的语言
            Set<LanguageDetectionService.Language> queryLanguages = languageDetectionService.detectLanguages(request.getKeyword());
            log.debug("Query language detected for '{}': {}", request.getKeyword(), queryLanguages);

            // 2. 构建ES查询
            NativeQuery query = buildSearchQuery(request, queryLanguages);

            // 3. 执行搜索 - 使用Sort处理排序
            Sort sort = buildSortWithNullHandling(request);
            Pageable pageable = PageRequest.of(
                    request.getPage() - 1,
                    request.getLimitedSize(),
                    sort
            );
            query.setPageable(pageable);

            SearchHits<TorrentDocument> searchHits = elasticsearchOperations.search(query, TorrentDocument.class);

            // 4. 构建结果
            long took = System.currentTimeMillis() - startTime;
            return buildSearchResult(searchHits, request, took, queryLanguages);

        } catch (Exception e) {
            log.error("Search failed for keyword: {}", request.getKeyword(), e);
            return SearchResult.empty();
        }
    }

    /**
     * 构建多语言搜索查询
     */
    private NativeQuery buildSearchQuery(SearchRequest request, Set<LanguageDetectionService.Language> queryLanguages) {
        // 使用multi_match查询，支持name和fileList.path字段搜索
        return NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(m -> m
                                .fields("name", "fileList.path^2")  // 搜索name和path，path权重更高
                                .query(request.getKeyword())
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                        )
                )
                .withTrackTotalHits(true)
                .build();
    }

    /**
     * 构建排序，处理null值
     */
    private Sort buildSortWithNullHandling(SearchRequest request) {
        String sortField = switch (request.getSort()) {
            case "time" -> "createTime";
            case "size" -> "size";
            default -> "seeders";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getOrder())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 使用_score作为默认排序（相关性排序）
        // 这样可以避免null值问题
        return Sort.by(direction, "_score");
    }

    /**
     * 构建搜索结果
     */
    private SearchResult buildSearchResult(SearchHits<TorrentDocument> searchHits,
                                           SearchRequest request,
                                           long took,
                                           Set<LanguageDetectionService.Language> queryLanguages) {
        List<SearchResult.TorrentItem> items = searchHits.getSearchHits().stream()
                .map(hit -> {
                    TorrentDocument doc = hit.getContent();

                    // 处理高亮
                    String highlightedName = doc.getName();
                    List<SearchResult.FileItem> highlightedFileItems = null;

                    if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                        // 获取name高亮
                        List<String> nameHighlights = hit.getHighlightFields().get("name");
                        if (nameHighlights != null && !nameHighlights.isEmpty()) {
                            highlightedName = String.join("...", nameHighlights);
                        }

                        // 获取fileList.path高亮
                        List<String> pathHighlights = hit.getHighlightFields().get("fileList.path");
                        if (pathHighlights != null && !pathHighlights.isEmpty()) {
                            highlightedFileItems = pathHighlights.stream()
                                    .map(h -> new SearchResult.FileItem(h, null))
                                    .toList();
                        }
                    }

                    // 转换文件列表
                    List<SearchResult.FileItem> fileItems = null;
                    if (doc.getFileList() != null && !doc.getFileList().isEmpty()) {
                        fileItems = doc.getFileList().stream()
                                .map(f -> new SearchResult.FileItem(f.getPath(), f.getSize()))
                                .toList();
                    }

                    return SearchResult.TorrentItem.builder()
                            .infoHash(doc.getInfoHash())
                            .name(doc.getName())
                            .size(doc.getSize())
                            .sizeFormatted(formatSize(doc.getSize()))
                            .seeders(doc.getSeeders())
                            .leechers(doc.getLeechers())
                            .peers(doc.getPeers())
                            .files(doc.getFiles())
                            .createTime(doc.getCreateTime())
                            .createTimeFormatted(formatTime(doc.getCreateTime()))
                            .detectedLanguages(doc.getDetectedLanguages())
                            .magnetUri(doc.getMagnetUri())
                            .fileList(fileItems)
                            .highlightedName(highlightedName)
                            .highlightedFileList(highlightedFileItems)
                            .build();
                })
                .collect(Collectors.toList());

        long total = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) total / request.getLimitedSize());

        return SearchResult.builder()
                .total(total)
                .took(took)
                .page(request.getPage())
                .size(request.getLimitedSize())
                .totalPages(totalPages)
                .results(items)
                .build();
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(Long bytes) {
        if (bytes == null) {
            return "0 B";
        }

        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else if (bytes < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        } else {
            return String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
        }
    }

    /**
     * 格式化时间
     */
    private String formatTime(Long timestamp) {
        if (timestamp == null) {
            return "未知";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
