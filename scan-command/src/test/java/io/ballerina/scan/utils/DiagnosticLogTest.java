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

import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Diagnostic log tests.
 *
 * @since 0.1.0
 */
public class DiagnosticLogTest extends BaseTest {
    @Test(description = "Test error diagnostic message")
    public void testErrorDiagnostic() throws IOException {
        String expected = "invalid number of arguments, expected one argument received 0";
        String result = DiagnosticLog.error(DiagnosticCode.INVALID_NUMBER_OF_ARGUMENTS, 0);
        Assert.assertEquals(result, expected);
    }

    @Test(description = "Test warning diagnostic message")
    public void testWarningDiagnostic() {
        String expected = "generating reports is not supported with single bal files, " +
                "ignoring the flag and continuing the scans";
        String result = DiagnosticLog.warning(DiagnosticCode.REPORT_NOT_SUPPORTED);
        Assert.assertEquals(result, expected);
    }
}
