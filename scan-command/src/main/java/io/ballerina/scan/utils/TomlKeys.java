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
import io.ballerina.toml.semantic.ast.TomlKeyValueNode;
import io.ballerina.toml.semantic.ast.TomlTableArrayNode;
import io.ballerina.toml.semantic.ast.TomlTableNode;
import io.ballerina.toml.semantic.ast.TopLevelNode;
import io.ballerina.toml.syntax.tree.KeyValueNode;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Removes the quotes the TOML parser keeps on quoted keys and table names, such as {@code "sonar.host.url"} or
 * {@code ['rule']}. TOML treats a quoted key and the same bare key as one key, so without this a quoted key would not
 * match the name it is looked up by.
 *
 * @since 0.14.1
 */
final class TomlKeys {

    private static final Comparator<TopLevelNode> BY_POSITION = Comparator
            .comparingInt((TopLevelNode node) -> node.location().lineRange().startLine().line())
            .thenComparingInt(node -> node.location().lineRange().startLine().offset());

    private TomlKeys() {
    }

    /**
     * A key or table written both quoted and bare, which TOML does not allow.
     *
     * @param key       the key without quotes
     * @param table     whether the second definition is a table rather than a key
     * @param lineRange the position of the second definition
     */
    record Duplicate(String key, boolean table, LineRange lineRange) {
    }

    /**
     * Removes the quotes from every key of a document, in place, so that lookups and {@link Toml#toMap()} use the
     * bare names. Table arrays written both quoted and bare, such as {@code [["platform"]]} and {@code [[platform]]},
     * are merged in the order of the file.
     *
     * @param toml the parsed document
     * @return the keys and tables defined both quoted and bare
     */
    static List<Duplicate> normalize(Toml toml) {
        List<Duplicate> duplicates = new ArrayList<>();
        normalize(toml.rootNode(), duplicates);
        return duplicates;
    }

    private static void normalize(TomlTableNode table, List<Duplicate> duplicates) {
        Map<String, TopLevelNode> normalized = new LinkedHashMap<>();
        table.entries().forEach((key, node) -> {
            normalizeChildren(node, duplicates);
            put(normalized, unquote(key), node, duplicates);
        });
        table.entries().clear();
        table.entries().putAll(normalized);
    }

    private static void normalizeChildren(TopLevelNode node, List<Duplicate> duplicates) {
        if (node instanceof TomlTableNode table) {
            normalize(table, duplicates);
        } else if (node instanceof TomlTableArrayNode tableArray) {
            tableArray.children().forEach(child -> normalize(child, duplicates));
        }
    }

    private static void put(Map<String, TopLevelNode> entries, String key, TopLevelNode node,
                            List<Duplicate> duplicates) {
        TopLevelNode existing = entries.get(key);
        if (existing == null) {
            entries.put(key, node);
        } else if (existing instanceof TomlTableArrayNode first && node instanceof TomlTableArrayNode second) {
            first.children().addAll(second.children());
            first.children().sort(BY_POSITION);
            reassignSubTables(first.children(), duplicates);
        } else if (existing instanceof TomlTableNode first && node instanceof TomlTableNode second
                && (first.generated() || second.generated())) {
            // A table created implicitly by a dotted name, such as [scan.x], may be defined again explicitly
            second.entries().forEach((childKey, child) -> put(first.entries(), childKey, child, duplicates));
        } else {
            TopLevelNode later = BY_POSITION.compare(existing, node) <= 0 ? node : existing;
            entries.put(key, later == node ? existing : node);
            boolean isKey = later instanceof TomlKeyValueNode || later.externalTreeNode() instanceof KeyValueNode;
            duplicates.add(new Duplicate(key, !isKey, later.location().lineRange()));
        }
    }

    /**
     * Moves each sub-table, such as {@code [platform.settings]}, to the element written last before it. The parser
     * attaches it to the last element of the array with the same spelling, which is wrong once quoted and bare
     * arrays are merged.
     */
    private static void reassignSubTables(List<TomlTableNode> elements, List<Duplicate> duplicates) {
        for (TomlTableNode element : List.copyOf(elements)) {
            for (Map.Entry<String, TopLevelNode> entry : List.copyOf(element.entries().entrySet())) {
                TopLevelNode node = entry.getValue();
                TomlTableNode owner = element;
                for (TomlTableNode candidate : elements) {
                    if (BY_POSITION.compare(candidate, node) < 0) {
                        owner = candidate;
                    }
                }
                if (owner != element) {
                    element.entries().remove(entry.getKey());
                    put(owner.entries(), entry.getKey(), node, duplicates);
                }
            }
        }
    }

    // A bare key cannot contain a quote, so matching quotes at both ends always come from a quoted key
    private static String unquote(String key) {
        if (key.length() >= 2) {
            char first = key.charAt(0);
            if ((first == '"' || first == '\'') && key.charAt(key.length() - 1) == first) {
                return key.substring(1, key.length() - 1);
            }
        }
        return key;
    }
}
