package com.btsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.btsearch.model.document.TorrentDocument;
import com.btsearch.model.dto.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    public SearchResult search(com.btsearch.model.dto.SearchRequest request) {
        long start = System.currentTimeMillis();

        try {
            SearchRequest esRequest = SearchRequest.of(s -> s
                    .index("torrents")
                    .from((request.getPage() - 1) * request.getLimitedSize())
                    .size(request.getLimitedSize())
                    .query(q -> q
                            .multiMatch(m -> m
                                    .query(request.getKeyword())
                                    .fields("name", "fileList.path^2")
                                    .type(TextQueryType.BestFields)
                            )
                    )
                    .highlight(h -> h
                            .preTags("<em>")
                            .postTags("</em>")
                            .fragmentSize(100)
                            .numberOfFragments(3)
                            .fields("name", f -> f
                                    .fragmentSize(150)
                                    .numberOfFragments(1)
                            )
                            .fields("fileList.path", f -> f
                                    .fragmentSize(80)
                                    .numberOfFragments(2)
                            )
                    )
                    .sort(srt -> {
                        // Determine sort field and order based on request
                        String sortField = request.getSort();
                        SortOrder sortOrder = "asc".equalsIgnoreCase(request.getOrder())
                                ? SortOrder.Asc
                                : SortOrder.Desc;

                        // Relevance sorting (default) - use score
                        if ("relevance".equals(sortField)) {
                            return srt.score(sc -> sc.order(sortOrder));
                        }
                        // Sort by seeders count
                        else if ("seeders".equals(sortField)) {
                            return srt.field(f -> f
                                    .field("seeders")
                                    .order(sortOrder)
                            );
                        }
                        // Sort by creation time
                        else if ("time".equals(sortField)) {
                            return srt.field(f -> f
                                    .field("createTime")
                                    .order(sortOrder)
                            );
                        }
                        // Sort by size
                        else if ("size".equals(sortField)) {
                            return srt.field(f -> f
                                    .field("size")
                                    .order(sortOrder)
                            );
                        }
                        // Default to relevance sorting
                        return srt.score(sc -> sc.order(sortOrder));
                    })
            );

            SearchResponse<TorrentDocument> response =
                    elasticsearchClient.search(esRequest, TorrentDocument.class);

            List<SearchResult.TorrentItem> items = response.hits().hits().stream()
                    .map(hit -> {
                        TorrentDocument doc = hit.source();

                        Map<String, List<String>> hl = hit.highlight();

                        String highlightedName = doc.getName();
                        if (hl != null && hl.containsKey("name")) {
                            highlightedName = hl.get("name").get(0);
                        }

                        List<SearchResult.FileItem> highlightedFiles = null;
                        if (hl != null && hl.containsKey("fileList.path")) {
                            highlightedFiles = hl.get("fileList.path").stream()
                                    .distinct()
                                    .limit(10)
                                    .map(p -> new SearchResult.FileItem(p, null))
                                    .toList();
                        }

                        return SearchResult.TorrentItem.builder()
                                .infoHash(doc.getInfoHash())
                                .name(doc.getName())
                                .highlightedName(highlightedName)
                                .size(doc.getSize())
                                .sizeFormatted(formatSize(doc.getSize()))
                                .seeders(doc.getSeeders())
                                .leechers(doc.getLeechers())
                                .peers(doc.getPeers())
                                .files(doc.getFileList() == null ? 0 : doc.getFileList().size())
                                .createTime(doc.getCreateTime())
                                .createTimeFormatted(formatTime(doc.getCreateTime()))
                                .magnetUri(doc.getMagnetUri())
                                .fileList(doc.getFileList() == null ? null :
                                        doc.getFileList().stream()
                                                .map(f -> new SearchResult.FileItem(f.getPath(), f.getSize()))
                                                .toList())
                                .highlightedFileList(highlightedFiles)
                                .build();
                    })
                    .collect(Collectors.toList());

            long total = response.hits().total().value();
            int totalPages = (int) Math.ceil((double) total / request.getLimitedSize());

            return SearchResult.builder()
                    .total(total)
                    .took(System.currentTimeMillis() - start)
                    .page(request.getPage())
                    .size(request.getLimitedSize())
                    .totalPages(totalPages)
                    .results(items)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("ES search failed", e);
        }
    }

    private String formatSize(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatTime(Long timestamp) {
        if (timestamp == null) return "未知";
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
