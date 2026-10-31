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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Edge case tests for the secret name matcher.
 *
 * @since 0.13.0
 */
public class SecretNameMatcherEdgeCaseTest {
    // The production lists, so that every keyword, preposition and descriptor is covered as the lists change.
    private static final List<String> EMBEDDED_KEYWORDS = SecretNameMatcher.EMBEDDED_KEYWORDS;
    private static final List<String> WORD_KEYWORDS = SecretNameMatcher.WORD_KEYWORDS;
    private static final List<String> PREPOSITIONS = SecretNameMatcher.PREPOSITIONS.stream().sorted().toList();
    private static final List<String> DESCRIPTORS = SecretNameMatcher.DESCRIPTORS.stream().sorted().toList();

    @DataProvider
    Object[][] casingVariants() {
        return new Object[][]{
                // UPPER_SNAKE
                {"DB_PASSWORD", true}, {"JWT_SECRET", true}, {"GITHUB_TOKEN", true}, {"AWS_SECRET_ACCESS_KEY", true},
                {"OAUTH_CLIENT_SECRET", true}, {"API_KEY", true}, {"TOKEN_FOR_URL", true}, {"TOKEN_URL", false},
                {"AUTH_TYPE", false}, {"PASSWORD_FILE_PATH", false}, {"AWS_ACCESS_KEY_ID", false},
                {"OAUTH_CLIENT_ID", false}, {"AUTHOR", false}, {"TOKENIZER", false}, {"SECRETARY", false},
                // Train-Case and kebab-case
                {"Client-Secret", true}, {"Access-Token", true}, {"Api-Key", true}, {"X-Auth-Token", true},
                {"Proxy-Authorization", true}, {"Token-Url", false}, {"Password-File", false},
                {"Token-Type-Hint", false},
                // all lower and all upper single words
                {"clientsecret", true}, {"secretkey", true}, {"privatekey", true}, {"accesskey", true},
                {"bearertoken", true}, {"APIKEY", true}, {"CLIENTSECRET", true}, {"tokenurl", false},
                {"authority", false},
                // camelCase and PascalCase with split keywords
                {"passWord", true}, {"PassWord", true}, {"pass_word", true}, {"passCode", true},
                {"passPhrase", true}, {"passWd", true}, {"apiKEY", true}, {"ApiKey", true}, {"PrivateKey", true},
                {"ClientSecret", true}, {"bypassCode", false}, {"compassCode", false}, {"rapiKey", false},
                {"apiKeyboard", false},
                // acronyms
                {"JWTToken", true}, {"HTTPAuthHeader", true}, {"IDToken", true}, {"SAMLToken", true},
                {"AWSSecretAccessKey", true}, {"MySQLPassword", true}, {"DBPassword", true}, {"APIKeyHeader", true},
                {"XAuthToken", true}, {"SSOAuthToken", true}, {"OAuthClientSecret", true}, {"dbURLPassword", true},
                {"APIKeyID", false}, {"TokenID", false}, {"OAuthURL", false}, {"OAuthClientId", false},
                {"JWTTokenTTL", false}, {"HTTPAuthType", false}, {"SSLCertPath", false}, {"XMLParser", false}
        };
    }

    @DataProvider
    Object[][] digits() {
        return new Object[][]{
                {"password1", true}, {"password123", true}, {"pwd2", true}, {"2faToken", true},
                {"s3SecretKey", true}, {"base64Secret", true}, {"md5Password", true}, {"pkcs12Password", true},
                {"x509PrivateKey", true}, {"token64", true}, {"API_KEY_2", true}, {"oauth2Token", true},
                {"OAuth2ClientSecret", true}, {"oauth2", false}, {"OAuth2", false}, {"oauth2Config", false},
                {"useOAuth2", false}, {"OAuth2ClientId", false}, {"password2Hash", false},
                {"pkcs12PasswordFile", false}, {"aes256Key", false}, {"sha256Digest", false}, {"token2Url", false},
                {"123", false}, {"2fa", false}
        };
    }

    @DataProvider
    Object[][] separatorsAndQuoting() {
        return new Object[][]{
                {"_password", true}, {"password_", true}, {"__token__", true}, {"-api-key-", true},
                {"password__", true}, {"  secret  ", true}, {"$apiKey", true}, {"_tokenUrl_", false},
                {"__password__file__", false}, {"-token-type-", false},
                // quoted identifiers, including escaped characters
                {"'apiKey", true}, {"'secret", true}, {"'db\\-password", true}, {"'client\\.secret", true},
                {"'tokenUrl", false}, {"'token\\-url", false},
                // quoted mapping keys
                {"\"api key\"", true}, {"\"client.secret\"", true}, {"\"x-auth-token\"", true},
                {"\"Proxy-Authorization\"", true}, {"\" password \"", true}, {"\"spring.datasource.password\"", true},
                {"\"oauth.client.secret\"", true}, {"\"db.password.file\"", false}, {"\"token url\"", false},
                {"\"oauth.client.id\"", false}, {"\"x-request-id\"", false}, {"\"content-type\"", false}
        };
    }

    @DataProvider
    Object[][] unicodeAndEmpty() {
        return new Object[][]{
                {"", false}, {" ", false}, {"_", false}, {"'", false}, {"\"\"", false}, {"--", false},
                {"\t\n", false},
                // non-ASCII characters separate words like punctuation does
                {"密码_password", true}, {"密码Password", true}, {"tokenΩ", true}, {"Ωtoken", true},
                {"café_token", true}, {"🔑apiKey", true}, {"パスワード", false}, {"秘密", false},
                {"🔑", false}, {"tokenΩUrl", false}
        };
    }

    @DataProvider
    Object[][] severalKeywords() {
        return new Object[][]{
                {"passwordToken", true}, {"tokenSecret", true}, {"apiKeyAndSecret", true}, {"authTokenSecret", true},
                {"tokenUrlSecret", true}, {"secretPathToken", true}, {"passwordFileAndToken", true},
                {"tokenizerSecret", true}, {"authorPassword", true}, {"secretaryPassword", true},
                {"authorToken", true}, {"keyStorePassword", true}, {"trustStorePassword", true},
                {"privateKeyPassphrase", true}, {"bearerAuthHeader", true},
                {"authTokenUrl", false}, {"secretTokenUrl", false}, {"passwordOrTokenName", false},
                {"apiKeySecretName", false}, {"clientSecretTokenEndpoint", false},
                {"tokenEndpointAuthMethod", false}, {"clientSecretExpiresAt", false},
                {"refresh_token_expires_in", false}, {"secretaryName", false}
        };
    }

    @DataProvider
    Object[][] prepositionsAndDescriptors() {
        return new Object[][]{
                {"tokenForUrl", true}, {"passwordOfFileName", true}, {"secretFromVaultPath", true},
                {"apiKeyFromFile", true}, {"tokenToId", true}, {"credentialsViaPath", true},
                {"passwordInFile", true}, {"authOnRequest", true}, {"tokenAtExpiry", true},
                {"bearerByName", true}, {"pwdWithHint", true}, {"tokenFor", true}, {"urlForToken", true},
                {"pathToSecret", true}, {"secretPathForToken", true}, {"forToken", true},
                {"tokenUrlForProvider", false}, {"tokenIdForUser", false}, {"tokenNameForUser", false},
                {"passwordFileFromVault", false}, {"secretTypeOfUrl", false}
        };
    }

    @DataProvider
    Object[][] everyDescriptor() {
        List<Object[]> cases = new ArrayList<>();
        for (String descriptor : DESCRIPTORS) {
            String capitalized = capitalize(descriptor);
            cases.add(new Object[]{"token" + capitalized, false});
            cases.add(new Object[]{"password_" + descriptor, false});
            cases.add(new Object[]{"API_KEY_" + descriptor.toUpperCase(Locale.ENGLISH), false});
            cases.add(new Object[]{"client-secret-" + descriptor, false});
            cases.add(new Object[]{"tokenFor" + capitalized, true});
            cases.add(new Object[]{"secret_from_" + descriptor, true});
            cases.add(new Object[]{descriptor + "Token", true});
            cases.add(new Object[]{descriptor + "_password", true});
        }
        return cases.toArray(Object[][]::new);
    }

    @DataProvider
    Object[][] everyPreposition() {
        List<Object[]> cases = new ArrayList<>();
        for (String preposition : PREPOSITIONS) {
            String capitalized = capitalize(preposition);
            cases.add(new Object[]{"token" + capitalized + "Url", true});
            cases.add(new Object[]{"password_" + preposition + "_file_path", true});
            cases.add(new Object[]{"tokenUrl" + capitalized + "Provider", false});
            cases.add(new Object[]{"secretName" + capitalized + "Token", true});
        }
        return cases.toArray(Object[][]::new);
    }

    @DataProvider
    Object[][] everyKeyword() {
        List<Object[]> cases = new ArrayList<>();
        for (String keyword : EMBEDDED_KEYWORDS) {
            cases.add(new Object[]{keyword, true});
            cases.add(new Object[]{keyword + "s", true});
            cases.add(new Object[]{"db" + keyword, true});
            cases.add(new Object[]{"my" + keyword + "value", true});
            cases.add(new Object[]{"MY_" + keyword.toUpperCase(Locale.ENGLISH), true});
            cases.add(new Object[]{capitalize(keyword), true});
            cases.add(new Object[]{keyword + "Url", false});
            cases.add(new Object[]{keyword + "_file", false});
        }
        for (String keyword : WORD_KEYWORDS) {
            cases.add(new Object[]{keyword, true});
            cases.add(new Object[]{"my" + keyword, true});
            cases.add(new Object[]{"my_" + keyword, true});
            cases.add(new Object[]{"my" + capitalize(keyword), true});
            cases.add(new Object[]{keyword.toUpperCase(Locale.ENGLISH), true});
            cases.add(new Object[]{keyword + "2", true});
            cases.add(new Object[]{keyword + "Url", false});
            cases.add(new Object[]{keyword + "_type", false});
        }
        return cases.toArray(Object[][]::new);
    }

    @DataProvider
    Object[][] pluralsAndWordPrefixes() {
        return new Object[][]{
                {"passwords", true}, {"dbPasswords", true}, {"secrets", true}, {"clientSecrets", true},
                {"credentials", true}, {"Credentials", true}, {"passphrases", true}, {"passcodes", true},
                {"apikeys", true}, {"privatekeys", true}, {"authorizations", true}, {"tokens", true},
                {"accessTokens", true}, {"refresh_tokens", true}, {"awscreds", true},
                {"author", false}, {"authored", false}, {"authority", false}, {"authenticate", false},
                {"authorize", false}, {"tokenize", false}, {"tokenized", false}, {"tokenizers", false},
                {"secretary", false}, {"secretaries", false}, {"secretariat", false}, {"SECRETARIES", false},
                {"oauth", false}, {"OAUTH", false}, {"myOAuth", false}, {"key", false}, {"keys", false},
                {"publicKey", false}, {"accessKeyword", false}, {"maxSecRetry", false}
        };
    }

    @Test(dataProvider = "casingVariants")
    void testCasingVariants(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "digits")
    void testDigits(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "separatorsAndQuoting")
    void testSeparatorsAndQuoting(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "unicodeAndEmpty")
    void testUnicodeAndEmpty(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "severalKeywords")
    void testSeveralKeywords(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "prepositionsAndDescriptors")
    void testPrepositionsAndDescriptors(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "everyDescriptor")
    void testEveryDescriptor(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "everyPreposition")
    void testEveryPreposition(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "everyKeyword")
    void testEveryKeyword(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test(dataProvider = "pluralsAndWordPrefixes")
    void testPluralsAndWordPrefixes(String name, boolean expected) {
        assertSecretName(name, expected);
    }

    @Test
    void testNamesAreLowerCasedIndependentlyOfTheDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertSecretName("AUTHORIZATION", true);
            assertSecretName("API_KEY", true);
            assertSecretName("CLIENT_SECRET", true);
            assertSecretName("PRIVATE_KEY", true);
            assertSecretName("TOKEN_ID", false);
            assertSecretName("PASSWORD_VALIDITY_ID", false);
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    private static void assertSecretName(String name, boolean expected) {
        Assert.assertEquals(SecretNameMatcher.isSecretName(name), expected, name);
    }

    private static String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
