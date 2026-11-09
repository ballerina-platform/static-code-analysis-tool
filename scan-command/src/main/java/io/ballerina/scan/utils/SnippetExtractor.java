/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
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

import io.ballerina.scan.internal.IssueImpl;
import io.ballerina.tools.text.TextRange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * {@code SnippetExtractor} extracts the exact source text an issue's location covers, for use as a
 * {@code snippet.text} value in both the SARIF and Ballerina JSON output.
 *
 * @since 0.11.1
 */
final class SnippetExtractor {

    /**
     * Extracts the source text covered by the issue's location. The text is taken from the source the compiler
     * analyzed, which the issue's offsets refer to, and from the file only when the issue carries no source.
     *
     * @param issue            the issue
     * @param fileContentCache cache of file path to its full content, or empty when it cannot be read
     * @return the source text covered by the range, or {@code null} when unavailable
     */
    static String extract(IssueImpl issue, Map<String, Optional<String>> fileContentCache) {
        Optional<String> content = fileContentCache.computeIfAbsent(issue.filePath(), path -> issue.textDocument()
                .map(textDocument -> new String(textDocument.toCharArray()))
                .or(() -> readFile(path)));
        if (content.isEmpty()) {
            return null;
        }
        TextRange textRange = issue.location().textRange();
        int start = textRange.startOffset();
        int end = start + textRange.length();
        if (start < 0 || end > content.get().length() || start > end) {
            return null;
        }
        return content.get().substring(start, end);
    }

    private static Optional<String> readFile(String path) {
        try {
            return Optional.of(Files.readString(Path.of(path), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private SnippetExtractor() {
    }
}
