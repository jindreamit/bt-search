package com.btsearch.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 搜索请求DTO
 */
public class SearchRequest {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "关键词不能为空")
    private String keyword;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 20;

    /**
     * 排序字段：relevance, seeders, time, size
     */
    private String sort = "relevance";

    /**
     * 排序方向：asc, desc
     */
    private String order = "desc";

    /**
     * 最小文件大小（字节）
     */
    private Long minSize;

    /**
     * 最大文件大小（字节）
     */
    private Long maxSize;

    /**
     * 最小种子数
     */
    private Integer minSeeders;

    /**
     * 最早时间戳（毫秒）
     */
    private Long afterTime;

    /**
     * 语言过滤
     * zh=中文, ja=日文, ko=韩文, ru=俄文, en=英文
     */
    private String lang;

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Long getMinSize() {
        return minSize;
    }

    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public Integer getMinSeeders() {
        return minSeeders;
    }

    public void setMinSeeders(Integer minSeeders) {
        this.minSeeders = minSeeders;
    }

    public Long getAfterTime() {
        return afterTime;
    }

    public void setAfterTime(Long afterTime) {
        this.afterTime = afterTime;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * 获取限制后的size（最大100）
     */
    public int getLimitedSize() {
        return Math.min(Math.max(size, 1), 100);
    }
}
