package com.btsearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JdbcTemplateService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> queryForList(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args);
    }

    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        return jdbcTemplate.queryForObject(sql, requiredType, args);
    }
}
