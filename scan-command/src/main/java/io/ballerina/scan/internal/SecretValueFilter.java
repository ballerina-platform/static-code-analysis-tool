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

package io.ballerina.scan.internal;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code SecretValueFilter} recognizes literal values that cannot be real secrets, such as empty strings,
 * templates and well-known placeholders, so that they are not reported as hard-coded secrets.
 *
 * @since 0.13.0
 */
final class SecretValueFilter {
    // Environment variable references are matched only in upper case, since "$ecureP4ss" is a plausible password.
    private static final Pattern TEMPLATE = Pattern.compile(
            "\\$\\{.*}|\\{\\{.*}}|<[^<>]+>|\\$[A-Z_][A-Z0-9_]*|%[A-Z_][A-Z0-9_]*%");
    // A template reference inside a larger value, such as "Bearer {{token}}", also makes it a placeholder.
    private static final Pattern EMBEDDED_TEMPLATE = Pattern.compile("\\$\\{[^}]*}|\\{\\{[^}]*}}");
    private static final Pattern REPEATED_CHARACTER = Pattern.compile("(.)\\1*");
    private static final Pattern YOUR_PREFIX = Pattern.compile(
            "(?i)your[-_ a-z]*(key|password|passwd|pwd|passphrase|secret|token|credentials?)s?([-_ ]?here)?");
    private static final Set<String> DUMMY_VALUES = Set.of("changeme", "change_me", "change-me", "changeit",
            "dummy", "placeholder", "example", "sample", "redacted", "todo", "tbd");

    private static final Pattern ESCAPE = Pattern.compile("\\\\(?:u\\{([0-9A-Fa-f]+)}|(.))");

    private SecretValueFilter() {
    }

    // The value of a string literal's source text, without its quotes and with its escapes decoded.
    static String literalValue(String literalText) {
        String content = literalText.length() > 1 && literalText.startsWith("\"") && literalText.endsWith("\"")
                ? literalText.substring(1, literalText.length() - 1)
                : literalText;
        return ESCAPE.matcher(content).replaceAll(escape -> {
            if (escape.group(1) != null) {
                return Matcher.quoteReplacement(codePoint(escape.group(1)).orElse(escape.group()));
            }
            return Matcher.quoteReplacement(switch (escape.group(2)) {
                case "t" -> "\t";
                case "n" -> "\n";
                case "r" -> "\r";
                default -> escape.group(2);
            });
        });
    }

    private static Optional<String> codePoint(String hexDigits) {
        try {
            return Optional.of(Character.toString(Integer.parseInt(hexDigits, 16)));
        } catch (IllegalArgumentException e) {
            // Not a valid code point, which only a source with compile errors contains.
            return Optional.empty();
        }
    }

    static boolean isPlaceholder(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty()
                || TEMPLATE.matcher(trimmedValue).matches()
                || EMBEDDED_TEMPLATE.matcher(trimmedValue).find()
                || REPEATED_CHARACTER.matcher(trimmedValue).matches()
                || YOUR_PREFIX.matcher(trimmedValue).matches()
                || DUMMY_VALUES.contains(trimmedValue.toLowerCase(Locale.ENGLISH));
    }
}
