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

import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.utils.GateSeverity;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Tests for the validation of the tables and keys of a Scan.toml file.
 *
 * @since 0.14.0
 */
public class ScanTomlValidationTest extends BaseTest {
    private static final String TABLES = "[[analyzer]], [[platform]], [rule], [suppression], [gate] or [analysis]";
    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path project = testResourcesRoot.resolve("bal-project-with-scan-toml-errors");
    private final Path scanToml = project.resolve("Scan.toml");
    private final Path workspace = testResourcesRoot.resolve("workspace-with-gates");
    private final Path workspaceScanToml = workspace.resolve("Scan.toml");

    @AfterMethod(alwaysRun = true)
    void restoreFiles() throws IOException {
        Files.deleteIfExists(scanToml);
        Files.deleteIfExists(workspaceScanToml);
    }

    @DataProvider(name = "invalid-scan-tomls")
    public Object[][] invalidScanTomls() {
        return new Object[][]{
                // A misspelled table turns its settings off
                {"""
                        [gates]
                        failOnCount = 1
                        """, List.of("Scan.toml:1:1: error: unknown table 'gates'; expected " + TABLES)},
                {"""
                        [rules]
                        include = ["ballerina:1"]
                        """, List.of("Scan.toml:1:1: error: unknown table 'rules'; expected " + TABLES)},
                {"""
                        [[gates]]
                        failOnCount = 1
                        """, List.of("Scan.toml:1:1: error: unknown table 'gates'; expected " + TABLES)},
                {"""
                        foo = 1
                        """, List.of("Scan.toml:1:1: error: unknown key 'foo'; expected " + TABLES)},
                {"""
                        analyzer = [1]
                        """, List.of("Scan.toml:1:1: error: 'analyzer' must be written as [[analyzer]] tables, or as "
                        + "analyzer = []")},
                {"""
                        [platform]
                        name = "examplePlatform"
                        path = "plugin.jar"
                        """, List.of("Scan.toml:1:1: error: 'platform' must be written as [[platform]] tables, or as "
                        + "platform = []")},
                // A name or path of another type would otherwise read as missing
                {"""
                        [[platform]]
                        name = "examplePlatform"
                        path = 10
                        """, List.of("Scan.toml:3:8: error: invalid value '10' for 'path'; expected the path or URL of "
                        + "the plugin JAR")},
                {"""
                        [[platform]]
                        name = 10
                        path = "plugin.jar"
                        """, List.of("Scan.toml:2:8: error: invalid value '10' for 'name'; expected a platform name, "
                        + "such as \"sonarqube\"")},
                {"""
                        platform = "plugin.jar"
                        """, List.of("Scan.toml:1:1: error: 'platform' must be written as [[platform]] tables, or as "
                        + "platform = []")},
                {"""
                        platform = ["plugin.jar"]
                        """, List.of("Scan.toml:1:1: error: 'platform' must be written as [[platform]] tables, or as "
                        + "platform = []")},
        };
    }

    @Test(description = "test an unknown table or key and a table written in the wrong form are Scan.toml errors",
            dataProvider = "invalid-scan-tomls")
    void testInvalidScanToml(String content, List<String> errors) throws IOException {
        Files.writeString(scanToml, content);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: Scan.toml has " + errors.size()
                + (errors.size() == 1 ? " error\n" : " errors\n") + String.join("\n", errors));
        Assert.assertFalse(run.output().contains("Running Scans"), run.output());
    }

    @Test(description = "test every error of a Scan.toml file is reported at once, in the order of the file")
    void testAllErrorsReportedAtOnce() throws IOException {
        Files.writeString(scanToml, """
                foo = 1

                [gates]
                failOnCount = 1

                [gate]
                failOnCount = 0
                """);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: Scan.toml has 3 errors
                Scan.toml:1:1: error: unknown key 'foo'; expected\s""" + TABLES + "\n"
                + "Scan.toml:3:1: error: unknown table 'gates'; expected " + TABLES + "\n"
                + "Scan.toml:7:15: error: invalid value '0' for 'failOnCount'; expected a whole number of 1 or more");
    }

    @Test(description = "test every documented table and key is accepted, with the extra keys of [[platform]]")
    void testValidScanToml() throws IOException {
        Files.writeString(scanToml, """
                [rule]
                include = ["ballerina:1", "exampleOrg/example_module_static_code_analyzer:1"]

                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"

                [[analyzer]]
                org = "ballerinax"
                name = "example_module_static_code_analyzer"
                version = "0.1.0"
                repository = "local"

                [[platform]]
                name = "examplePlatform"
                path = "https://example.com/example-platform-plugin.jar"
                sonarProjectPropertiesPath = "sonar-project.properties"
                token = 10

                [[platform]]
                name = "noPathPlatform"

                [suppression]
                enabled = true
                requireReason = false
                requireRuleId = false

                [gate]
                failOnSeverity = "NONE"
                failOnCount = 5

                [analysis]
                excludeTests = false
                """);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("error:"), run.output());
        Assert.assertFalse(run.output().contains("warning:"), run.output());

        // The entries of [[analyzer]] are not checked, so an entry that cannot be used is still left out
        Files.writeString(scanToml, """
                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"
                version = "latest"
                bogus = 1

                [[analyzer]]
                org = 10
                name = ""
                repository = "central"
                """);
        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("error:"), run.output());
        Assert.assertFalse(run.output().contains("warning:"), run.output());

        Files.writeString(scanToml, """
                analyzer = []
                platform = []
                """);
        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
    }

    @Test(description = "test --list-rules does not fail on an unknown table, which does not change the rules")
    void testListRulesWithUnknownTable() throws IOException {
        Files.writeString(scanToml, """
                [gates]
                failOnCount = 1
                """);
        ScanRun run = scan(project, "--list-rules");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("ballerina:1 "), run.output());
    }

    @Test(description = "test the editor loads a Scan.toml with an unknown table leniently")
    void testLenientLoadingWithUnknownTable() throws IOException {
        Files.writeString(scanToml, """
                [gates]
                failOnCount = 1

                [gate]
                failOnSeverity = "HIGH"
                """);
        Project loaded = ProjectLoader.load(project).project();
        Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(loaded, printStream);
        Assert.assertTrue(scanTomlFile.isPresent());
        Assert.assertEquals(scanTomlFile.get().getFailOnSeverity(), Optional.of(GateSeverity.HIGH));
        ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                () -> ScanUtils.readScanTomlConfigurations(loaded, printStream));
        Assert.assertEquals(exception.diagnostics(), List.of("Scan.toml:1:1: error: unknown table 'gates'; "
                + "expected " + TABLES));
    }

    @Test(description = "test a workspace Scan.toml rejects unknown tables and does not offer [[platform]]")
    void testWorkspaceUnknownTable() throws IOException {
        Files.writeString(workspaceScanToml, """
                [gates]
                failOnCount = 1
                """);
        ScanRun run = scan(workspace);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: Scan.toml has 1 error
                Scan.toml:1:1: error: unknown table 'gates'; expected [[analyzer]], [rule], [suppression], [gate] \
                or [analysis]""");
    }

    @Test(description = "test a workspace Scan.toml accepts platform = [], which declares no platform")
    void testWorkspaceEmptyPlatformList() throws IOException {
        Files.writeString(workspaceScanToml, """
                platform = []
                """);
        ScanRun run = scan(workspace);
        Assert.assertFalse(run.output().contains("Scan.toml has"), run.output());

        Files.writeString(workspaceScanToml, """
                platform = ["plugin.jar"]
                """);
        run = scan(workspace);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: Scan.toml has 1 error\nScan.toml:1:1: error: "
                + "[[platform]] cannot be set in a workspace Scan.toml"), run.output());
    }

    @Test(description = "test a platform named in --platforms whose [[platform]] has no path is reported as such")
    void testPlatformWithoutPath() throws IOException {
        Files.writeString(scanToml, """
                [[platform]]
                name = "examplePlatform"
                """);
        ScanRun run = scan(project, "--platforms=examplePlatform");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: platform 'examplePlatform' has no 'path' in the "
                + "[[platform]] table of Scan.toml; set it to the plugin JAR");

        run = scan(project, "--platforms=otherPlatform");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: platform 'otherPlatform' is not available; add its "
                + "plugin to the [[platform]] table of Scan.toml");

        // A platform without a path and one that is not declared are reported in the same run
        run = scan(project, "--platforms=examplePlatform,otherPlatform");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.output().endsWith("""
                bal scan: error: platform 'otherPlatform' is not available; add its plugin to the [[platform]] table \
                of Scan.toml
                bal scan: error: platform 'examplePlatform' has no 'path' in the [[platform]] table of Scan.toml; set \
                it to the plugin JAR
                """), run.output());

        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, "a platform without a path is ignored until it is named: "
                + run.output());
    }

    private ScanRun scan(Path target, String... args) throws IOException {
        System.setProperty("user.dir", target.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream);
            new CommandLine(scanCmd).parseArgs(args);
            scanCmd.execute();
            return new ScanRun(scanCmd.exitCode(), readOutput(true));
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private record ScanRun(int exitCode, String output) {

        // The error report is the last thing written, so it runs from the last error line to the end
        String errorReport() {
            int start = output.lastIndexOf("bal scan: error: ");
            return start < 0 ? "" : output.substring(start).trim();
        }
    }
}
