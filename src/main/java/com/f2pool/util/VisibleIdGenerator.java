package com.f2pool.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VisibleIdGenerator {
    // Only use for user-visible business records such as user/order/recharge/withdraw/address ids.
    // Do not apply to chat messages, admin tables, or internal relation tables unless explicitly required.
    private static final int MIN_STEP = 10;
    private static final int MAX_STEP = 50;

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    public VisibleIdGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long nextId(String tableName) {
        String safeTable = normalizeTableName(tableName);
        String lockName = "visible_id:" + safeTable;
        Boolean locked = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 5)", Boolean.class, lockName);
        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalStateException("failed to acquire id generation lock for table " + safeTable);
        }
        try {
            Long maxId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM " + safeTable,
                    Long.class
            );
            long base = maxId == null ? 0L : maxId;
            return base + randomStep();
        } finally {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Long.class, lockName);
        }
    }

    private int randomStep() {
        return random.nextInt(MAX_STEP - MIN_STEP + 1) + MIN_STEP;
    }

    private String normalizeTableName(String tableName) {
        if (tableName == null || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid table name");
        }
        return tableName;
    }
}
