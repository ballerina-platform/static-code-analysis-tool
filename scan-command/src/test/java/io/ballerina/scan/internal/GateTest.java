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

import com.google.gson.JsonParser;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.ReportFormat;
import io.ballerina.scan.utils.GateSeverity;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.scan.TestConstants.EXAMPLE_ORG_ANALYZER;

/**
 * Tests for the exit codes, error reports and gate of the scan command.
 *
 * @since 0.13.0
 */
public class GateTest extends BaseTest {
    private static final String USAGE_HINT = """
            Usage: bal scan [OPTIONS] [<package>|<source-file>]
            Try 'bal scan --help' for more information.""";
    // bal-project-with-gate-issues has 1 HIGH, 2 MEDIUM and 3 LOW issues
    private static final String ALL_ISSUES = "HIGH: 1, MEDIUM: 2, LOW: 3";
    private static final int TOTAL_ISSUES = 6;

    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path gateIssuesProject = testResourcesRoot.resolve("bal-project-with-gate-issues");
    private final Path gateConfigProject = testResourcesRoot.resolve("bal-project-with-gate-configurations");
    private final Path invalidGateConfigProject = testResourcesRoot
            .resolve("bal-project-with-invalid-gate-configurations");
    private final Path gatedWorkspace = testResourcesRoot.resolve("workspace-with-gates");

    @Test(description = "test findings do not fail the scan when no gate is set")
    void testNoGate() throws IOException {
        ScanRun run = scan(gateIssuesProject);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("bal scan: error"), run.output());
    }

    @DataProvider(name = "gate-settings")
    public Object[][] gateSettings() {
        return new Object[][]{
                {new String[]{"--fail-on-severity=HIGH"}, "1 issue at or above HIGH, limit 1 (HIGH: 1)"},
                {new String[]{"--fail-on-severity=BLOCKER"}, null},
                {new String[]{"--fail-on-severity=medium", "--fail-on-count=3"},
                        "3 issues at or above MEDIUM, limit 3 (HIGH: 1, MEDIUM: 2)"},
                {new String[]{"--fail-on-severity=Medium", "--fail-on-count=4"}, null},
                {new String[]{"--fail-on-count=" + (TOTAL_ISSUES - 1)}, TOTAL_ISSUES + " issues at or above INFO, "
                        + "limit " + (TOTAL_ISSUES - 1) + " (" + ALL_ISSUES + ")"},
                {new String[]{"--fail-on-count=" + TOTAL_ISSUES}, TOTAL_ISSUES + " issues at or above INFO, limit "
                        + TOTAL_ISSUES + " (" + ALL_ISSUES + ")"},
                {new String[]{"--fail-on-count=" + (TOTAL_ISSUES + 1)}, null},
                {new String[]{"--fail-on-severity=NONE", "--fail-on-count=1"}, null},
                {new String[]{"--exclude-rules=ballerina:13", "--fail-on-severity=HIGH"}, null},
        };
    }

    @Test(description = "test the gate fails the scan when the issues at or above the severity reach the count",
            dataProvider = "gate-settings")
    void testGate(String[] args, String gateFailure) throws IOException {
        ScanRun run = scan(gateIssuesProject, args);
        if (gateFailure == null) {
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertEquals(run.errorReport(), "", run.output());
        } else {
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertEquals(run.errorReport(), "bal scan: error: " + gateFailure);
        }
    }

    @Test(description = "test a scan that fails the gate still reports every issue")
    void testGateKeepsReports() throws IOException {
        ScanRun run = scan(gateIssuesProject, "--fail-on-severity=HIGH");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.output().contains("View scan results at:"), run.output());
        String report = Files.readString(gateIssuesProject.resolve("target").resolve("report")
                .resolve("scan_results.json"), StandardCharsets.UTF_8);
        Assert.assertEquals(JsonParser.parseString(report).getAsJsonArray().size(), TOTAL_ISSUES);
    }

    @DataProvider(name = "usage-errors")
    public Object[][] usageErrors() {
        return new Object[][]{
                {new String[]{"--fail-on-severity=CRITICAL"}, "invalid value 'CRITICAL' for --fail-on-severity; "
                        + "expected BLOCKER, HIGH, MEDIUM, LOW, INFO or NONE"},
                {new String[]{"--fail-on-count=0"},
                        "invalid value '0' for --fail-on-count; expected a whole number of 1 or more"},
                {new String[]{"--fail-on-count=-1"},
                        "invalid value '-1' for --fail-on-count; expected a whole number of 1 or more"},
                {new String[]{"--fail-on-count=ten"},
                        "invalid value 'ten' for --fail-on-count; expected a whole number of 1 or more"},
                {new String[]{"--format=xml"}, "invalid value 'xml' for --format; expected json or sarif"},
                {new String[]{"--exclude-tests=yes"},
                        "invalid value 'yes' for --exclude-tests; expected true or false"},
                {new String[]{"--include-rules=ballerina:1", "--exclude-rules=ballerina:2"},
                        "cannot include and exclude rules at the same time"},
        };
    }

    @Test(description = "test invalid command-line input exits as a usage error before scanning",
            dataProvider = "usage-errors")
    void testUsageErrors(String[] args, String message) throws IOException {
        ScanRun run = scan(gateIssuesProject, args);
        Assert.assertEquals(run.exitCode(), 2, run.output());
        Assert.assertEquals(run.output().trim(), "bal scan: error: " + message + "\n" + USAGE_HINT);
    }

    @Test(description = "test an unknown rule ID is a usage error that points to the rule list")
    void testUnknownRuleIds() throws IOException {
        ScanRun run = scan(gateIssuesProject, "--include-rules=ballerina:99");
        Assert.assertEquals(run.exitCode(), 2, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: unknown rule 'ballerina:99'\n"
                + "  Run 'bal scan --list-rules' to see the available rules.\n" + USAGE_HINT);
        Assert.assertFalse(run.output().contains("Running Scans"), run.output());

        run = scan(gateIssuesProject, "--exclude-rules=ballerina:98, ballerina:1, ballerina:99");
        Assert.assertEquals(run.exitCode(), 2, run.output());
        Assert.assertTrue(run.errorReport().startsWith(
                "bal scan: error: unknown rules 'ballerina:98' and 'ballerina:99'\n"), run.output());
    }

    @Test(description = "test the gate set in Scan.toml")
    void testGateFromScanToml() throws IOException {
        ScanRun run = scan(gateConfigProject);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)");
        Assert.assertFalse(run.output().contains("warning"), run.output());
    }

    @Test(description = "test each command-line gate option replaces only its own field of [gate] in Scan.toml")
    void testGatePrecedence() throws IOException {
        ScanRun run = scan(gateConfigProject, "--fail-on-severity=NONE");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=NONE overrides "
                + "[gate] failOnSeverity = \"HIGH\" in Scan.toml\n"), run.output());

        run = scan(gateConfigProject, "--fail-on-severity=high");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertFalse(run.output().contains("warning"), "the same value gives no warning");

        // The severity in Scan.toml is kept, and the count is not written there, so there is no warning
        run = scan(gateConfigProject, "--fail-on-count=2");
        Assert.assertEquals(run.exitCode(), 0, "1 HIGH issue is below the count of 2: " + run.output());
        Assert.assertFalse(run.output().contains("warning"), run.output());

        run = scan(gateConfigProject, "--fail-on-severity=LOW", "--fail-on-count=2");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.output().split("bal scan: warning: ", -1).length - 1, 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 6 issues at or above LOW, limit 2"),
                run.output());
    }

    @Test(description = "test a command-line count warns that it does not turn on a gate that Scan.toml turns off")
    void testCountKeepsNoneInScanToml() throws IOException {
        Path scanToml = gateConfigProject.resolve("Scan.toml");
        String original = Files.readString(scanToml, StandardCharsets.UTF_8);
        Files.writeString(scanToml, """
                [gate]
                failOnSeverity = "NONE"
                """);
        try {
            ScanRun run = scan(gateConfigProject, "--fail-on-count=1");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=1 has no effect, as "
                    + "[gate] failOnSeverity = \"NONE\" in Scan.toml turns the gate off\n"), run.output());

            run = scan(gateConfigProject, "--fail-on-severity=INFO", "--fail-on-count=1");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=INFO overrides "
                    + "[gate] failOnSeverity = \"NONE\" in Scan.toml\n"), run.output());
        } finally {
            Files.writeString(scanToml, original);
        }
    }

    @Test(description = "test --list-rules ignores the rule filters")
    void testListRulesIgnoresRuleFilters() throws IOException {
        ScanRun run = scan(gateIssuesProject, "--list-rules", "--include-rules=ballerina:1",
                "--exclude-rules=unknown:1");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("ballerina:1"), run.output());
    }

    @Test(description = "test --list-rules ignores the Scan.toml settings and options that do not change the rules")
    void testListRulesIgnoresSettingsOutsideRules() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        Files.writeString(scanToml, """
                [rule]
                include = ["ballerina:1"]
                exclude = ["ballerina:2"]

                [gate]
                failOnSeverity = "CRITICAL"

                [suppression]
                enabled = "yes"

                [analysis]
                excludeTests = 1
                unknown = true
                """);
        try {
            ScanRun run = scan(gateIssuesProject, "--list-rules", "--fail-on-severity=CRITICAL",
                    "--fail-on-count=0", "--exclude-tests=maybe", "--format=xml");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("ballerina:1"), run.output());
            Assert.assertFalse(run.output().contains("error:"), run.output());
            Assert.assertFalse(run.output().contains("warning:"), run.output());

            run = scan(gateIssuesProject);
            Assert.assertEquals(run.exitCode(), 1, run.output());
        } finally {
            Files.deleteIfExists(scanToml);
        }
    }

    @Test(description = "test --list-rules fails on the Scan.toml errors that change the rules")
    void testListRulesFailsOnRuleChangingErrors() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        try {
            Files.writeString(scanToml, """
                    analyzer = "org/name"
                    """);
            ScanRun run = scan(gateIssuesProject, "--list-rules");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("Scan.toml:1:1: error:"), run.output());

            Files.writeString(scanToml, """
                    [rule
                    """);
            run = scan(gateIssuesProject, "--list-rules");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("bal scan: error: Scan.toml has"), run.output());
        } finally {
            Files.deleteIfExists(scanToml);
        }
    }

    @Test(description = "test the language server loads Scan.toml leniently, ignoring invalid gate settings")
    void testLenientScanTomlLoading() {
        Project project = ProjectLoader.load(invalidGateConfigProject).project();
        Optional<ScanTomlFile> scanToml = ScanUtils.loadScanTomlConfigurations(project, printStream);
        Assert.assertTrue(scanToml.isPresent());
        Assert.assertTrue(scanToml.get().getFailOnSeverity().isEmpty());
        Assert.assertTrue(scanToml.get().getFailOnCount().isEmpty());
        Assert.assertThrows(ScanToolException.class,
                () -> ScanUtils.readScanTomlConfigurations(project, printStream));
    }

    @Test(description = "test an empty workspace package is skipped with a warning")
    void testEmptyWorkspacePackageSkipped() throws IOException {
        Path workspaceToml = gatedWorkspace.resolve("Ballerina.toml");
        String original = Files.readString(workspaceToml, StandardCharsets.UTF_8);
        Path emptyPackage = gatedWorkspace.resolve("pkg-c");
        Files.createDirectories(emptyPackage);
        Files.writeString(emptyPackage.resolve("Ballerina.toml"), """
                [package]
                org = "ballerina"
                name = "pkg_c"
                version = "0.1.0"
                """);
        Files.writeString(workspaceToml, original.replace("\"pkg-b\"]", "\"pkg-b\", \"pkg-c\"]"));
        try {
            ScanRun run = scan(gatedWorkspace, "--fail-on-count=100");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: pkg-c has no sources to scan and is "
                    + "skipped\n"), run.output());
        } finally {
            Files.writeString(workspaceToml, original);
            ProjectUtils.deleteDirectory(emptyPackage);
        }
    }

    @Test(description = "test a platform-triggered scan reports to the caller's stream and throws the failure")
    void testPlatformTriggeredConstructor() throws IOException {
        ScanCmd scanCmd = new PlatformScanCmd(invalidGateConfigProject, printStream, ReportFormat.BALLERINA);
        ScanToolException exception = Assert.expectThrows(ScanToolException.class, scanCmd::execute);
        Assert.assertEquals(exception.getMessage(), "Scan.toml has 3 errors");
        Assert.assertEquals(exception.diagnostics().size(), 3);
        Assert.assertTrue(readOutput(true).contains("bal scan: error: Scan.toml has 3 errors\n"));

        new PlatformScanCmd(gateIssuesProject, printStream, ReportFormat.SARIF).execute();
        Assert.assertTrue(readOutput(true).contains("\"$schema\""), "the report format is applied");
    }

    @Test(description = "test [gate] and [rule] written in another form, or with invalid rule settings, are errors")
    void testInvalidGateAndRuleTables() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        try {
            Files.writeString(scanToml, """
                    [[gate]]
                    failOnSeverity = "HIGH"
                    """);
            ScanRun run = scan(gateIssuesProject);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().contains("Scan.toml:1:1: error: 'gate' must be a table, written as "
                    + "[gate]"), run.output());

            Files.writeString(scanToml, """
                    [rule]
                    includes = ["ballerina:1"]
                    exclude = "ballerina:1"
                    """);
            run = scan(gateIssuesProject);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            String report = run.errorReport();
            Assert.assertTrue(report.startsWith("bal scan: error: Scan.toml has 2 errors\n"), report);
            Assert.assertTrue(report.contains("Scan.toml:2:1: error: unknown key 'includes' in [rule]; expected "
                    + "include or exclude"), report);
            Assert.assertTrue(report.contains("Scan.toml:3:11: error: invalid value 'ballerina:1' for 'exclude'; "
                    + "expected an array of rule IDs, such as [\"ballerina:1\"]"), report);
        } finally {
            Files.deleteIfExists(scanToml);
        }
    }

    @Test(description = "test the test-runner constructor ignores the [gate] table of the fixture")
    void testGateOffForTestRunnerConstructor() throws IOException {
        new PlatformScanCmd(gateConfigProject, printStream, ReportFormat.BALLERINA).execute();
        Assert.assertFalse(readOutput(true).contains("bal scan: error:"));
    }

    @Test(description = "test the test-runner constructor defaults to the JSON format when none is given")
    void testTestRunnerConstructorWithoutFormat() throws IOException {
        new PlatformScanCmd(gateIssuesProject, printStream, null).execute();
        String output = readOutput(true);
        Assert.assertFalse(output.contains("bal scan: error:"), output);
        Assert.assertFalse(output.contains("\"$schema\""), output);
    }

    @Test(description = "test a target that is not a valid path exits as an invalid target")
    void testInvalidTargetPath() throws IOException {
        ScanRun run = scan(gateIssuesProject, "invalid\u0000path");
        Assert.assertEquals(run.exitCode(), 3, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: not a Ballerina package, workspace or "
                + "source file: "), run.output());
    }

    @Test(description = "test a package that cannot be loaded exits as a runtime error, not an invalid target")
    void testProjectLoadFailure() throws IOException {
        Path brokenPackage = Files.createTempDirectory("broken-package");
        try {
            Files.writeString(brokenPackage.resolve("Ballerina.toml"), """
                    [package]
                    org = "ballerina"
                    name = "broken
                    """);
            Files.writeString(brokenPackage.resolve("main.bal"), "public function main() {}\n");
            ScanRun run = scan(brokenPackage);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("bal scan: error: failed to load the project at "),
                    run.output());
        } finally {
            ProjectUtils.deleteDirectory(brokenPackage);
        }
    }

    @Test(description = "test invalid gate settings in Scan.toml are reported at their location")
    void testInvalidGateInScanToml() throws IOException {
        ScanRun run = scan(invalidGateConfigProject, "--fail-on-severity=NONE");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        String report = run.errorReport();
        Assert.assertTrue(report.startsWith("bal scan: error: Scan.toml has 3 errors\n"), report);
        Assert.assertTrue(report.contains("Scan.toml:2:18: error: invalid value 'CRITICAL' for 'failOnSeverity'; "
                + "expected BLOCKER, HIGH, MEDIUM, LOW, INFO or NONE"), report);
        Assert.assertTrue(report.contains("Scan.toml:3:15: error: invalid value '0' for 'failOnCount'; expected a "
                + "whole number of 1 or more"), report);
        Assert.assertTrue(report.contains("Scan.toml:4:1: error: unknown key 'failOnSevrity' in [gate]; expected "
                + "failOnSeverity or failOnCount"), report);
        Assert.assertFalse(run.output().contains("Running Scans"), "a configuration error stops the scan");
    }

    @Test(description = "test suppressed issues count toward the gate only when suppressions are disregarded")
    void testGateWithSuppressions() throws IOException {
        Path suppressionsProject = testResourcesRoot.resolve("bal-project-with-suppressions");
        ScanRun run = scan(suppressionsProject, "--fail-on-count=2");
        Assert.assertEquals(run.exitCode(), 0, run.output());

        run = scan(suppressionsProject, "--ignore-suppressions", "--fail-on-count=3");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 3 issues at or above INFO, limit 3"),
                run.output());
    }

    @Test(description = "test an error is reported instead of a tripped gate, whatever the gate is set to")
    void testErrorsWinOverGate() throws IOException {
        ScanRun run = scan(testResourcesRoot.resolve("bal-project-with-compilation-errors"),
                "--fail-on-severity=NONE");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: compilation contains errors"),
                run.output());

        // The project has one active issue, so the gate is tripped as well
        run = scan(testResourcesRoot.resolve("bal-project-with-suppressions"), "--require-suppression-reason",
                "--fail-on-count=1");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 1 invalid suppression\n"), run.output());
        Assert.assertFalse(run.output().contains("at or above"), run.output());
    }

    @Test(description = "test each workspace package is checked against the gate in its own Scan.toml")
    void testPackageGatesInWorkspace() throws IOException {
        ScanRun run = scan(gatedWorkspace);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: 1 package failed the gate
                pkg-a: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)""");
        Assert.assertTrue(Files.exists(gatedWorkspace.resolve("target").resolve("report")
                .resolve("scan_results.json")), "the workspace report is written before the gate is evaluated");
    }

    @Test(description = "test a command-line gate checks each workspace package with its own issues")
    void testCommandLineGateInWorkspace() throws IOException {
        // pkg-a has 1 HIGH issue and pkg-b has 2 LOW issues, so no package reaches a count of 3 on its own
        ScanRun run = scan(gatedWorkspace, "--fail-on-count=3");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("in pkg-a/Scan.toml"), "pkg-a does not write failOnCount: "
                + run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=3 overrides [gate] "
                + "failOnCount = 5 in pkg-b/Scan.toml\n"), run.output());

        // pkg-b keeps failOnCount = 5 from its own Scan.toml, so only pkg-a fails
        run = scan(gatedWorkspace, "--fail-on-severity=LOW");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=LOW overrides [gate] "
                + "failOnSeverity = \"HIGH\" in pkg-a/Scan.toml\n"), run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: 1 package failed the gate
                pkg-a: error: 1 issue at or above LOW, limit 1 (HIGH: 1)""");

        run = scan(gatedWorkspace, "--fail-on-severity=LOW", "--fail-on-count=1");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: 2 packages failed the gate
                pkg-a: error: 1 issue at or above LOW, limit 1 (HIGH: 1)
                pkg-b: error: 2 issues at or above LOW, limit 1 (LOW: 2)""");
    }

    @Test(description = "test a package uses its own Scan.toml as a whole, or the workspace one when it has none")
    void testWorkspaceScanTomlIsUsedAsAWhole() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        String notApplied = "bal scan: warning: pkg-b/Scan.toml overrides Scan.toml for pkg-b; not applied: "
                + "[gate] failOnSeverity = \"LOW\"\n";
        try {
            writeWorkspaceScanToml("""
                    [gate]
                    failOnSeverity = "LOW"
                    """);
            // pkg-b has no Scan.toml, so it uses the workspace one; pkg-a uses its own
            Files.delete(packageScanToml);
            ScanRun run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertEquals(run.errorReport(), """
                    bal scan: error: 2 packages failed the gate
                    pkg-a: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)
                    pkg-b: error: 2 issues at or above LOW, limit 1 (LOW: 2)""");
            Assert.assertFalse(run.output().contains("overrides Scan.toml for pkg-a"),
                    "pkg-a sets failOnSeverity itself: " + run.output());
            // Progress lines name the package each file and scan is for
            for (String line : List.of("Loading scan tool configurations for the workspace from Scan.toml\n",
                    "Loading scan tool configurations for pkg-a from pkg-a/Scan.toml\n",
                    "Using the workspace scan tool configurations for pkg-b\n", "Running scans on pkg-a\n",
                    "Running scans on pkg-b\n", "\nCombining the results of 2 packages\n")) {
                Assert.assertTrue(run.output().contains(line), line + " in " + run.output());
            }
            Assert.assertFalse(run.output().contains("Loading scan tool configurations from"), run.output());
            Assert.assertTrue(run.output().contains("\nRunning Scans\nRunning scans on pkg-a\n"), run.output());

            run = scan(gatedWorkspace, "--fail-on-severity=BLOCKER");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=BLOCKER overrides "
                    + "[gate] failOnSeverity = \"LOW\" in Scan.toml\n"), "names the workspace Scan.toml");
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=BLOCKER overrides "
                    + "[gate] failOnSeverity = \"HIGH\" in pkg-a/Scan.toml\n"), run.output());

            // pkg-b's own file sets only the count, so the workspace severity does not apply
            Files.writeString(packageScanToml, """
                    [gate]
                    failOnCount = 2
                    """);
            run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains(notApplied), run.output());
            Assert.assertTrue(run.errorReport().endsWith("pkg-b: error: 2 issues at or above INFO, limit 2 (LOW: 2)"),
                    run.output());

            run = scan(gatedWorkspace, "--fail-on-severity=BLOCKER", "--fail-on-count=3");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=3 overrides [gate] "
                    + "failOnCount = 2 in pkg-b/Scan.toml\n"), run.output());
            Assert.assertFalse(run.output().contains("--fail-on-severity=BLOCKER overrides [gate] failOnSeverity "
                    + "= \"LOW\""), "the workspace file is not used by any package: " + run.output());

            // An empty file of its own turns the gate off for pkg-b
            Files.writeString(packageScanToml, "");
            run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains(notApplied), run.output());
            Assert.assertFalse(run.errorReport().contains("pkg-b"), run.output());

            // Repeating every workspace setting gives no warning
            Files.writeString(packageScanToml, """
                    [gate]
                    failOnSeverity = "LOW"
                    failOnCount = 5
                    """);
            run = scan(gatedWorkspace);
            Assert.assertFalse(run.output().contains("overrides Scan.toml for pkg-b"), run.output());
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test a workspace package uses the workspace Scan.toml when it has none, even when it is "
            + "loaded without its workspace")
    void testWorkspacePackageScannedOnItsOwn() throws IOException {
        Path packageRoot = gatedWorkspace.resolve("pkg-b");
        Path packageScanToml = packageRoot.resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        try {
            writeWorkspaceScanToml("""
                    [gate]
                    failOnSeverity = "LOW"
                    """);
            Files.delete(packageScanToml);
            // Scanning from inside a package loads its whole workspace
            ScanRun run = scan(packageRoot);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().endsWith("pkg-b: error: 2 issues at or above LOW, limit 1 (LOW: 2)"),
                    run.output());

            // A package loaded on its own finds the workspace file from its root
            Project packageOnly = BuildProject.load(packageRoot);
            Assert.assertTrue(packageOnly.workspaceProject().isEmpty(), "the package is loaded without the workspace");
            Optional<ScanTomlFile> scanToml = ScanUtils.readEnclosingWorkspaceScanToml(packageOnly, printStream);
            Assert.assertTrue(scanToml.isPresent());
            Assert.assertEquals(scanToml.get().getFailOnSeverity(), Optional.of(GateSeverity.LOW));

            Files.writeString(packageScanToml, original);
            Assert.assertTrue(ScanUtils.readScanTomlConfigurations(packageOnly, printStream).getPath().isPresent(),
                    "a Scan.toml of its own is used instead");
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test an analyzer entry that is neither [[analyzer]] nor analyzer = [] is an error")
    void testInvalidAnalyzerList() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        try {
            Files.writeString(packageScanToml, """
                    analyzer = "exampleOrg/example_analyzer"
                    """);
            ScanRun run = scan(gatedWorkspace);
            Assert.assertNotEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("Scan.toml:1:1: error: 'analyzer' must be written as "
                    + "[[analyzer]] tables, or as analyzer = []\n"), run.output());
        } finally {
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test a package without a Scan.toml uses the workspace [rule] table")
    void testWorkspaceRuleDefault() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-a").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        Path otherScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String otherOriginal = Files.readString(otherScanToml, StandardCharsets.UTF_8);
        try {
            // pkg-a has no Scan.toml, so it uses the workspace exclusion of its HIGH issue
            Files.delete(packageScanToml);
            writeWorkspaceScanToml("""
                    [rule]
                    exclude = ["ballerina:13"]
                    """);
            ScanRun run = scan(gatedWorkspace, "--fail-on-severity=HIGH");
            Assert.assertEquals(run.exitCode(), 0, run.output());

            writeWorkspaceScanToml("""
                    [rule]
                    exclude = ["exampleOrg/example_module_static_code_analyzer:1"]
                    """);
            // pkg-b provides the rule through an analyzer of its own, so the workspace entry is valid
            Files.writeString(otherScanToml, otherOriginal + EXAMPLE_ORG_ANALYZER);
            run = scan(gatedWorkspace);
            Assert.assertTrue(run.output().contains("bal scan: warning: 'exampleOrg/example_module_static_code_"
                    + "analyzer:1' in [rule] of Scan.toml is not a rule of pkg-a\n"), run.output());
            Assert.assertFalse(run.output().contains("is not a rule of pkg-b"), "pkg-b uses its own file: "
                    + run.output());

            // --exclude-rules adds to the exclude field, so pkg-a still leaves out its HIGH issue
            writeWorkspaceScanToml("""
                    [rule]
                    exclude = ["ballerina:13"]
                    """);
            run = scan(gatedWorkspace, "--fail-on-severity=HIGH", "--exclude-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertFalse(run.output().contains("overrides [rule]"), run.output());
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
            Files.writeString(packageScanToml, original);
            Files.writeString(otherScanToml, otherOriginal);
        }
    }

    @Test(description = "test a rule option that leaves both include and exclude set is a usage error")
    void testRuleOptionConflictsWithScanToml() throws IOException {
        Path scanToml = gateConfigProject.resolve("Scan.toml");
        String original = Files.readString(scanToml, StandardCharsets.UTF_8);
        try {
            Files.writeString(scanToml, original + """

                    [rule]
                    include = ["ballerina:13"]
                    """);
            ScanRun run = scan(gateConfigProject, "--exclude-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 2, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: --exclude-rules=ballerina:1 cannot be "
                    + "combined with [rule] include = [\"ballerina:13\"] in Scan.toml"), run.output());
            Assert.assertFalse(run.output().contains("Running Scans"), run.output());

            // Adding to the same field is not a conflict, and the rule of Scan.toml is still included
            run = scan(gateConfigProject, "--include-rules=ballerina:1", "--fail-on-severity=NONE");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertFalse(run.output().contains("overrides [rule]"), run.output());
            Assert.assertEquals(reportedRuleIds(gateConfigProject), Set.of("ballerina:1", "ballerina:13"),
                    run.output());

            Files.writeString(scanToml, original + """

                    [rule]
                    exclude = ["ballerina:13"]
                    """);
            run = scan(gateConfigProject, "--include-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 2, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: --include-rules=ballerina:1 cannot be "
                    + "combined with [rule] exclude = [\"ballerina:13\"] in Scan.toml"), run.output());
            Assert.assertFalse(run.output().contains("Running Scans"), run.output());
        } finally {
            Files.writeString(scanToml, original);
        }
    }

    @Test(description = "test a workspace [rule] entry a package lacks is reported even with a rule option")
    void testUnavailableWorkspaceRuleWithRuleOption() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-a").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        Path otherScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String otherOriginal = Files.readString(otherScanToml, StandardCharsets.UTF_8);
        try {
            Files.delete(packageScanToml);
            Files.writeString(otherScanToml, otherOriginal + EXAMPLE_ORG_ANALYZER);
            writeWorkspaceScanToml("""
                    [rule]
                    exclude = ["exampleOrg/example_module_static_code_analyzer:1"]
                    """);
            // The option adds to the workspace exclude, so the entry pkg-a lacks still applies to it
            ScanRun run = scan(gatedWorkspace, "--exclude-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: 'exampleOrg/example_module_static_code_"
                    + "analyzer:1' in [rule] of Scan.toml is not a rule of pkg-a\n"), run.output());
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
            Files.writeString(packageScanToml, original);
            Files.writeString(otherScanToml, otherOriginal);
        }
    }

    private static Set<String> reportedRuleIds(Path project) throws IOException {
        String report = Files.readString(project.resolve("target").resolve("report").resolve("scan_results.json"),
                StandardCharsets.UTF_8);
        Set<String> ruleIds = new HashSet<>();
        JsonParser.parseString(report).getAsJsonArray().forEach(issue ->
                ruleIds.add(issue.getAsJsonObject().getAsJsonObject("rule").get("id").getAsString()));
        return ruleIds;
    }

    @Test(description = "test a package Scan.toml is used instead of the workspace one, [rule] included")
    void testPackageRuleTableReplacesWorkspaceTable() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-a").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        try {
            writeWorkspaceScanToml("""
                    [rule]
                    include = ["ballerina:13"]
                    """);
            ScanRun run = scan(gatedWorkspace, "--fail-on-severity=HIGH");
            Assert.assertEquals(run.exitCode(), 1, "pkg-a uses its own file, which selects every rule: "
                    + run.output());
            Assert.assertTrue(run.output().contains("not applied: [rule] include = [\"ballerina:13\"]"),
                    run.output());

            Files.writeString(packageScanToml, original + """

                    [rule]
                    exclude = ["ballerina:13"]
                    """);
            run = scan(gatedWorkspace, "--fail-on-severity=HIGH");
            Assert.assertEquals(run.exitCode(), 0, "the package exclude replaces the workspace include: "
                    + run.output());

            // An empty table clears the inherited include, so every rule runs
            Files.writeString(packageScanToml, original + """

                    [rule]
                    """);
            run = scan(gatedWorkspace, "--fail-on-severity=HIGH");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().endsWith("pkg-a: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)"),
                    run.output());

            // Both lists in one table are an error at the location of the table
            Files.writeString(packageScanToml, original + """

                    [rule]
                    include = ["ballerina:13"]
                    exclude = ["ballerina:1"]
                    """);
            run = scan(gatedWorkspace, "--fail-on-severity=HIGH");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("Scan.toml:4:1: error: cannot include and exclude rules at the "
                    + "same time"), run.output());
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test a workspace Scan.toml cannot set platforms")
    void testWorkspacePlatformNotAllowed() throws IOException {
        try {
            writeWorkspaceScanToml("""
                    [[platform]]
                    name = "sonarqube"
                    path = "sonar-platform-plugin.jar"
                    """);
            ScanRun run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: Scan.toml has 1 error\nScan.toml:1:"),
                    run.output());
            Assert.assertTrue(run.errorReport().endsWith("error: [[platform]] cannot be set in a workspace "
                    + "Scan.toml; set it in the Scan.toml of each package"), run.output());
            Assert.assertFalse(run.output().contains("Running scans on"), run.output());
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
        }
    }

    private void writeWorkspaceScanToml(String content) throws IOException {
        Files.writeString(gatedWorkspace.resolve("Scan.toml"), content);
    }

    @Test(description = "test the gate is off when an analysis platform triggers the scan")
    void testGateOffWhenPlatformTriggered() throws IOException {
        ScanRun run = scan(gateConfigProject, "--platform-triggered");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertEquals(run.errorReport(), "", run.output());

        run = scan(gatedWorkspace, "--platform-triggered", "--fail-on-severity=LOW");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("warning"), run.output());
    }

    @Test(description = "test a package that fails to load in a workspace stops the scan before any is scanned")
    void testPackageFailureInWorkspace() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        Files.writeString(packageScanToml, """
                [gate]
                failOnCount = 0
                """);
        try {
            // The core rules are known without the failed package, so an unknown one is still a usage error
            ScanRun run = scan(gatedWorkspace, "--include-rules=ballerina:99");
            Assert.assertEquals(run.exitCode(), 2, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: unknown rule 'ballerina:99'\n"),
                    run.output());
            Assert.assertFalse(run.output().contains("Running scans on"), run.output());

            // An analyzer rule could belong to the failed package, so the package failure is reported instead
            run = scan(gatedWorkspace, "--include-rules=exampleOrg/example_module_static_code_analyzer:1");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 1 package failed\n"
                    + "pkg-b: error: Scan.toml has 1 error\n"), run.output());
            Assert.assertFalse(run.output().contains("unknown rule"), run.output());
            Assert.assertFalse(run.output().contains("Running scans on"), run.output());
        } finally {
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test errors and warnings are written to standard error, and results to standard output")
    void testOutputStreams() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setProperty("user.dir", gateConfigProject.toString());
        try (PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
             PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            ScanCmd scanCmd = new ScanCmd(outStream, errStream);
            new CommandLine(scanCmd).parseArgs("--fail-on-severity=LOW");
            scanCmd.execute();
            Assert.assertEquals(scanCmd.exitCode(), 1);
        } finally {
            System.setProperty("user.dir", userDir);
        }
        String output = out.toString(StandardCharsets.UTF_8);
        String error = err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        Assert.assertEquals(JsonParser.parseString(output).getAsJsonArray().size(), TOTAL_ISSUES, output);
        Assert.assertTrue(error.contains("bal scan: warning: --fail-on-severity=LOW overrides "), error);
        Assert.assertTrue(error.contains("View scan results at:"), error);
        Assert.assertTrue(error.trim().endsWith("issues at or above LOW, limit 1 (HIGH: 1, MEDIUM: 2, LOW: 3)"),
                error);
    }

    private ScanRun scan(Path project, String... args) throws IOException {
        System.setProperty("user.dir", project.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream);
            new CommandLine(scanCmd).parseArgs(args);
            scanCmd.execute();
            return new ScanRun(scanCmd.exitCode(), readOutput(true));
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private static class PlatformScanCmd extends ScanCmd {
        PlatformScanCmd(Path projectPath, PrintStream outputStream, ReportFormat format) {
            super(projectPath, outputStream, false, false, null, false, format, false, List.of(), List.of(),
                    List.of());
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
