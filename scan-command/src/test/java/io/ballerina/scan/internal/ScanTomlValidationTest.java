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
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.ballerina.scan.TestConstants.EXAMPLE_ORG_ANALYZER;

/**
 * Tests for the validation of the tables, analyzers and rules of a Scan.toml file.
 *
 * @since 0.11.2
 */
public class ScanTomlValidationTest extends BaseTest {
    private static final String TABLES = "[[analyzer]], [[platform]], [rule], [suppression], [gate] or [analysis]";
    private static final String LIST_RULES_HINT = "  Run 'bal scan --list-rules' to see the available rules.";
    private static final String EXAMPLE_ORG_RULE = "exampleOrg/example_module_static_code_analyzer:1";
    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path project = testResourcesRoot.resolve("bal-project-with-scan-toml-errors");
    private final Path scanToml = project.resolve("Scan.toml");

    @AfterMethod(alwaysRun = true)
    void restoreFiles() throws IOException {
        Files.deleteIfExists(scanToml);
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
                        [[analyzer]]
                        org = 10
                        name = ""
                        bogus = 1
                        """, List.of(
                        "Scan.toml:4:1: error: unknown key 'bogus' in [[analyzer]]; expected org, name, version or "
                                + "repository",
                        "Scan.toml:2:7: error: invalid value '10' for 'org'; expected an organization name, such as "
                                + "\"ballerina\"",
                        "Scan.toml:3:8: error: invalid value '' for 'name'; expected a package name, such as "
                                + "\"example_analyzer\"")},
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        """, List.of("Scan.toml:1:1: error: 'name' is required in [[analyzer]]")},
                {"""
                        [[analyzer]]
                        """, List.of("Scan.toml:1:1: error: 'org' is required in [[analyzer]]",
                        "Scan.toml:1:1: error: 'name' is required in [[analyzer]]")},
                {"""
                        [[analyzer]]
                        org = "example org"
                        name = "example-analyzer"
                        """, List.of(
                        "Scan.toml:2:7: error: invalid value 'example org' for 'org'; expected an organization name, "
                                + "such as \"ballerina\"",
                        "Scan.toml:3:8: error: invalid value 'example-analyzer' for 'name'; expected a package name, "
                                + "such as \"example_analyzer\"")},
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer"
                        version = "latest"
                        """, List.of("Scan.toml:4:11: error: invalid value 'latest' for 'version'; expected a "
                        + "version, such as \"1.0.0\"")},
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer"
                        version = "0.1.0"
                        repository = "central"
                        """, List.of("Scan.toml:5:14: error: invalid value 'central' for 'repository'; expected "
                        + "\"local\"")},
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer"
                        repository = "local"
                        """, List.of("Scan.toml:1:1: error: 'version' is required in [[analyzer]] with "
                        + "repository = \"local\"")},
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
        };
    }

    @Test(description = "test an unknown table or key and an invalid [[analyzer]] are Scan.toml errors",
            dataProvider = "invalid-scan-tomls")
    void testInvalidScanToml(String content, List<String> errors) throws IOException {
        Files.writeString(scanToml, content);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: Scan.toml has " + errors.size()
                + (errors.size() == 1 ? " error\n" : " errors\n") + String.join("\n", errors));
        Assert.assertFalse(run.output().contains("Running Scans"), run.output());
    }

    @Test(description = "test every error of a Scan.toml file is reported at once")
    void testAllErrorsReportedAtOnce() throws IOException {
        Files.writeString(scanToml, """
                foo = 1

                [[analyzer]]
                name = "example_module_static_code_analyzer"

                [gates]
                failOnCount = 1

                [gate]
                failOnCount = 0
                """);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: Scan.toml has 4 errors
                Scan.toml:3:1: error: 'org' is required in [[analyzer]]
                Scan.toml:10:15: error: invalid value '0' for 'failOnCount'; expected a whole number of 1 or more
                Scan.toml:1:1: error: unknown key 'foo'; expected\s""" + TABLES + "\n"
                + "Scan.toml:6:1: error: unknown table 'gates'; expected " + TABLES);
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

        Files.writeString(scanToml, """
                analyzer = []
                platform = []
                """);
        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
    }

    @Test(description = "test --list-rules fails on an invalid [[analyzer]] but not on the errors of other tables")
    void testListRulesWithInvalidScanToml() throws IOException {
        Files.writeString(scanToml, """
                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"
                bogus = 1
                """);
        ScanRun run = scan(project, "--list-rules");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().endsWith("Scan.toml:4:1: error: unknown key 'bogus' in [[analyzer]]; "
                + "expected org, name, version or repository"), run.output());

        Files.writeString(scanToml, """
                [gates]
                failOnCount = 1

                [rule]
                include = ["ballerina:999"]
                """);
        run = scan(project, "--list-rules");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("ballerina:1 "), run.output());
    }

    @Test(description = "test the editor loads an invalid [[analyzer]] leniently, keeping the valid ones")
    void testLenientAnalyzerLoading() throws IOException {
        Files.writeString(scanToml, """
                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"
                version = "latest"

                [[analyzer]]
                org = "ballerinax"
                name = "example_module_static_code_analyzer"
                repository = "local"

                [[analyzer]]
                org = 10
                name = "name"

                [[analyzer]]
                org = "pre"
                name = "release"
                version = "1.0.0-alpha"

                [gates]
                failOnCount = 1
                """);
        Project loaded = ProjectLoader.loadProject(project);
        // The Scan.toml file of a package is found in the working directory
        System.setProperty("user.dir", project.toString());
        try {
            Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(loaded, printStream);
            Assert.assertTrue(scanTomlFile.isPresent());
            Assert.assertEquals(List.copyOf(scanTomlFile.get().getAnalyzers()), List.of(
                    new ScanTomlFile.Analyzer("exampleOrg", "example_module_static_code_analyzer", null, null),
                    new ScanTomlFile.Analyzer("ballerinax", "example_module_static_code_analyzer", null, null),
                    new ScanTomlFile.Analyzer("pre", "release", "1.0.0-alpha", null)));
            ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                    () -> ScanUtils.readScanTomlConfigurations(loaded, printStream));
            Assert.assertEquals(exception.diagnostics().size(), 4, exception.diagnostics().toString());
        } finally {
            System.setProperty("user.dir", userDir);
        }
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

        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, "a platform without a path is ignored until it is named: "
                + run.output());
    }

    @Test(description = "test a rule of [rule] in Scan.toml that the package does not provide fails the scan")
    void testUnknownRuleInScanToml() throws IOException {
        Files.writeString(scanToml, """
                [rule]
                exclude = ["ballerina:999"]
                """);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: Scan.toml has 1 error
                Scan.toml:2:12: error: unknown rule 'ballerina:999'; it is not a rule of the package
                """ + LIST_RULES_HINT);
        Assert.assertFalse(run.output().contains("Running Scans"), run.output());

        // An analyzer rule is known only from the analyzers the package engages
        Files.writeString(scanToml, """
                [rule]
                include = [
                    "ballerina:1",
                    "exampleOrg/example_module_static_code_analyzer:99",
                    "ballerina:99",
                    "exampleOrg/example_module_static_code_analyzer:1",
                    "ballerina:99",
                ]
                """ + EXAMPLE_ORG_ANALYZER);
        run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: Scan.toml has 2 errors
                Scan.toml:4:5: error: unknown rule 'exampleOrg/example_module_static_code_analyzer:99'; it is not \
                a rule of the package
                Scan.toml:5:5: error: unknown rule 'ballerina:99'; it is not a rule of the package
                """ + LIST_RULES_HINT);
    }

    @Test(description = "test the rules of [rule] that the package provides, from core rules and analyzers, are valid")
    void testKnownRulesInScanToml() throws IOException {
        Files.writeString(scanToml, """
                [rule]
                exclude = ["ballerina:1", "exampleOrg/example_module_static_code_analyzer:1"]
                """ + EXAMPLE_ORG_ANALYZER);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("error:"), run.output());

        // Without the analyzer, its rules cannot be checked, as a shared Scan.toml can name them for other packages
        Files.writeString(scanToml, """
                [rule]
                exclude = ["exampleOrg/example_module_static_code_analyzer:1", "ballerina/http:1"]
                """);
        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: '" + EXAMPLE_ORG_RULE + "' and "
                + "'ballerina/http:1' in [rule] of Scan.toml are not rules of the package\n"), run.output());
    }

    @DataProvider(name = "mistyped-rules")
    public Object[][] mistypedRules() {
        return new Object[][]{
                {"ballerina:999"},
                {"ballerina:0"},
                {"ballerina-1"},
                {"exampleOrg/example_module_static_code_analyzer"},
                {"exampleOrg/example_module_static_code_analyzer:99"},
        };
    }

    @Test(description = "test a mistyped rule fails the scan: a malformed ID, a core rule that does not exist, or a "
            + "rule that an analyzer the package uses does not define", dataProvider = "mistyped-rules")
    void testMistypedRuleInScanToml(String ruleId) throws IOException {
        Files.writeString(scanToml, """
                [rule]
                exclude = ["__rule__"]
                """.replace("__rule__", ruleId) + EXAMPLE_ORG_ANALYZER);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: Scan.toml has 1 error\nScan.toml:2:12: error: "
                + "unknown rule '" + ruleId + "'; it is not a rule of the package\n" + LIST_RULES_HINT);
    }

    @Test(description = "test a Scan.toml shared through a relative configPath names rules of analyzers that only "
            + "some packages use")
    void testSharedScanTomlByRelativePath() throws IOException {
        Path ballerinaToml = project.resolve("Ballerina.toml");
        String original = Files.readString(ballerinaToml, StandardCharsets.UTF_8);
        Path sharedScanToml = testResourcesRoot.resolve("shared-scan-config.toml");
        try {
            Files.writeString(ballerinaToml, original + """

                    [scan]
                    configPath = "../shared-scan-config.toml"
                    """);
            Files.writeString(sharedScanToml, """
                    [rule]
                    exclude = ["ballerina/http:1", "ballerinax/mysql:2", "ballerina:1"]
                    """);
            ScanRun run = scan(project);
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: 'ballerina/http:1' and 'ballerinax/mysql:2' "
                    + "in [rule] of "), run.output());
            Assert.assertTrue(run.output().contains("shared-scan-config.toml are not rules of the package\n"),
                    run.output());

            Files.writeString(sharedScanToml, """
                    [rule]
                    exclude = ["ballerina/http:1", "ballerina:999"]
                    """);
            run = scan(project);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().contains("shared-scan-config.toml:2:32: error: unknown rule "
                    + "'ballerina:999'; it is not a rule of the package\n" + LIST_RULES_HINT), run.output());
        } finally {
            Files.writeString(ballerinaToml, original);
            Files.deleteIfExists(sharedScanToml);
        }
    }

    @Test(description = "test a Scan.toml shared through a remote configPath names rules of analyzers that only some "
            + "packages use")
    void testSharedScanTomlByUrl() throws IOException {
        Path ballerinaToml = project.resolve("Ballerina.toml");
        String original = Files.readString(ballerinaToml, StandardCharsets.UTF_8);
        Path sharedScanToml = testResourcesRoot.resolve("shared-remote-scan-config.toml");
        String url = sharedScanToml.toAbsolutePath().toUri().toString();
        try {
            Files.writeString(ballerinaToml, original + """

                    [scan]
                    configPath = "__url__"
                    """.replace("__url__", url));
            Files.writeString(sharedScanToml, """
                    [rule]
                    exclude = ["ballerina/http:1", "exampleOrg/example_module_static_code_analyzer:99"]
                    """ + EXAMPLE_ORG_ANALYZER);
            ScanRun run = scan(project);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertEquals(run.errorReport(), "bal scan: error: " + url + " has 1 error\n" + url + ":2:32: "
                    + "error: unknown rule 'exampleOrg/example_module_static_code_analyzer:99'; it is not a rule of "
                    + "the package\n" + LIST_RULES_HINT);
            Assert.assertTrue(run.output().contains("bal scan: warning: 'ballerina/http:1' in [rule] of " + url
                    + " is not a rule of the package\n"), run.output());
        } finally {
            Files.writeString(ballerinaToml, original);
            Files.deleteIfExists(sharedScanToml);
            ProjectUtils.deleteDirectory(project.resolve("target"));
        }
    }

    @Test(description = "test an unknown rule on the command line is still a usage error, reported before Scan.toml")
    void testUnknownRuleOptionWithUnknownScanTomlRule() throws IOException {
        Files.writeString(scanToml, """
                [rule]
                exclude = ["ballerina:999"]
                """);
        ScanRun run = scan(project, "--exclude-rules=ballerina:998");
        Assert.assertEquals(run.exitCode(), 2, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: unknown rule 'ballerina:998'\n"),
                run.output());

        run = scan(project, "--exclude-rules=ballerina:1");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: Scan.toml has 1 error\n"), run.output());
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
