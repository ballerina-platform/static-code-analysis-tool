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
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.ReportFormat;
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
import java.util.List;
import java.util.Optional;

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

    @Test(description = "test the command-line gate options replace the whole [gate] table of Scan.toml")
    void testGatePrecedence() throws IOException {
        ScanRun run = scan(gateConfigProject, "--fail-on-severity=NONE");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=NONE overrides "
                + "[gate] failOnSeverity = \"HIGH\" in Scan.toml\n"), run.output());

        run = scan(gateConfigProject, "--fail-on-severity=high");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertFalse(run.output().contains("warning"), "the same gate gives no warning");

        // The severity in Scan.toml is not read, so the count covers every severity
        run = scan(gateConfigProject, "--fail-on-count=2");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=2 overrides "
                + "[gate] failOnSeverity = \"HIGH\" in Scan.toml\n"), run.output());
        Assert.assertTrue(run.errorReport().contains("at or above INFO, limit 2"), run.output());
    }

    @Test(description = "test a command-line count turns on a gate that Scan.toml turns off")
    void testCountOverridesNoneInScanToml() throws IOException {
        Path scanToml = gateConfigProject.resolve("Scan.toml");
        String original = Files.readString(scanToml, StandardCharsets.UTF_8);
        Files.writeString(scanToml, """
                [gate]
                failOnSeverity = "NONE"
                """);
        try {
            ScanRun run = scan(gateConfigProject, "--fail-on-count=1");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=1 overrides "
                    + "[gate] failOnSeverity = \"NONE\" in Scan.toml\n"), run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 6 issues at or above INFO, limit 1"),
                    run.output());
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

    @Test(description = "test the language server loads Scan.toml leniently, ignoring invalid gate settings")
    void testLenientScanTomlLoading() {
        Project project = ProjectLoader.load(invalidGateConfigProject).project();
        Optional<ScanTomlFile> scanToml = ScanUtils.loadScanTomlConfigurations(project, printStream);
        Assert.assertTrue(scanToml.isPresent());
        Assert.assertFalse(scanToml.get().hasGate());
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
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=3 overrides [gate] "
                + "failOnSeverity = \"HIGH\" in pkg-a/Scan.toml\n"), run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-count=3 overrides [gate] "
                + "failOnCount = 5 in pkg-b/Scan.toml\n"), run.output());

        // pkg-b does not keep failOnCount = 5 from its own Scan.toml, so both packages fail on their first issue
        run = scan(gatedWorkspace, "--fail-on-severity=LOW");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: 2 packages failed the gate
                pkg-a: error: 1 issue at or above LOW, limit 1 (HIGH: 1)
                pkg-b: error: 2 issues at or above LOW, limit 1 (LOW: 2)""");

        run = scan(gatedWorkspace, "--fail-on-severity=LOW", "--fail-on-count=1");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: 2 packages failed the gate
                pkg-a: error: 1 issue at or above LOW, limit 1 (HIGH: 1)
                pkg-b: error: 2 issues at or above LOW, limit 1 (LOW: 2)""");
    }

    @Test(description = "test the workspace [gate] table is the default of the packages, as a whole")
    void testWorkspaceGateDefault() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        try {
            writeWorkspaceScanToml("""
                    [gate]
                    failOnSeverity = "LOW"
                    """);
            Files.writeString(packageScanToml, "");
            ScanRun run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertEquals(run.errorReport(), """
                    bal scan: error: 2 packages failed the gate
                    pkg-a: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)
                    pkg-b: error: 2 issues at or above LOW, limit 1 (LOW: 2)""");

            run = scan(gatedWorkspace, "--fail-on-severity=BLOCKER");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=BLOCKER overrides "
                    + "[gate] failOnSeverity = \"LOW\" in Scan.toml\n"),
                    "an inherited gate names the workspace Scan.toml");

            // pkg-b sets only the count, so the workspace severity is not inherited and every severity counts
            Files.writeString(packageScanToml, """
                    [gate]
                    failOnCount = 2
                    """);
            run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().endsWith("pkg-b: error: 2 issues at or above INFO, limit 2 (LOW: 2)"),
                    run.output());

            run = scan(gatedWorkspace, "--fail-on-severity=BLOCKER", "--fail-on-count=3");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=BLOCKER --fail-on-count=3 "
                    + "overrides [gate] failOnCount = 2 in pkg-b/Scan.toml\n"), run.output());

            // An empty table turns off the inherited gate
            Files.writeString(packageScanToml, """
                    [gate]
                    """);
            run = scan(gatedWorkspace);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertFalse(run.errorReport().contains("pkg-b"), run.output());
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
                    + "[[analyzer]] tables, or as analyzer = [] to clear the workspace analyzers"), run.output());
        } finally {
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test the packages inherit the workspace [rule] table they do not set")
    void testWorkspaceRuleDefault() throws IOException {
        try {
            // pkg-a sets only [gate], so it inherits the exclusion of its HIGH issue
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
            run = scan(gatedWorkspace);
            Assert.assertTrue(run.output().contains("bal scan: warning: 'exampleOrg/example_module_static_code_"
                    + "analyzer:1' in [rule] of Scan.toml is not a rule of pkg-a\n"), run.output());
            Assert.assertTrue(run.output().contains("is not a rule of pkg-b\n"), run.output());

            // The command-line rules replace the inherited [rule] table, so pkg-a reports its HIGH issue again
            writeWorkspaceScanToml("""
                    [rule]
                    exclude = ["ballerina:13"]
                    """);
            run = scan(gatedWorkspace, "--fail-on-severity=HIGH", "--exclude-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("bal scan: warning: --exclude-rules=ballerina:1 overrides "
                    + "[rule] exclude = [\"ballerina:13\"] in Scan.toml\n"), run.output());
        } finally {
            Files.deleteIfExists(gatedWorkspace.resolve("Scan.toml"));
        }
    }

    @Test(description = "test a package [rule] table replaces the workspace one as a whole")
    void testPackageRuleTableReplacesWorkspaceTable() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-a").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        try {
            writeWorkspaceScanToml("""
                    [rule]
                    include = ["ballerina:13"]
                    """);
            ScanRun run = scan(gatedWorkspace, "--fail-on-severity=HIGH");
            Assert.assertEquals(run.exitCode(), 1, "pkg-a inherits the include of its HIGH issue: " + run.output());

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
            Assert.assertFalse(run.output().contains("Running Scans"), run.output());
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

    @Test(description = "test a package that fails to load in a workspace is reported instead of unknown rule IDs")
    void testPackageFailureWinsOverUnknownRules() throws IOException {
        Path packageScanToml = gatedWorkspace.resolve("pkg-b").resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        Files.writeString(packageScanToml, """
                [gate]
                failOnCount = 0
                """);
        try {
            ScanRun run = scan(gatedWorkspace, "--include-rules=ballerina:99");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 1 package failed\n"
                    + "pkg-b: error: Scan.toml has 1 error\n"), run.output());
            Assert.assertFalse(run.output().contains("unknown rule"), run.output());
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
