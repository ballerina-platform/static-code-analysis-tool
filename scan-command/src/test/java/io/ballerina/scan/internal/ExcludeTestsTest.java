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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.ballerina.scan.ScanReportTestUtils.readScanReportData;

/**
 * Tests for leaving the test sources out of a scan.
 *
 * @since 0.13.0
 */
public class ExcludeTestsTest extends BaseTest {
    private static final String ANALYZER_RULE = "exampleOrg/example_module_static_code_analyzer:1";
    private static final String TEST_FILE = "main_test.bal";

    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path testsProject = testResourcesRoot.resolve("bal-project-with-tests");

    @AfterClass
    void cleanup() {
        Path target = testsProject.resolve("target");
        if (Files.exists(target)) {
            ProjectUtils.deleteDirectory(target);
        }
    }

    @Test(description = "test the test sources are scanned by default")
    void testTestsScannedByDefault() throws IOException {
        String output = scan(testsProject);
        JsonArray issues = readJsonReport();
        Assert.assertEquals(countIssues(issues, "ballerina:1", true), 2, issues.toString());
        Assert.assertEquals(countIssues(issues, ANALYZER_RULE, true), 1, "the analyzer reports in the test file");
        Assert.assertTrue(output.contains("suppression of 'ballerina:7' did not match any issue"), output);
    }

    @Test(description = "test --exclude-tests leaves the test sources and their suppressions out of the scan")
    void testExcludeTestsFromCommandLine() throws IOException {
        String output = scan(testsProject, "--exclude-tests");
        assertTestsExcluded(output);
    }

    @Test(description = "test [analysis] excludeTests in Scan.toml leaves the test sources out of the scan")
    void testExcludeTestsFromScanToml() throws IOException {
        withScanToml("""
                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"

                [analysis]
                excludeTests = true
                """, () -> assertTestsExcluded(scan(testsProject)));
    }

    @Test(description = "test --exclude-tests overrides [analysis] excludeTests in Scan.toml either way")
    void testExcludeTestsOptionOverridesScanToml() throws IOException {
        withScanToml("""
                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"

                [analysis]
                excludeTests = true
                """, () -> {
            scan(testsProject, "--exclude-tests=false");
            JsonArray issues = readJsonReport();
            Assert.assertEquals(countIssues(issues, "ballerina:1", true), 2, "the test file is scanned: " + issues);
        });
        assertTestsExcluded(scan(testsProject, "--exclude-tests=true"));
    }

    @Test(description = "test the HTML report does not list the test sources that were not scanned")
    void testScanReportWithoutTests() throws IOException {
        scan(testsProject, "--scan-report");
        Assert.assertTrue(scannedFiles().contains(TEST_FILE), "the test file is listed by default");

        scan(testsProject, "--scan-report", "--exclude-tests");
        Assert.assertFalse(scannedFiles().contains(TEST_FILE), scannedFiles());
    }

    @Test(description = "test invalid [analysis] settings are reported at their location")
    void testInvalidAnalysisTable() throws IOException {
        withScanToml("""
                [analysis]
                excludeTests = "yes"
                excludeTest = true
                """, () -> {
            String output = scan(testsProject);
            Assert.assertTrue(output.contains("bal scan: error: Scan.toml has 2 errors\n"), output);
            Assert.assertTrue(output.contains("Scan.toml:2:16: error: invalid value 'yes' for 'excludeTests'; "
                    + "expected true or false"), output);
            Assert.assertTrue(output.contains("Scan.toml:3:1: error: unknown key 'excludeTest' in [analysis]; "
                    + "expected excludeTests"), output);
        });
    }

    @Test(description = "test --exclude-tests warns that a single file has no tests")
    void testExcludeTestsWithSingleFile() throws IOException {
        Path singleFile = testResourcesRoot.resolve("valid-single-file-project").resolve("main.bal");
        String output = scan(testsProject, "--exclude-tests", singleFile.toAbsolutePath().toString());
        Assert.assertTrue(output.contains("bal scan: warning: a single bal file has no tests to exclude, ignoring "
                + "--exclude-tests"), output);
    }

    private void assertTestsExcluded(String output) throws IOException {
        JsonArray issues = readJsonReport();
        Assert.assertEquals(countIssues(issues, "ballerina:1", false), 1, issues.toString());
        Assert.assertEquals(countIssues(issues, ANALYZER_RULE, false), 1, issues.toString());
        Assert.assertEquals(issues.size(), 2, "no issue is reported in the test file: " + issues);
        Assert.assertFalse(output.contains("did not match any issue"), "the test suppressions are skipped: "
                + output);
    }

    private static int countIssues(JsonArray issues, String ruleId, boolean inTests) {
        int count = 0;
        for (JsonElement issue : issues) {
            JsonObject issueObject = issue.getAsJsonObject();
            if (issueObject.getAsJsonObject("rule").get("id").getAsString().equals(ruleId)
                    && issueObject.get("filePath").getAsString().contains(TEST_FILE) == inTests) {
                count++;
            }
        }
        return count;
    }

    private JsonArray readJsonReport() throws IOException {
        Path report = testsProject.resolve("target").resolve("report").resolve("scan_results.json");
        return JsonParser.parseString(Files.readString(report, StandardCharsets.UTF_8)).getAsJsonArray();
    }

    private String scannedFiles() throws IOException {
        Path report = testsProject.resolve("target").resolve("report").resolve("index.html");
        return readScanReportData(report).getAsJsonArray("scannedFiles").toString();
    }

    private void withScanToml(String content, ThrowingRunnable test) throws IOException {
        Path scanToml = testsProject.resolve("Scan.toml");
        String original = Files.readString(scanToml, StandardCharsets.UTF_8);
        Files.writeString(scanToml, content);
        try {
            test.run();
        } finally {
            Files.writeString(scanToml, original);
        }
    }

    private String scan(Path project, String... args) throws IOException {
        System.setProperty("user.dir", project.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream);
            new CommandLine(scanCmd).parseArgs(args);
            scanCmd.execute();
            return readOutput(true);
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private interface ThrowingRunnable {
        void run() throws IOException;
    }
}
