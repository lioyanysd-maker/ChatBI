package com.chatbi.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.DataSourceEntity;
import com.chatbi.entity.TableWhitelist;
import com.chatbi.mapper.TableWhitelistMapper;
import com.chatbi.util.ColumnLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final JdbcTemplate jdbcTemplate;
    private final TableWhitelistMapper tableWhitelistMapper;
    private final DataSourceService dataSourceService;
    private final ColumnSemanticService columnSemanticService;

    public Set<String> getAllowedTableNames(Long dataSourceId) {
        if (dataSourceId == null) {
            return getDefaultAllowedTableNames();
        }
        List<TableWhitelist> list = tableWhitelistMapper.selectList(
                new LambdaQueryWrapper<TableWhitelist>()
                        .eq(TableWhitelist::getDataSourceId, dataSourceId)
                        .eq(TableWhitelist::getIsActive, 1)
        );
        if (list.isEmpty()) {
            DataSourceEntity entity = dataSourceService.requireEntity(dataSourceId);
            JdbcTemplate dsJdbc = dataSourceService.getJdbcTemplate(dataSourceId);
            return discoverTables(dsJdbc, entity.getDbType(), entity.getDatabaseName()).stream()
                    .map(t -> t.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
        }
        return list.stream()
                .map(t -> t.getTableName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public String buildSchemaDescription(Long dataSourceId) {
        Set<String> tables = getAllowedTableNames(dataSourceId);
        JdbcTemplate template = resolveJdbcTemplate(dataSourceId);
        String schemaName = resolveSchemaName(dataSourceId);
        Map<String, String> labelIndex = columnSemanticService.buildColumnLabelIndex(dataSourceId);
        Map<String, String> descIndex = columnSemanticService.buildColumnDescriptionIndex(dataSourceId);
        StringBuilder sb = new StringBuilder();
        for (String table : tables) {
            sb.append("表 ").append(table).append(":\n");
            List<Map<String, Object>> columns = queryColumns(template, schemaName, table, dataSourceId);
            for (Map<String, Object> col : columns) {
                String colName = String.valueOf(col.get("COLUMN_NAME"));
                sb.append("  - ")
                        .append(colName)
                        .append(" (")
                        .append(col.get("DATA_TYPE"))
                        .append(")");
                String label = labelIndex.get(colName.toLowerCase(Locale.ROOT));
                if (StrUtil.isNotBlank(label)) {
                    sb.append(" 业务名:").append(label);
                }
                Object comment = col.get("COLUMN_COMMENT");
                if (comment != null && StrUtil.isNotBlank(comment.toString())) {
                    sb.append(" ").append(comment);
                }
                String descKey = table.toLowerCase(Locale.ROOT) + "." + colName.toLowerCase(Locale.ROOT);
                String desc = descIndex.get(descKey);
                if (StrUtil.isNotBlank(desc)) {
                    sb.append(" 说明:").append(desc);
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        String relationships = buildRelationshipDescription(dataSourceId);
        if (StrUtil.isNotBlank(relationships)) {
            sb.append("表间关系（JOIN 参考）:\n").append(relationships).append("\n");
        }
        return sb.toString();
    }

    /**
     * 输出表间外键/推断关联，供 LLM 生成 JOIN 时使用。
     */
    public String buildRelationshipDescription(Long dataSourceId) {
        Set<String> tables = getAllowedTableNames(dataSourceId);
        if (tables.isEmpty()) {
            return "";
        }
        JdbcTemplate template = resolveJdbcTemplate(dataSourceId);
        String schemaName = resolveSchemaName(dataSourceId);
        LinkedHashSet<String> relations = new LinkedHashSet<>(queryForeignKeys(template, schemaName, tables, dataSourceId));

        Map<String, List<String>> tableColumns = new HashMap<>();
        for (String table : tables) {
            tableColumns.put(table, queryColumns(template, schemaName, table, dataSourceId).stream()
                    .map(c -> String.valueOf(c.get("COLUMN_NAME")).toLowerCase(Locale.ROOT))
                    .toList());
        }
        inferRelationships(tables, tableColumns).forEach(relations::add);

        if (relations.isEmpty()) {
            return "";
        }
        return relations.stream().map(r -> "- " + r).collect(Collectors.joining("\n"));
    }

    public Map<String, Object> getSchemaSummary(Long dataSourceId) {
        Set<String> tables = getAllowedTableNames(dataSourceId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("tables", tables);
        summary.put("description", buildSchemaDescription(dataSourceId));
        summary.put("tableSummary", buildTableSummary(dataSourceId));
        summary.put("dataSourceId", dataSourceId);
        return summary;
    }

    public String buildTableSummary(Long dataSourceId) {
        Set<String> tables = getAllowedTableNames(dataSourceId);
        JdbcTemplate template = resolveJdbcTemplate(dataSourceId);
        String schemaName = resolveSchemaName(dataSourceId);
        StringBuilder sb = new StringBuilder();
        for (String table : tables) {
            sb.append("- ").append(table);
            List<Map<String, Object>> columns = queryColumns(template, schemaName, table, dataSourceId);
            if (!columns.isEmpty()) {
                sb.append("（");
                sb.append(columns.stream()
                        .limit(5)
                        .map(c -> String.valueOf(c.get("COLUMN_NAME")))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));
                if (columns.size() > 5) {
                    sb.append(" 等");
                }
                sb.append("）");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 供描述性问答使用：仅输出业务语义，不包含物理表名/字段名。
     */
    public String buildBusinessSchemaDescription(Long dataSourceId) {
        Set<String> tables = getAllowedTableNames(dataSourceId);
        JdbcTemplate template = resolveJdbcTemplate(dataSourceId);
        String schemaName = resolveSchemaName(dataSourceId);
        Map<String, String> whitelistComments = loadWhitelistComments(dataSourceId);
        Map<String, String> labelIndex = columnSemanticService.buildColumnLabelIndex(dataSourceId);
        Map<String, String> descIndex = columnSemanticService.buildColumnDescriptionIndex(dataSourceId);

        StringBuilder sb = new StringBuilder();
        for (String table : tables) {
            String tableComment = whitelistComments.getOrDefault(table.toLowerCase(Locale.ROOT),
                    queryTableComment(template, schemaName, table, dataSourceId));
            String businessName = toBusinessName(table, tableComment);

            sb.append("- ").append(businessName);
            if (StrUtil.isNotBlank(tableComment) && !tableComment.equals(businessName)) {
                sb.append("：").append(tableComment);
            }

            List<Map<String, Object>> columns = queryColumns(template, schemaName, table, dataSourceId);
            List<String> fieldDesc = columns.stream()
                    .map(col -> toFieldDescription(col, table, labelIndex, descIndex))
                    .filter(StrUtil::isNotBlank)
                    .limit(8)
                    .toList();
            if (!fieldDesc.isEmpty()) {
                sb.append("，包含：").append(String.join("、", fieldDesc));
                if (columns.size() > 8) {
                    sb.append(" 等");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public Map<String, String> resolveColumnLabels(Long dataSourceId, List<String> columnKeys) {
        return ColumnLabelResolver.resolve(columnKeys, buildColumnCommentIndex(dataSourceId));
    }

    private Map<String, String> buildColumnCommentIndex(Long dataSourceId) {
        Map<String, String> index = new HashMap<>(columnSemanticService.buildColumnLabelIndex(dataSourceId));
        Set<String> tables = getAllowedTableNames(dataSourceId);
        JdbcTemplate template = resolveJdbcTemplate(dataSourceId);
        String schemaName = resolveSchemaName(dataSourceId);
        for (String table : tables) {
            for (Map<String, Object> col : queryColumns(template, schemaName, table, dataSourceId)) {
                Object name = col.get("COLUMN_NAME");
                Object comment = col.get("COLUMN_COMMENT");
                if (name == null) {
                    continue;
                }
                String key = name.toString().toLowerCase(Locale.ROOT);
                if (index.containsKey(key)) {
                    continue;
                }
                if (comment != null && StrUtil.isNotBlank(comment.toString())) {
                    index.put(key, comment.toString().trim());
                }
            }
        }
        return index;
    }

    private Map<String, String> loadWhitelistComments(Long dataSourceId) {
        LambdaQueryWrapper<TableWhitelist> wrapper = new LambdaQueryWrapper<TableWhitelist>()
                .eq(TableWhitelist::getIsActive, 1);
        if (dataSourceId != null) {
            wrapper.eq(TableWhitelist::getDataSourceId, dataSourceId);
        }
        return tableWhitelistMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(
                        t -> t.getTableName().toLowerCase(Locale.ROOT),
                        t -> StrUtil.blankToDefault(t.getTableComment(), ""),
                        (a, b) -> a
                ));
    }

    private String queryTableComment(JdbcTemplate template, String schemaName, String table, Long dataSourceId) {
        DataSourceEntity entity = dataSourceId == null ? null : dataSourceService.requireEntity(dataSourceId);
        String dbType = entity == null ? "mysql" : entity.getDbType();
        if (dbType != null && dbType.toLowerCase(Locale.ROOT).startsWith("pg")) {
            List<Map<String, Object>> rows = template.queryForList(
                    """
                    SELECT obj_description(pgc.oid) AS TABLE_COMMENT
                    FROM pg_class pgc
                    JOIN pg_namespace pgn ON pgn.oid = pgc.relnamespace
                    WHERE pgn.nspname = 'public' AND pgc.relname = ?
                    """,
                    table
            );
            if (!rows.isEmpty() && rows.get(0).get("TABLE_COMMENT") != null) {
                return rows.get(0).get("TABLE_COMMENT").toString();
            }
            return "";
        }
        List<Map<String, Object>> rows = template.queryForList(
                """
                SELECT TABLE_COMMENT FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                """,
                schemaName, table
        );
        if (!rows.isEmpty() && rows.get(0).get("TABLE_COMMENT") != null) {
            return rows.get(0).get("TABLE_COMMENT").toString();
        }
        return "";
    }

    private String toBusinessName(String tableName, String tableComment) {
        if (StrUtil.isNotBlank(tableComment)) {
            return tableComment.split("[，,；;（(]")[0].trim();
        }
        String name = tableName.toLowerCase(Locale.ROOT);
        if (name.startsWith("tb_")) {
            name = name.substring(3);
        }
        return name.replace("_", " ");
    }

    private String toFieldDescription(Map<String, Object> column, String table,
                                      Map<String, String> labelIndex, Map<String, String> descIndex) {
        Object colNameObj = column.get("COLUMN_NAME");
        if (colNameObj == null) {
            return "";
        }
        String colName = colNameObj.toString();
        String label = labelIndex.get(colName.toLowerCase(Locale.ROOT));
        if (StrUtil.isNotBlank(label)) {
            String descKey = table.toLowerCase(Locale.ROOT) + "." + colName.toLowerCase(Locale.ROOT);
            String desc = descIndex.get(descKey);
            if (StrUtil.isNotBlank(desc)) {
                return label + "（" + desc + "）";
            }
            return label;
        }
        return toFieldDescription(column);
    }

    private String toFieldDescription(Map<String, Object> column) {
        Object comment = column.get("COLUMN_COMMENT");
        if (comment != null && StrUtil.isNotBlank(comment.toString())) {
            return comment.toString().trim();
        }
        Object colName = column.get("COLUMN_NAME");
        if (colName == null) {
            return "";
        }
        String name = colName.toString().toLowerCase(Locale.ROOT);
        if (name.endsWith("_id")) {
            return name.replace("_id", "") + "编号";
        }
        return name.replace("_", " ");
    }

    private Set<String> getDefaultAllowedTableNames() {
        List<TableWhitelist> list = tableWhitelistMapper.selectList(
                new LambdaQueryWrapper<TableWhitelist>()
                        .eq(TableWhitelist::getDataSourceId, 1)
                        .eq(TableWhitelist::getIsActive, 1)
        );
        if (list.isEmpty()) {
            list = tableWhitelistMapper.selectList(
                    new LambdaQueryWrapper<TableWhitelist>().eq(TableWhitelist::getIsActive, 1)
            );
        }
        if (list.isEmpty()) {
            return Set.of("products", "orders", "users");
        }
        return list.stream()
                .map(t -> t.getTableName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private JdbcTemplate resolveJdbcTemplate(Long dataSourceId) {
        if (dataSourceId == null) {
            return jdbcTemplate;
        }
        return dataSourceService.getJdbcTemplate(dataSourceId);
    }

    private String resolveSchemaName(Long dataSourceId) {
        if (dataSourceId == null) {
            return jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        }
        return dataSourceService.requireEntity(dataSourceId).getDatabaseName();
    }

    private List<Map<String, Object>> queryColumns(JdbcTemplate template, String schemaName,
                                                    String table, Long dataSourceId) {
        DataSourceEntity entity = dataSourceId == null ? null : dataSourceService.requireEntity(dataSourceId);
        String dbType = entity == null ? "mysql" : entity.getDbType();
        if (dbType != null && dbType.toLowerCase(Locale.ROOT).startsWith("pg")) {
            return template.queryForList(
                    """
                    SELECT column_name AS COLUMN_NAME, data_type AS DATA_TYPE, '' AS COLUMN_COMMENT
                    FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ?
                    ORDER BY ordinal_position
                    """,
                    table
            );
        }
        return template.queryForList(
                """
                SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """,
                schemaName, table
        );
    }

    private List<String> discoverTables(JdbcTemplate jdbcTemplate, String dbType, String databaseName) {
        String type = dbType == null ? "mysql" : dbType.toLowerCase(Locale.ROOT);
        if (type.startsWith("pg")) {
            return jdbcTemplate.queryForList(
                    """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                    ORDER BY table_name
                    """,
                    String.class
            );
        }
        return jdbcTemplate.queryForList(
                """
                SELECT TABLE_NAME
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """,
                String.class,
                databaseName
        );
    }

    private List<String> queryForeignKeys(JdbcTemplate template, String schemaName,
                                          Set<String> tables, Long dataSourceId) {
        DataSourceEntity entity = dataSourceId == null ? null : dataSourceService.requireEntity(dataSourceId);
        String dbType = entity == null ? "mysql" : entity.getDbType();
        List<String> tableList = new ArrayList<>(tables);
        if (tableList.isEmpty()) {
            return List.of();
        }
        String inClause = tableList.stream().map(t -> "?").collect(Collectors.joining(", "));
        if (dbType != null && dbType.toLowerCase(Locale.ROOT).startsWith("pg")) {
            String sql = """
                    SELECT kcu.table_name AS TABLE_NAME,
                           kcu.column_name AS COLUMN_NAME,
                           ccu.table_name AS REFERENCED_TABLE_NAME,
                           ccu.column_name AS REFERENCED_COLUMN_NAME
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                     AND tc.table_schema = kcu.table_schema
                    JOIN information_schema.constraint_column_usage ccu
                      ON ccu.constraint_name = tc.constraint_name
                     AND ccu.table_schema = tc.table_schema
                    WHERE tc.constraint_type = 'FOREIGN KEY'
                      AND tc.table_schema = 'public'
                      AND kcu.table_name IN (%s)
                    """.formatted(inClause);
            Object[] params = tableList.toArray();
            return template.queryForList(sql, params).stream()
                    .map(this::formatForeignKeyRow)
                    .filter(StrUtil::isNotBlank)
                    .toList();
        }
        String sql = """
                SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                  AND TABLE_NAME IN (%s)
                """.formatted(inClause);
        List<Object> params = new ArrayList<>();
        params.add(schemaName);
        params.addAll(tableList);
        return template.queryForList(sql, params.toArray()).stream()
                .map(this::formatForeignKeyRow)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private String formatForeignKeyRow(Map<String, Object> row) {
        String table = String.valueOf(row.get("TABLE_NAME"));
        String column = String.valueOf(row.get("COLUMN_NAME"));
        String refTable = String.valueOf(row.get("REFERENCED_TABLE_NAME"));
        String refColumn = String.valueOf(row.get("REFERENCED_COLUMN_NAME"));
        if (StrUtil.hasBlank(table, column, refTable, refColumn)) {
            return "";
        }
        return table + "." + column + " -> " + refTable + "." + refColumn;
    }

    private List<String> inferRelationships(Set<String> tables, Map<String, List<String>> tableColumns) {
        List<String> inferred = new ArrayList<>();
        Set<String> lowerTables = tables.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        for (String table : tables) {
            List<String> columns = tableColumns.getOrDefault(table, List.of());
            for (String column : columns) {
                if (!column.endsWith("_id") || "id".equals(column)) {
                    continue;
                }
                String base = column.substring(0, column.length() - 3);
                String refTable = resolveReferencedTable(base, lowerTables);
                if (refTable != null) {
                    inferred.add(table + "." + column + " -> " + refTable + ".id");
                }
            }
        }
        return inferred;
    }

    private String resolveReferencedTable(String base, Set<String> tables) {
        List<String> candidates = List.of(
                base,
                base + "s",
                base + "es",
                base.endsWith("y") ? base.substring(0, base.length() - 1) + "ies" : base
        );
        for (String candidate : candidates) {
            if (tables.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
