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

import io.ballerina.tools.text.TextRange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * {@code SnippetExtractor} extracts the exact source text an issue's location covers, for use as a
 * {@code snippet.text} value in both the SARIF and Ballerina JSON output.
 *
 * @since 0.1.0
 */
final class SnippetExtractor {

    /**
     * Extracts the source text covered by {@code textRange} from the given file.
     *
     * @param filePath         absolute path of the file the issue was found in
     * @param textRange        the character range of the issue's location
     * @param fileContentCache cache of file path to its full content, shared across a single report
     * @return the source text covered by the range, or {@code null} when unavailable
     */
    static String extract(String filePath, TextRange textRange, Map<String, String> fileContentCache) {
        String content = fileContentCache.computeIfAbsent(filePath, path -> {
            try {
                return Files.readString(Path.of(path), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                return null;
            }
        });
        if (content == null) {
            return null;
        }
        int start = textRange.startOffset();
        int end = start + textRange.length();
        if (start < 0 || end > content.length() || start > end) {
            return null;
        }
        return content.substring(start, end);
    }

    private SnippetExtractor() {
    }
}
