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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * {@code SecretNameMatcher} decides whether the name of a variable, field, parameter, argument or mapping key
 * suggests that its value is a secret.
 *
 * <p>A name is split into words. It matches when it contains a secret keyword and no word after the last keyword
 * describes something other than the secret itself, such as {@code tokenUrl} or {@code passwordFilePath}.</p>
 *
 * @since 0.13.0
 */
final class SecretNameMatcher {
    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^A-Za-z0-9]+");
    private static final Pattern WORD_BOUNDARY = Pattern.compile(
            "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[A-Za-z])(?=[0-9])|(?<=[0-9])(?=[A-Za-z])");
    // "OAuth" at the start of a word, but not the "oAuth" in "ssoAuth".
    private static final Pattern OAUTH = Pattern.compile("(?:(?<![A-Za-z])[Oo]|(?<=[a-z])O)(?i:auth)");
    // The plural of an acronym, such as the "URLs" in "authURLs", is one word rather than "UR" and "Ls".
    private static final Pattern PLURAL_ACRONYM = Pattern.compile("([A-Z]{2,})s(?![a-z])");

    // Matched anywhere in the name, so that all-lowercase compounds such as "dbpassword" and "apikey" match.
    static final List<String> EMBEDDED_KEYWORDS = List.of("password", "passwd", "passphrase", "passcode",
            "secret", "apikey", "credential", "privatekey", "accesskey", "clientkey", "authorization");

    // Matched as whole words, or at the end of an all-lowercase compound such as "accesstoken", since they are also
    // the start of unrelated words such as "author" and "tokenizer".
    static final List<String> WORD_KEYWORDS = List.of("pwd", "pwds", "auth", "token", "tokens", "creds",
            "bearer");

    private static final Set<String> NON_SECRET_WORDS = Set.of("secretary", "secretaries", "secretariat");

    // A descriptor after one of these words qualifies something else, as in "tokenForPaymentProvider".
    static final Set<String> PREPOSITIONS = Set.of("for", "from", "of", "to", "in", "on", "at", "by", "with",
            "via");

    static final Set<String> DESCRIPTORS = Set.of("enabled", "disabled", "required", "type", "hint", "url",
            "uri", "endpoint", "path", "file", "dir", "directory", "name", "id", "ids", "response", "request",
            "method", "expiry", "expires", "expiration", "time", "timeout", "ttl", "length", "size", "count", "index",
            "hash", "digest", "regex", "pattern", "policy", "rule", "label", "message", "msg", "error", "prompt",
            "placeholder", "field", "param", "scheme", "mode", "provider", "issuer", "audience", "format",
            "encoding", "version", "alias", "ref", "arn", "store", "vault", "scope", "scopes", "location", "prefix",
            "suffix", "template", "handler", "strength", "validator", "status", "valid", "flag", "user", "username",
            "host", "hostname", "server", "alg", "algorithm", "supported");

    private SecretNameMatcher() {
    }

    static boolean isSecretName(String name) {
        // Escapes in an identifier or key, such as api\-key, are matched by the characters they encode.
        List<String> words = splitIntoWords(SecretValueFilter.literalValue(name));
        int lastKeywordIndex = lastKeywordWordIndex(words);
        if (lastKeywordIndex < 0) {
            return false;
        }
        for (String word : words.subList(lastKeywordIndex + 1, words.size())) {
            if (PREPOSITIONS.contains(word)) {
                return true;
            }
            if (isDescriptor(word)) {
                return false;
            }
        }
        return true;
    }

    // A descriptor or its plural, such as "urls", "hashes" or "policies".
    private static boolean isDescriptor(String word) {
        return DESCRIPTORS.contains(word)
                || (word.endsWith("s") && DESCRIPTORS.contains(word.substring(0, word.length() - 1)))
                || (word.endsWith("es") && DESCRIPTORS.contains(word.substring(0, word.length() - 2)))
                || (word.endsWith("ies") && DESCRIPTORS.contains(word.substring(0, word.length() - 3) + "y"));
    }

    private static List<String> splitIntoWords(String name) {
        // Keeps "OAuth" as one word, so that it is not split into "O" and the keyword "Auth".
        String normalizedName = PLURAL_ACRONYM.matcher(OAUTH.matcher(name).replaceAll("Oauth")).replaceAll("$1S");
        List<String> words = new ArrayList<>();
        for (String part : WORD_SEPARATOR.split(normalizedName)) {
            for (String word : WORD_BOUNDARY.split(part)) {
                if (!word.isEmpty()) {
                    words.add(word.toLowerCase(Locale.ENGLISH));
                }
            }
        }
        return words;
    }

    private static int lastKeywordWordIndex(List<String> words) {
        int lastIndex = -1;
        StringBuilder joinedWords = new StringBuilder();
        int[] wordStarts = new int[words.size() + 1];
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            // "oauth" ends with the keyword "auth" but names a protocol, as in "googleoauth".
            if (!word.endsWith("oauth") && WORD_KEYWORDS.stream().anyMatch(word::endsWith)) {
                lastIndex = i;
            }
            wordStarts[i] = joinedWords.length();
            joinedWords.append(word);
        }
        wordStarts[words.size()] = joinedWords.length();

        String joined = joinedWords.toString();
        for (String keyword : EMBEDDED_KEYWORDS) {
            for (int start = joined.indexOf(keyword); start >= 0; start = joined.indexOf(keyword, start + 1)) {
                int end = start + keyword.length();
                int firstWord = wordAt(wordStarts, start);
                int lastWord = wordAt(wordStarts, end - 1);
                // A keyword spanning words, such as "apiKey" or "apiKeys", must not start or end inside one, as in
                // "bypassCode".
                int nextWordStart = wordStarts[lastWord + 1];
                boolean endsWord = nextWordStart == end || (nextWordStart == end + 1 && joined.charAt(end) == 's');
                boolean spansWholeWords = firstWord == lastWord || (wordStarts[firstWord] == start && endsWord);
                if (spansWholeWords && !NON_SECRET_WORDS.contains(words.get(firstWord))) {
                    lastIndex = Math.max(lastIndex, lastWord);
                }
            }
        }
        return lastIndex;
    }

    private static int wordAt(int[] wordStarts, int offset) {
        int word = 0;
        while (wordStarts[word + 1] <= offset) {
            word++;
        }
        return word;
    }
}
