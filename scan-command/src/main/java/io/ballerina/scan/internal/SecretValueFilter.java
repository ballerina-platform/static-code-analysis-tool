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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final Pattern ANGLE_PLACEHOLDER = Pattern.compile("<[^<>]+>");
    // A word in one case is no longer than this; a longer run, as in a base32 key, is checked for randomness.
    private static final int MAX_SINGLE_CASE_WORD_LENGTH = 15;
    private static final Pattern YOUR_PREFIX = Pattern.compile("(?i)your[-_ a-z]*(key|"
            + String.join("|", Stream.concat(SecretNameMatcher.EMBEDDED_KEYWORDS.stream(),
            SecretNameMatcher.WORD_KEYWORDS.stream()).toList()) + ")s?([-_ ]?here)?");
    private static final Set<String> DUMMY_VALUES = Set.of("changeme", "change_me", "change-me", "changeit",
            "dummy", "placeholder", "example", "sample", "redacted", "todo", "tbd");
    // A generated secret, such as a token or a key, is at least this long and this random, in bits per character.
    // Six distinct characters in eight reach it, as do random hexadecimal digits.
    private static final int MIN_GENERATED_LENGTH = 8;
    private static final double MIN_GENERATED_ENTROPY = 2.5;
    // Words are separated as in text, a header or a path, as in "Authorization: Bearer" or "auth/token", but a
    // colon alone joins a user and a password, as in "admin:admin"
    private static final Pattern LETTER_SEGMENTS = Pattern.compile("[A-Za-z]+(?:(?:: |[ _./,-])+[A-Za-z]+)*");
    private static final Pattern SEGMENT_SEPARATOR = Pattern.compile("(?:: |[ _./,-])+");
    private static final Pattern EDGE_SEPARATORS = Pattern.compile("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$");
    private static final Pattern SINGLE_CASE_WORD = Pattern.compile("[a-z]+|[A-Z]+|[A-Z][a-z]+");
    // The words of a camel case segment, such as "XML", "Http" and "Request" in "XMLHttpRequest".
    private static final Pattern CAMEL_CASE_WORD = Pattern.compile("[A-Z]+(?![a-z])|[A-Z]?[a-z]+");
    private static final double MIN_AVERAGE_WORD_LENGTH = 3;

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

    // The value without its placeholders such as "<token>"; a "${TOKEN}" is kept, as it makes the whole a placeholder.
    static String withoutAnglePlaceholders(String value) {
        return ANGLE_PLACEHOLDER.matcher(value).replaceAll("");
    }

    // Whether a value looks generated, as a token or a key does; a password can be any value.
    static boolean looksGenerated(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.length() >= MIN_GENERATED_LENGTH
                && !isWords(trimmedValue)
                && !isConsecutive(trimmedValue)
                && entropy(trimmedValue) >= MIN_GENERATED_ENTROPY;
    }

    // Random letters, as in "xKqLzPmRtYvWbNcD", change case every letter or two, unlike "XMLHttpRequest".
    private static boolean isWords(String value) {
        // A prefix or a header name, such as "sk_live_" or "Authorization: ", ends with a separator
        String letters = EDGE_SEPARATORS.matcher(value).replaceAll("");
        if (!LETTER_SEGMENTS.matcher(letters).matches()) {
            return false;
        }
        for (String segment : SEGMENT_SEPARATOR.split(letters)) {
            if (SINGLE_CASE_WORD.matcher(segment).matches()) {
                if (segment.length() > MAX_SINGLE_CASE_WORD_LENGTH) {
                    return false;
                }
                continue;
            }
            long words = CAMEL_CASE_WORD.matcher(segment).results().count();
            if ((double) segment.length() / words < MIN_AVERAGE_WORD_LENGTH) {
                return false;
            }
        }
        return true;
    }

    // A run of consecutive characters, such as "12345678" or "abcdefg1", with at most one break in the run.
    private static boolean isConsecutive(String value) {
        int steps = 0;
        for (int i = 1; i < value.length(); i++) {
            if (Math.abs(value.charAt(i) - value.charAt(i - 1)) == 1) {
                steps++;
            }
        }
        return steps >= value.length() - 2;
    }

    // The Shannon entropy of the characters of a value, in bits per character.
    static double entropy(String value) {
        Map<Integer, Long> counts = value.codePoints().boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        double length = value.codePointCount(0, value.length());
        return counts.values().stream()
                .mapToDouble(count -> count / length)
                .map(probability -> -probability * Math.log(probability) / Math.log(2))
                .sum();
    }
}
