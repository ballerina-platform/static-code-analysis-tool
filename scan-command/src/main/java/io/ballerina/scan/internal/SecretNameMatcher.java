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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

    /**
     * A secret keyword.
     *
     * @param text       the keyword in lower case
     * @param isEmbedded whether it matches anywhere, as in "dbpassword", or only as a word or at the end of one, as
     *                   "token" does, since it starts unrelated words such as "tokenizer"
     * @param kind       the kind of secret it names; a password is chosen by a person, so any value of it can be real
     */
    private record Keyword(String text, boolean isEmbedded, Kind kind) {
    }

    private static final List<Keyword> KEYWORDS = List.of(
            new Keyword("password", true, Kind.PASSWORD),
            new Keyword("passwd", true, Kind.PASSWORD),
            new Keyword("passphrase", true, Kind.PASSWORD),
            new Keyword("passcode", true, Kind.PASSWORD),
            new Keyword("pwd", false, Kind.PASSWORD),
            new Keyword("secret", true, Kind.SECRET),
            new Keyword("apikey", true, Kind.SECRET),
            new Keyword("credential", true, Kind.SECRET),
            new Keyword("privatekey", true, Kind.SECRET),
            new Keyword("accesskey", true, Kind.SECRET),
            new Keyword("clientkey", true, Kind.SECRET),
            new Keyword("authorization", true, Kind.SECRET),
            new Keyword("auth", false, Kind.SECRET),
            new Keyword("token", false, Kind.SECRET),
            new Keyword("creds", false, Kind.SECRET),
            new Keyword("bearer", false, Kind.SECRET));

    static final List<String> EMBEDDED_KEYWORDS = KEYWORDS.stream()
            .filter(Keyword::isEmbedded).map(Keyword::text).toList();

    static final List<String> WORD_KEYWORDS = KEYWORDS.stream()
            .filter(keyword -> !keyword.isEmbedded()).map(Keyword::text).toList();

    // Words that contain a keyword but name something else, as in "undersecretary".
    private static final Set<String> NON_SECRET_WORDS = Set.of("secretar", "passwordless");

    // A hashed secret, as in "hashedPassword", is no longer the secret itself.
    private static final String HASHED = "hashed";

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

    /**
     * The kind of secret a name suggests. A password is chosen by a person, so any value of it can be real. Other
     * secrets, such as tokens and keys, are generated, so only a value that looks generated can be one.
     */
    enum Kind {
        NONE,
        SECRET,
        PASSWORD;

        boolean isSecret() {
            return this != NONE;
        }

        // A password is the strictest kind, as any value of it can be real.
        Kind stricter(Kind other) {
            return compareTo(other) >= 0 ? this : other;
        }
    }

    static boolean isSecretName(String name) {
        return kind(name) != Kind.NONE;
    }

    static Kind kind(String name) {
        // Escapes in an identifier or key, such as api\-key, are matched by the characters they encode.
        List<String> words = splitIntoWords(SecretValueFilter.literalValue(name));
        List<KeywordMatch> keywords = keywords(words);
        Optional<KeywordMatch> lastKeyword = keywords.stream().max(Comparator.comparingInt(KeywordMatch::end));
        if (lastKeyword.isEmpty() || words.subList(0, lastKeyword.get().wordIndex()).contains(HASHED)) {
            return Kind.NONE;
        }
        int lastKeywordIndex = lastKeyword.get().wordIndex();
        // The last word before any preposition names what the value is, as in "passwordFilePath" and
        // "privateKeyFileContent".
        int headWordIndex = lastKeywordIndex;
        for (int i = lastKeywordIndex + 1; i < words.size() && !PREPOSITIONS.contains(words.get(i)); i++) {
            headWordIndex = i;
        }
        if (headWordIndex != lastKeywordIndex && isDescriptor(words.get(headWordIndex))) {
            return Kind.NONE;
        }
        // A name that mentions a password names one, as in "passwordToken", since any value of it can be real
        return keywords.stream().anyMatch(keyword -> keyword.kind() == Kind.PASSWORD) ? Kind.PASSWORD : Kind.SECRET;
    }

    /**
     * A keyword found in a name.
     *
     * @param wordIndex the index of the word it ends in
     * @param end       where it ends in the name, with the words joined
     * @param kind      the kind of secret it names
     */
    private record KeywordMatch(int wordIndex, int end, Kind kind) {
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

    // The keywords found in the name, wherever they end, as "password" in "dbpasswordvalue".
    private static List<KeywordMatch> keywords(List<String> words) {
        List<KeywordMatch> matches = new ArrayList<>();
        StringBuilder joinedWords = new StringBuilder();
        int[] wordStarts = new int[words.size() + 1];
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            wordStarts[i] = joinedWords.length();
            joinedWords.append(word);
            // "oauth" ends with the keyword "auth" but names a protocol, as in "googleoauth".
            if (word.endsWith("oauth")) {
                continue;
            }
            for (Keyword keyword : KEYWORDS) {
                if (!keyword.isEmbedded() && (word.endsWith(keyword.text()) || word.endsWith(keyword.text() + "s"))) {
                    matches.add(new KeywordMatch(i, joinedWords.length(), keyword.kind()));
                }
            }
        }
        wordStarts[words.size()] = joinedWords.length();

        String joined = joinedWords.toString();
        for (Keyword keyword : KEYWORDS) {
            if (!keyword.isEmbedded()) {
                continue;
            }
            for (int start = joined.indexOf(keyword.text()); start >= 0;
                 start = joined.indexOf(keyword.text(), start + 1)) {
                int end = start + keyword.text().length();
                int firstWord = wordAt(wordStarts, start);
                int lastWord = wordAt(wordStarts, end - 1);
                // A keyword spanning words, such as "apiKey" or "apiKeys", must not start or end inside one, as in
                // "bypassCode".
                int nextWordStart = wordStarts[lastWord + 1];
                boolean endsWord = nextWordStart == end || (nextWordStart == end + 1 && joined.charAt(end) == 's');
                boolean spansWholeWords = firstWord == lastWord || (wordStarts[firstWord] == start && endsWord);
                if (spansWholeWords && NON_SECRET_WORDS.stream().noneMatch(words.get(firstWord)::contains)) {
                    matches.add(new KeywordMatch(lastWord, end, keyword.kind()));
                }
            }
        }
        return matches;
    }

    private static int wordAt(int[] wordStarts, int offset) {
        int word = 0;
        while (wordStarts[word + 1] <= offset) {
            word++;
        }
        return word;
    }
}
