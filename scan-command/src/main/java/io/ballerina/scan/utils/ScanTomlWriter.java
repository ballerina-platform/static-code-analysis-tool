/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.utils;

import io.ballerina.toml.api.Toml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * {@code ScanTomlWriter} provides utilities for creating and modifying Scan.toml files,
 * specifically for adding symbol-based and global exclusion entries.
 *
 * @since 0.11.1
 */
public final class ScanTomlWriter {

    private ScanTomlWriter() {
    }

    /**
     * Adds a symbol-based exclusion entry to the Scan.toml file at the given path.
     * If the file does not exist, it will be created. Duplicate entries are not added.
     *
     * @param scanTomlPath the path to the Scan.toml file
     * @param filePath     the relative file path for the exclusion
     * @param ruleId       the fully qualified rule identifier
     * @param symbol       the enclosing symbol name
     * @param lineHash     the hashed line content
     * @throws IOException if an I/O error occurs while reading or writing the file
     */
    public static void addExclusion(Path scanTomlPath, String filePath, String ruleId, String symbol, String lineHash)
            throws IOException {
        modifyAndWrite(scanTomlPath, tomlMap -> {
            List<Map<String, Object>> exclusions = getOrCreateTableArray(tomlMap, Constants.EXCLUSION_TABLE);

            // Check for duplicate entry
            for (Map<String, Object> ex : exclusions) {
                if (Objects.equals(ex.get(Constants.EXCLUSION_FILE_PATH), filePath) &&
                        Objects.equals(ex.get(Constants.EXCLUSION_RULE_ID), ruleId) &&
                        Objects.equals(ex.get(Constants.EXCLUSION_SYMBOL), symbol) &&
                        Objects.equals(ex.get(Constants.EXCLUSION_LINE_HASH), lineHash)) {
                    return;
                }
            }

            Map<String, Object> newExclusion = new LinkedHashMap<>();
            newExclusion.put(Constants.EXCLUSION_FILE_PATH, filePath);
            newExclusion.put(Constants.EXCLUSION_RULE_ID, ruleId);
            newExclusion.put(Constants.EXCLUSION_SYMBOL, symbol);
            newExclusion.put(Constants.EXCLUSION_LINE_HASH, lineHash);
            exclusions.add(newExclusion);
        });
    }

    /**
     * Adds multiple symbol-based exclusion entries to the Scan.toml file.
     *
     * @param scanTomlPath the path to the Scan.toml file
     * @param exclusions   the set of exclusions to add
     * @throws IOException if an I/O error occurs
     */
    public static void addExclusions(Path scanTomlPath, Set<ScanTomlFile.Exclusion> exclusions) throws IOException {
        modifyAndWrite(scanTomlPath, tomlMap -> {
            List<Map<String, Object>> exclusionList = getOrCreateTableArray(tomlMap, Constants.EXCLUSION_TABLE);

            for (ScanTomlFile.Exclusion exclusion : exclusions) {
                boolean duplicate = false;
                for (Map<String, Object> ex : exclusionList) {
                    if (Objects.equals(ex.get(Constants.EXCLUSION_FILE_PATH), exclusion.filePath()) &&
                            Objects.equals(ex.get(Constants.EXCLUSION_RULE_ID), exclusion.ruleId()) &&
                            Objects.equals(ex.get(Constants.EXCLUSION_SYMBOL), exclusion.symbol()) &&
                            Objects.equals(ex.get(Constants.EXCLUSION_LINE_HASH), exclusion.lineHash())) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    Map<String, Object> newExclusion = new LinkedHashMap<>();
                    newExclusion.put(Constants.EXCLUSION_FILE_PATH, exclusion.filePath());
                    newExclusion.put(Constants.EXCLUSION_RULE_ID, exclusion.ruleId());
                    newExclusion.put(Constants.EXCLUSION_SYMBOL, exclusion.symbol());
                    newExclusion.put(Constants.EXCLUSION_LINE_HASH, exclusion.lineHash());
                    exclusionList.add(newExclusion);
                }
            }
        });
    }

    /**
     * Adds a rule ID to the global [rule] exclude array in Scan.toml.
     *
     * @param scanTomlPath the path to the Scan.toml file
     * @param ruleId       the rule identifier to globally exclude
     * @throws IOException if an I/O error occurs
     */
    public static void addGlobalExclusion(Path scanTomlPath, String ruleId) throws IOException {
        modifyAndWrite(scanTomlPath, tomlMap -> {
            Map<String, Object> ruleMap = getOrCreateTable(tomlMap, Constants.RULES_TABLE);
            List<Object> excludeList = getOrCreateArray(ruleMap, Constants.EXCLUDE_KEY);

            if (!excludeList.contains(ruleId)) {
                excludeList.add(ruleId);
            }
        });
    }

    /**
     * Removes a symbol-based exclusion entry from the Scan.toml file.
     *
     * @param scanTomlPath the path to the Scan.toml file
     * @param filePath     the relative file path for the exclusion
     * @param ruleId       the fully qualified rule identifier
     * @param symbol       the enclosing symbol name
     * @param lineHash     the hashed line content
     * @throws IOException if an I/O error occurs
     */
    public static void removeExclusion(Path scanTomlPath, String filePath, String ruleId, String symbol, 
            String lineHash)
            throws IOException {
        modifyAndWrite(scanTomlPath, tomlMap -> {
            if (tomlMap.containsKey(Constants.EXCLUSION_TABLE) && 
                    tomlMap.get(Constants.EXCLUSION_TABLE) instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exclusions = 
                        (List<Map<String, Object>>) tomlMap.get(Constants.EXCLUSION_TABLE);
                exclusions.removeIf(ex ->
                        Objects.equals(ex.get(Constants.EXCLUSION_FILE_PATH), filePath) &&
                        Objects.equals(ex.get(Constants.EXCLUSION_RULE_ID), ruleId) &&
                        Objects.equals(ex.get(Constants.EXCLUSION_SYMBOL), symbol) &&
                        Objects.equals(ex.get(Constants.EXCLUSION_LINE_HASH), lineHash)
                );
                
                if (exclusions.isEmpty()) {
                    tomlMap.remove(Constants.EXCLUSION_TABLE);
                }
            }
        });
    }

    /**
     * Removes a rule ID from the global [rule] exclude array in Scan.toml.
     *
     * @param scanTomlPath the path to the Scan.toml file
     * @param ruleId       the rule identifier to globally remove from exclusions
     * @throws IOException if an I/O error occurs
     */
    public static void removeGlobalExclusion(Path scanTomlPath, String ruleId) throws IOException {
        modifyAndWrite(scanTomlPath, tomlMap -> {
            if (tomlMap.containsKey(Constants.RULES_TABLE) && tomlMap.get(Constants.RULES_TABLE) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleMap = (Map<String, Object>) tomlMap.get(Constants.RULES_TABLE);
                if (ruleMap.containsKey(Constants.EXCLUDE_KEY) && ruleMap.get(Constants.EXCLUDE_KEY) instanceof List) {
                    List<?> excludeList = (List<?>) ruleMap.get(Constants.EXCLUDE_KEY);
                    excludeList.remove(ruleId);

                    if (excludeList.isEmpty()) {
                        ruleMap.remove(Constants.EXCLUDE_KEY);
                        if (ruleMap.isEmpty()) {
                            tomlMap.remove(Constants.RULES_TABLE);
                        }
                    }
                }
            }
        });
    }

    // --- Core Modification Logic ---

    private interface Modifier {
        void modify(Map<String, Object> tomlMap);
    }

    private static void modifyAndWrite(Path scanTomlPath, Modifier modifier) throws IOException {
        Map<String, Object> tomlMap = new HashMap<>();
        if (Files.exists(scanTomlPath)) {
            String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8).trim();
            if (!content.isEmpty()) {
                Toml toml = Toml.read(scanTomlPath);
                tomlMap = deepMutableCopy(toml.toMap());
            }
        }

        modifier.modify(tomlMap);

        Path parentDir = scanTomlPath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        String newTomlContent = generateTomlString(tomlMap);
        Files.writeString(scanTomlPath, newTomlContent, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepMutableCopy(Map<String, Object> map) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            copy.put(entry.getKey(), makeMutable(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object makeMutable(Object value) {
        if (value instanceof Map) {
            return deepMutableCopy((Map<String, Object>) value);
        } else if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                copy.add(makeMutable(item));
            }
            return copy;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getOrCreateTableArray(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) instanceof List) {
            return (List<Map<String, Object>>) map.get(key);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        map.put(key, list);
        return list;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getOrCreateTable(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) instanceof Map) {
            return (Map<String, Object>) map.get(key);
        }
        Map<String, Object> table = new LinkedHashMap<>();
        map.put(key, table);
        return table;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getOrCreateArray(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) instanceof List) {
            return (List<Object>) map.get(key);
        }
        List<Object> list = new ArrayList<>();
        map.put(key, list);
        return list;
    }

    // --- Generation Logic ---

    private static String generateTomlString(Map<String, Object> tomlMap) {
        StringBuilder sb = new StringBuilder();

        // Prioritize writing in standard order: platform, analyzer, rule, exclusion
        writeTableArray(sb, tomlMap, Constants.PLATFORM_TABLE);
        writeTableArray(sb, tomlMap, Constants.ANALYZER_TABLE);

        if (tomlMap.containsKey(Constants.RULES_TABLE)) {
            writeTable(sb, Constants.RULES_TABLE, tomlMap.get(Constants.RULES_TABLE));
        }

        // Write any other keys that might exist (to prevent data loss)
        for (Map.Entry<String, Object> entry : tomlMap.entrySet()) {
            String k = entry.getKey();
            if (k.equals(Constants.PLATFORM_TABLE) || k.equals(Constants.ANALYZER_TABLE) 
                    || k.equals(Constants.RULES_TABLE)
                    || k.equals(Constants.EXCLUSION_TABLE)) {
                continue;
            }

            Object v = entry.getValue();
            if (v instanceof List && !((List<?>) v).isEmpty() && ((List<?>) v).get(0) instanceof Map) {
                writeTableArray(sb, tomlMap, k);
            } else if (v instanceof Map) {
                writeTable(sb, k, v);
            } else {
                sb.append(k).append(" = ");
                writeValue(sb, v);
                sb.append("\n\n");
            }
        }

        writeTableArray(sb, tomlMap, Constants.EXCLUSION_TABLE);

        String result = sb.toString().trim();
        return result.isEmpty() ? "" : result + "\n";
    }

    @SuppressWarnings("unchecked")
    private static void writeTable(StringBuilder sb, String key, Object tableObj) {
        if (tableObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) tableObj;
            if (!map.isEmpty()) {
                sb.append("[").append(key).append("]\n");
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    sb.append(entry.getKey()).append(" = ");
                    writeValue(sb, entry.getValue());
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeTableArray(StringBuilder sb, Map<String, Object> tomlMap, String key) {
        Object arrayObj = tomlMap.get(key);
        if (arrayObj instanceof List) {
            List<?> list = (List<?>) arrayObj;
            for (Object item : list) {
                if (item instanceof Map) {
                    sb.append("[[").append(key).append("]]\n");
                    Map<String, Object> map = (Map<String, Object>) item;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        sb.append(entry.getKey()).append(" = ");
                        writeValue(sb, entry.getValue());
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
            }
        }
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value instanceof String) {
            sb.append("\"").append(escapeTomlString((String) value)).append("\"");
        } else if (value instanceof List) {
            sb.append("[");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                writeValue(sb, list.get(i));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        } else if (value instanceof Map) {
            writeInlineTable(sb, (Map<?, ?>) value);
        } else {
            sb.append(value);
        }
    }

    private static void writeInlineTable(StringBuilder sb, Map<?, ?> map) {
        sb.append("{ ");
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(String.valueOf(entry.getKey())).append(" = ");
            writeValue(sb, entry.getValue());
            if (index < map.size() - 1) {
                sb.append(", ");
            }
            index++;
        }
        sb.append(" }");
    }

    private static String escapeTomlString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}
