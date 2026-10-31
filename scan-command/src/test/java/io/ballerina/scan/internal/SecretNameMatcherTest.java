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

/**
 * Secret name matcher tests.
 *
 * @since 0.13.0
 */
public class SecretNameMatcherTest {
    @DataProvider
    Object[][] secretNames() {
        return new Object[][]{
                {"password"}, {"passwd"}, {"pwd"}, {"passphrase"}, {"passcode"}, {"secret"}, {"token"},
                {"auth"}, {"creds"}, {"bearer"}, {"PASSWORD"}, {"dbPassword"}, {"dbpassword"},
                {"keyPassword"}, {"clientSecret"}, {"clientsecret"}, {"sharedSecret"}, {"secretKey"},
                {"apiKey"}, {"apikey"}, {"API_KEY"}, {"APIKey"}, {"x-api-key"}, {"accessToken"},
                {"refresh_token"}, {"x-csrf-token"}, {"authToken"}, {"OAuthToken"}, {"JWTSecret"},
                {"\"Authorization\""}, {"proxyAuthorization"}, {"dbCredential"}, {"credentials"},
                {"privateKey"}, {"accessKey"}, {"clientKey"}, {"tokens"}, {"'password"}, {"accesstoken"},
                {"authtoken"}, {"refreshtoken"}, {"xauthtoken"}, {"dbpwd"}, {"basicauth"}, {"awscreds"}, {"token2"},
                {"apiToken1"}, {"pwd1"}, {"githubToken2"}, {"tokenForPaymentProvider"}, {"apiKeyForUrlShortener"},
                {"passwordForFileServer"}, {"clientSecretFromVault"}, {"ssoAuth"}, {"cognitoAuth"},
                {"myOAuthToken"}, {"OAUTH_TOKEN"}, {"apiKeys"}, {"API_KEYS"}, {"accessKeys"}, {"privateKeys"},
                {"passCodes"}, {"pwds"}, {"pass\\u{0077}ord"}, {"api\\-key"}
        };
    }

    @DataProvider
    Object[][] nonSecretNames() {
        return new Object[][]{
                {"username"}, {"author"}, {"authority"}, {"authenticate"}, {"tokenizer"}, {"secretary"},
                {"oauth"}, {"basicAuthEnabled"}, {"tokenTypeHint"}, {"tokenResponse"}, {"tokenUrl"},
                {"tokenEndpoint"}, {"passwordFilePath"}, {"tokenUrlTemplate"}, {"secretKeyName"},
                {"accessKeyId"}, {"passwordStrength"}, {"token_type"}, {"auth-scheme"}, {"tokenExpiryTime"},
                {"\"x-token-hint\""}, {"key"}, {"publicKey"}, {"keyStore"}, {""}, {"bypassCode"}, {"maxSecRetry"},
                {"accessKeyword"}, {"myOAuth"}, {"oauthConfig"}, {"tokenUrlForProvider"}, {"tokenUrls"},
                {"passwordFiles"}, {"secretNames"}, {"tokenEndpoints"}, {"authURLs"}, {"secretIDs"}, {"tokenARNs"},
                {"myoauth"}, {"googleoauth"}, {"basicAuthUsername"}, {"authUser"}, {"proxyAuthHost"},
                {"authorizationServer"}, {"token_endpoint_auth_signing_alg"},
                {"id_token_signing_alg_values_supported"}, {"tokenEndpointAuthMethodsSupported"}, {"passwordHashes"},
                {"passwordPolicies"}, {"tokenIndexes"}, {"tokenStatuses"}, {"tokenPrefixes"}, {"secretAliases"},
                {"tokenHostname"}, {"secretAlgorithm"}, {"apiKeyz"}
        };
    }

    @Test(dataProvider = "secretNames")
    void testSecretName(String name) {
        Assert.assertTrue(SecretNameMatcher.isSecretName(name), name);
    }

    @Test(dataProvider = "nonSecretNames")
    void testNonSecretName(String name) {
        Assert.assertFalse(SecretNameMatcher.isSecretName(name), name);
    }
}
