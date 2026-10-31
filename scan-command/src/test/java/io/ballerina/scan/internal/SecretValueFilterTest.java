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
 * Secret value filter tests.
 *
 * @since 0.13.0
 */
public class SecretValueFilterTest {
    @DataProvider
    Object[][] placeholderValues() {
        return new Object[][]{
                {""}, {"   "}, {"${DB_PASSWORD}"}, {"{{api_key}}"}, {"<token>"}, {"<your password>"},
                {"$DB_PASSWORD"}, {"%DB_PASSWORD%"}, {"changeme"}, {"CHANGEIT"}, {"change_me"}, {"dummy"},
                {"placeholder"}, {"redacted"}, {"your-client-secret"}, {"YOUR_API_KEY"}, {"yourPassword"},
                {"your-api-key-here"}, {"xxxxxxxx"}, {"********"}
        };
    }

    @DataProvider
    Object[][] secretValues() {
        return new Object[][]{
                {"s3cr3t-value"}, {"password"}, {"admin123"}, {"Fetch"}, {"access_token"}, {"yours"},
                {"$2a$10$abcdefghijklmnopqrstuv"}, {"a<b>c"}, {"xxxxxxxy"}, {"$ecureP4ss"}, {"$k9Zq7LmP2x"},
                {"%dbPassword%"}, {"YourC0mpany!2024"}, {"your_svc_9f8a7b6c5d"}
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
}
