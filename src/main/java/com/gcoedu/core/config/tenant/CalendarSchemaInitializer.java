package com.gcoedu.core.config.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Compatibiliza, de forma idempotente, os schemas legados da agenda.
 *
 * O Hibernate executa ddl-auto apenas para o tenant padrão durante o bootstrap.
 * Por isso, tabelas auxiliares já existentes em alguns municípios precisam ser
 * verificadas em todos os schemas antes de a API começar a atendê-los.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> schemas = jdbcTemplate.queryForList(
                """
                SELECT nspname
                  FROM pg_namespace
                 WHERE nspname = 'public' OR nspname LIKE 'city\\_%' ESCAPE '\\'
                 ORDER BY nspname
                """,
                String.class
        );
        for (String schema : schemas) {
            if (!schema.matches("^(public|city_[A-Za-z0-9_]+)$")) {
                log.warn("Schema ignorado na compatibilização da agenda: {}", schema);
                continue;
            }
            initializeSchema(schema);
        }
    }

    private void initializeSchema(String schema) {
        String qualifiedEvents = quote(schema) + ".calendar_events";
        Integer eventTableExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM information_schema.tables
                 WHERE table_schema = ? AND table_name = 'calendar_events'
                """,
                Integer.class,
                schema
        );
        if (eventTableExists == null || eventTableExists == 0) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS location VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS recurrence_rule VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(36)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS created_by_role VARCHAR(32)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS visibility_scope VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS municipality_id VARCHAR(36)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS school_id VARCHAR(36)");
        jdbcTemplate.execute("ALTER TABLE " + qualifiedEvents
                + " ADD COLUMN IF NOT EXISTS metadata_json JSON");

        String targets = quote(schema) + ".calendar_event_targets";
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(36) PRIMARY KEY,
                    event_id VARCHAR(36),
                    target_type VARCHAR(32) NOT NULL,
                    target_id VARCHAR(64),
                    target_filters JSON,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(targets));
        jdbcTemplate.execute("ALTER TABLE " + targets
                + " ADD COLUMN IF NOT EXISTS target_filters JSON");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS "
                + quote(indexName(schema, "calendar_targets_event_idx"))
                + " ON " + targets + " (event_id)");

        String resources = quote(schema) + ".calendar_event_resources";
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(36) PRIMARY KEY,
                    event_id VARCHAR(36),
                    resource_type VARCHAR(16) NOT NULL,
                    title VARCHAR(160) NOT NULL,
                    url VARCHAR(2048),
                    minio_bucket VARCHAR(100),
                    minio_object_name VARCHAR(1024),
                    original_filename VARCHAR(255),
                    content_type VARCHAR(160),
                    size_bytes BIGINT,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(resources));
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS "
                + quote(indexName(schema, "calendar_resources_event_idx"))
                + " ON " + resources + " (event_id)");
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String indexName(String schema, String suffix) {
        String value = (schema + "_" + suffix).replaceAll("[^A-Za-z0-9_]", "_");
        return value.length() <= 63 ? value : value.substring(0, 63);
    }
}
