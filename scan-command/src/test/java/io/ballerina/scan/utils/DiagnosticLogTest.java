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
import io.ballerina.scan.RuleKind;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;

/**
 * Diagnostic log tests.
 *
 * @since 0.1.0
 */
public class DiagnosticLogTest extends BaseTest {
    private final Path commandOutputs = testResources.resolve("command-outputs");

    @Test(description = "Test error diagnostic message")
    public void testErrorDiagnostic() throws IOException {
        String expected = "Invalid number of arguments, expected one argument received 0";
        String result = DiagnosticLog.error(DiagnosticCode.INVALID_NUMBER_OF_ARGUMENTS, 0);
        Assert.assertEquals(result, expected);
        expected = "Package is empty. Please add at least one .bal file";
        result = DiagnosticLog.error(DiagnosticCode.EMPTY_PACKAGE);
        Assert.assertEquals(result, expected);
        expected = "Failed to load scan tool configurations: cannot find 'configPath'";
        result = DiagnosticLog.error(DiagnosticCode.MISSING_CONFIG_FIELD);
        Assert.assertEquals(result, expected);
        expected = "Failed to load configuration file: Configurations.toml";
        result = DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_CONFIG_FILE, "Configurations.toml");
        Assert.assertEquals(result, expected);
        expected = "Failed to read the configuration file: Configurations.toml";
        result = DiagnosticLog.error(DiagnosticCode.READING_CONFIG_FILE, "Configurations.toml");
        Assert.assertEquals(result, expected);
        expected = "Failed to retrieve remote platform file: www.example.com/example-platform-analyzer-1.0.jar";
        result = DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_PLATFORM_FILE,
                "www.example.com/example-platform-analyzer-1.0.jar");
        Assert.assertEquals(result, expected);
        expected = "Failed to download remote JAR file: www.example.com/example-platform-analyzer-1.0.jar";
        result = DiagnosticLog.error(DiagnosticCode.DOWNLOADING_REMOTE_JAR_FILE,
                "www.example.com/example-platform-analyzer-1.0.jar");
        Assert.assertEquals(result, expected);
        Path commandOutputFile = commandOutputs.resolve("invalid-json-format-for-rules.txt");
        expected = Files.readString(commandOutputFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR).trim();
        result = DiagnosticLog.error(DiagnosticCode.INVALID_JSON_FORMAT, "rules.json", "exampleOrg/exampleName",
                "{}");
        Assert.assertEquals(result, expected);
        commandOutputFile = commandOutputs.resolve("invalid-json-format-for-rule.txt");
        expected = Files.readString(commandOutputFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR).trim();
        result = DiagnosticLog.error(DiagnosticCode.INVALID_JSON_FORMAT_RULE, "exampleOrg/exampleName",
                "{'id':'1', 'severity':'BUG', 'description':'rule 1'}");
        Assert.assertEquals(result, expected);
        commandOutputFile = commandOutputs.resolve("invalid-json-format-for-rule-kind.txt");
        expected = Files.readString(commandOutputFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR).trim();
        result = DiagnosticLog.error(DiagnosticCode.INVALID_JSON_FORMAT_RULE_KIND, "exampleOrg/exampleName",
                Arrays.toString(RuleKind.values()), "SECURITY_HOTSPOT");
        Assert.assertEquals(result, expected);
        expected = "Failed to read the file with exception: file not found";
        result = DiagnosticLog.error(DiagnosticCode.FAILED_TO_READ_FILE, "file not found");
        Assert.assertEquals(result, expected);
        expected = "Failed to copy the file with exception: io error";
        result = DiagnosticLog.error(DiagnosticCode.FAILED_TO_COPY_FILE, "io error");
        Assert.assertEquals(result, expected);
        expected = "Rule not found: Invalid rule numeric identifier '999'";
        result = DiagnosticLog.error(DiagnosticCode.RULE_NOT_FOUND, 999);
        Assert.assertEquals(result, expected);
    }

    @Test(description = "Test warning diagnostic message")
    public void testWarningDiagnostic() {
        String expected = "Generating reports is not supported with single bal files. " +
                "Ignoring the flag and continuing the scans";
        String result = DiagnosticLog.warning(DiagnosticCode.REPORT_NOT_SUPPORTED);
        Assert.assertEquals(result, expected);
        expected = "Generating scan reports is not supported with single bal files. " +
                "Ignoring the flag and continuing the scans";
        result = DiagnosticLog.warning(DiagnosticCode.SCAN_REPORT_NOT_SUPPORTED);
        Assert.assertEquals(result, expected);
    }
}
