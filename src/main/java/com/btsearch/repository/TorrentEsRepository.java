package com.btsearch.repository;

import com.btsearch.model.document.TorrentDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch种子Repository
 */
@Repository
public interface TorrentEsRepository extends ElasticsearchRepository<TorrentDocument, String> {

    /**
     * 根据infoHash查找
     */
    // Optional<TorrentDocument> findByInfoHash(String infoHash);

    /**
     * 自定义Repository实现类
     */
    class CustomTorrentEsRepository {

        private final ElasticsearchOperations operations;

        public CustomTorrentEsRepository(ElasticsearchOperations operations) {
            this.operations = operations;
        }

        /**
         * 多语言搜索
         */
        public SearchHits<TorrentDocument> multiLangSearch(Query query) {
            return operations.search(query, TorrentDocument.class);
        }

        /**
         * 批量索引
         */
        public void bulkIndex(List<TorrentDocument> documents) {
            if (documents.isEmpty()) {
                return;
            }

            operations.save(documents);
        }

        /**
         * 统计总数
         */
        public long count() {
            return operations.count(Query.findAll(), TorrentDocument.class);
        }
    }
}
