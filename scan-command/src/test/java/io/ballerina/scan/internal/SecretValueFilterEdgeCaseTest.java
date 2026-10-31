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
 * Boundary values of the secret value filter.
 *
 * @since 0.13.0
 */
public class SecretValueFilterEdgeCaseTest {
    @DataProvider
    Object[][] placeholderValues() {
        return new Object[][]{
                {"\t\n "}, {"  changeme  "}, {"ChangeMe"}, {"CHANGE-ME"}, {"Change_Me"}, {"ChangeIt"}, {"TBD"},
                {"Todo"}, {"Sample"}, {"EXAMPLE"}, {"Dummy"}, {"Placeholder"}, {"REDACTED"},
                {"${}"}, {"${db.password}"}, {"x{{abc}}"}, {"x${abc}"}, {"${abc}y"}, {"Prefix${DB}"},
                {"Bearer {{token}}"}, {"Bearer ${TOKEN}"}, {"${ DB_PASSWORD }"}, {"${USER}${PASS}"},
                {"{{}}"}, {"{{ .Values.password }}"}, {"<YOUR_API_KEY>"}, {"<a b c>"},
                {"$A"}, {"$DB_PASSWORD_2"}, {"%A%"}, {"%DB_PASSWORD_2%"},
                {"a"}, {"0"}, {"$"}, {"------"}, {"...."},
                {"your_token"}, {"your token here"}, {"YOUR-SECRET-HERE"}, {"yourkey"}, {"yourapikey"},
                {"YourApiKeyHere"}, {"your_credentials"}, {"your-credential"}, {"your-passphrase"}, {"your_pwds"},
                {"your password here"}, {"your-passwd"}
        };
    }

    @DataProvider
    Object[][] secretValues() {
        return new Object[][]{
                {"$ecret123"}, {"$Password1"}, {"$DB_PASSWORD$"}, {"$1"}, {"pa$$word"}, {"P@$$w0rd!"},
                {"<abc"}, {"abc>"}, {"<a<b>"}, {"<<x>>"}, {"<>"},
                {"{abc}"}, {"{{abc}"}, {"{abc}}"}, {"${abc"},
                {"%DB_PASSWORD"}, {"DB_PASSWORD%"}, {"%db_password%"}, {"100%"},
                {"yourpasswordis9876"}, {"your-key-9f8a7b"}, {"your secret: hunter22"}, {"my-api-key"},
                {"Your$ecretKey!"}, {"your"}, {"yourself123"},
                {"aa1"}, {"aaaaab"}, {"ab"}, {"Aa"},
                {"changeme1"}, {"change me"}, {"changeme!"}, {"dummy-token-12"}, {"redacted-but-real"},
                {"todo123"}, {"examples"}
        };
    }

    @Test(dataProvider = "placeholderValues")
    void testPlaceholderValue(String value) {
        Assert.assertTrue(SecretValueFilter.isPlaceholder(value), value);
    }

    @Test(dataProvider = "secretValues")
    void testSecretValue(String value) {
        Assert.assertFalse(SecretValueFilter.isPlaceholder(value), value);
    }

    @DataProvider
    Object[][] literalValues() {
        return new Object[][]{
                {"\"plain\"", "plain"}, {"\"\\tchangeme\"", "\tchangeme"}, {"\"a\\\"b\"", "a\"b"},
                {"\"back\\\\slash\"", "back\\slash"}, {"\"\\u{63}hangeme\"", "changeme"}, {"\"\"", ""},
                {"\"\\u{110000}\"", "\\u{110000}"}, {"\"a\\nb\\rc\"", "a\nb\rc"}, {"unquoted", "unquoted"}
        };
    }

    @Test(dataProvider = "literalValues")
    void testLiteralValue(String literalText, String value) {
        Assert.assertEquals(SecretValueFilter.literalValue(literalText), value);
    }

    @Test
    void testEscapedPlaceholders() {
        Assert.assertTrue(SecretValueFilter.isPlaceholder(SecretValueFilter.literalValue("\"\\tchangeme\"")));
        Assert.assertTrue(SecretValueFilter.isPlaceholder(SecretValueFilter.literalValue("\"\\u{63}hangeme\"")));
    }
}
