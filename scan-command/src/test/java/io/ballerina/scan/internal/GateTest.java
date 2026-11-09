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
import io.ballerina.projects.directory.BuildProject;
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

/**
 * Tests for the exit codes, error reports and gate of the scan command.
 *
 * @since 0.11.1
 */
public class GateTest extends BaseTest {
    private static final String USAGE_HINT = """
            Usage: bal scan [OPTIONS] [<package>|<source-file>]
            Try 'bal scan --help' for more information.""";
    // Every core rule is a code smell, so bal-project-with-gate-issues has LOW issues only
    private static final int TOTAL_ISSUES = 3;
    private static final String ALL_ISSUES = "LOW: " + TOTAL_ISSUES;

    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path gateIssuesProject = testResourcesRoot.resolve("bal-project-with-gate-issues");
    private final Path gateConfigProject = testResourcesRoot.resolve("bal-project-with-gate-configurations");
    private final Path invalidGateConfigProject = testResourcesRoot
            .resolve("bal-project-with-invalid-gate-configurations");

    @Test(description = "test findings do not fail the scan when no gate is set")
    void testNoGate() throws IOException {
        ScanRun run = scan(gateIssuesProject);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains(TOTAL_ISSUES + " issue(s)"), run.output());
        Assert.assertFalse(run.output().contains("bal scan: error"), run.output());
    }

    @DataProvider(name = "gate-settings")
    public Object[][] gateSettings() {
        return new Object[][]{
                {new String[]{"--fail-on-severity=LOW"}, TOTAL_ISSUES + " issues at or above LOW, limit 1 ("
                        + ALL_ISSUES + ")"},
                {new String[]{"--fail-on-severity=MEDIUM"}, null},
                {new String[]{"--fail-on-severity=low", "--fail-on-count=" + TOTAL_ISSUES},
                        TOTAL_ISSUES + " issues at or above LOW, limit " + TOTAL_ISSUES + " (" + ALL_ISSUES + ")"},
                {new String[]{"--fail-on-severity=Low", "--fail-on-count=" + (TOTAL_ISSUES + 1)}, null},
                {new String[]{"--fail-on-count=" + (TOTAL_ISSUES - 1)}, TOTAL_ISSUES + " issues at or above INFO, "
                        + "limit " + (TOTAL_ISSUES - 1) + " (" + ALL_ISSUES + ")"},
                {new String[]{"--fail-on-count=" + TOTAL_ISSUES}, TOTAL_ISSUES + " issues at or above INFO, limit "
                        + TOTAL_ISSUES + " (" + ALL_ISSUES + ")"},
                {new String[]{"--fail-on-count=" + (TOTAL_ISSUES + 1)}, null},
                {new String[]{"--fail-on-severity=NONE", "--fail-on-count=1"}, null},
                {new String[]{"--exclude-rules=ballerina:1", "--fail-on-severity=LOW"}, null},
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
        ScanRun run = scan(gateIssuesProject, "--fail-on-severity=LOW");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.output().contains("View scan results at:"), run.output());
        Assert.assertTrue(run.output().indexOf(TOTAL_ISSUES + " issue(s)") < run.output().indexOf("bal scan: error"),
                "the summary is printed before the gate fails: " + run.output());
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
        Assert.assertEquals(run.errorReport(), "bal scan: error: " + TOTAL_ISSUES + " issues at or above LOW, "
                + "limit 1 (" + ALL_ISSUES + ")");
        Assert.assertFalse(run.output().contains("warning"), run.output());
    }

    @Test(description = "test each command-line gate option replaces only its own field of [gate] in Scan.toml")
    void testGatePrecedence() throws IOException {
        ScanRun run = scan(gateConfigProject, "--fail-on-severity=NONE");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("bal scan: warning: --fail-on-severity=NONE overrides "
                + "[gate] failOnSeverity = \"LOW\" in Scan.toml\n"), run.output());

        run = scan(gateConfigProject, "--fail-on-severity=low");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertFalse(run.output().contains("warning"), "the same value gives no warning");

        // The severity in Scan.toml is kept, and the count is not written there, so there is no warning
        run = scan(gateConfigProject, "--fail-on-count=" + (TOTAL_ISSUES + 1));
        Assert.assertEquals(run.exitCode(), 0, "the issues are below the count: " + run.output());
        Assert.assertFalse(run.output().contains("warning"), run.output());

        run = scan(gateConfigProject, "--fail-on-severity=MEDIUM", "--fail-on-count=2");
        Assert.assertEquals(run.exitCode(), 0, "no issue is at or above MEDIUM: " + run.output());
        Assert.assertEquals(run.output().split("bal scan: warning: ", -1).length - 1, 1, run.output());
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
            Assert.assertTrue(run.output().contains("Scan.toml:1:1: error: 'analyzer' must be written as "
                    + "[[analyzer]] tables, or as analyzer = []\n"), run.output());

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

    @Test(description = "test a rule option that leaves both include and exclude set is a usage error")
    void testRuleOptionConflictsWithScanToml() throws IOException {
        Path scanToml = gateConfigProject.resolve("Scan.toml");
        String original = Files.readString(scanToml, StandardCharsets.UTF_8);
        try {
            Files.writeString(scanToml, original + """

                    [rule]
                    include = ["ballerina:2"]
                    """);
            ScanRun run = scan(gateConfigProject, "--exclude-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 2, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: --exclude-rules=ballerina:1 cannot be "
                    + "combined with [rule] include = [\"ballerina:2\"] in Scan.toml"), run.output());
            Assert.assertFalse(run.output().contains("Running Scans"), run.output());

            // Adding to the same field is not a conflict, and the rule of Scan.toml is still included
            run = scan(gateConfigProject, "--include-rules=ballerina:1", "--fail-on-severity=NONE");
            Assert.assertEquals(run.exitCode(), 0, run.output());
            Assert.assertFalse(run.output().contains("overrides [rule]"), run.output());
            Assert.assertEquals(reportedRuleIds(gateConfigProject), Set.of("ballerina:1"), run.output());

            Files.writeString(scanToml, original + """

                    [rule]
                    exclude = ["ballerina:2"]
                    """);
            run = scan(gateConfigProject, "--include-rules=ballerina:1");
            Assert.assertEquals(run.exitCode(), 2, run.output());
            Assert.assertTrue(run.errorReport().startsWith("bal scan: error: --include-rules=ballerina:1 cannot be "
                    + "combined with [rule] exclude = [\"ballerina:2\"] in Scan.toml"), run.output());
            Assert.assertFalse(run.output().contains("Running Scans"), run.output());
        } finally {
            Files.writeString(scanToml, original);
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

    @Test(description = "test an analyzer entry that is neither [[analyzer]] nor analyzer = [] is an error")
    void testInvalidAnalyzerList() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        try {
            Files.writeString(scanToml, """
                    analyzer = "exampleOrg/example_analyzer"
                    """);
            ScanRun run = scan(gateIssuesProject);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().contains("Scan.toml:1:1: error: 'analyzer' must be written as "
                    + "[[analyzer]] tables, or as analyzer = []\n"), run.output());

            Files.writeString(scanToml, """
                    analyzer = []
                    """);
            run = scan(gateIssuesProject);
            Assert.assertEquals(run.exitCode(), 0, run.output());
        } finally {
            Files.deleteIfExists(scanToml);
        }
    }

    @Test(description = "test a target that is not a valid path exits as an invalid target")
    void testInvalidTargetPath() throws IOException {
        ScanRun run = scan(gateIssuesProject, "invalid\u0000path");
        Assert.assertEquals(run.exitCode(), 3, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: not a Ballerina package or source file: "),
                run.output());
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
            Assert.assertTrue(run.output().contains("bal scan: error: "), run.output());
        } finally {
            ProjectUtils.deleteDirectory(brokenPackage);
        }
    }

    @Test(description = "test the language server style loader ignores invalid settings, unlike bal scan")
    void testScanTomlLoaders() throws IOException {
        System.setProperty("user.dir", invalidGateConfigProject.toString());
        try {
            BuildProject project = BuildProject.load(invalidGateConfigProject.toAbsolutePath());
            Optional<ScanTomlFile> scanToml = ScanUtils.loadScanTomlConfigurations(project, printStream);
            Assert.assertTrue(scanToml.isPresent());
            Assert.assertTrue(scanToml.get().getFailOnSeverity().isEmpty());
            Assert.assertTrue(scanToml.get().getFailOnCount().isEmpty());
            ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                    () -> ScanUtils.readScanTomlConfigurations(project, printStream));
            Assert.assertEquals(exception.diagnostics().size(), 3);

            BuildProject gatedProject = BuildProject.load(gateConfigProject.toAbsolutePath());
            System.setProperty("user.dir", gateConfigProject.toString());
            ScanTomlFile gatedToml = ScanUtils.readScanTomlConfigurations(gatedProject, printStream);
            Assert.assertEquals(gatedToml.getFailOnSeverity(), Optional.of(GateSeverity.LOW));
            Assert.assertEquals(gatedToml.getFailOnCount(), Optional.empty());
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    @Test(description = "test the test-runner constructor ignores the [gate] table and throws the failure")
    void testTestRunnerConstructor() throws IOException {
        ScanToolException exception;
        try {
            System.setProperty("user.dir", gateConfigProject.toString());
            new TestRunnerScanCmd(gateConfigProject, printStream, ReportFormat.BALLERINA).execute();
            String output = readOutput(true);
            Assert.assertTrue(output.contains(TOTAL_ISSUES + " issue(s)"), output);
            Assert.assertFalse(output.contains("bal scan: error:"), "the gate is off for test runners: " + output);

            ScanCmd scanCmd = new TestRunnerScanCmd(invalidGateConfigProject, printStream, ReportFormat.BALLERINA);
            System.setProperty("user.dir", invalidGateConfigProject.toString());
            exception = Assert.expectThrows(ScanToolException.class, scanCmd::execute);
        } finally {
            System.setProperty("user.dir", userDir);
        }
        Assert.assertEquals(exception.getMessage(), "Scan.toml has 3 errors");
        Assert.assertEquals(exception.diagnostics().size(), 3);
        Assert.assertTrue(readOutput(true).contains("bal scan: error: Scan.toml has 3 errors\n"));
    }

    @Test(description = "test the test-runner constructor applies the given format, and defaults to JSON")
    void testTestRunnerConstructorFormat() throws IOException {
        System.setProperty("user.dir", gateIssuesProject.toString());
        try {
            new TestRunnerScanCmd(gateIssuesProject, printStream, ReportFormat.SARIF).execute();
            Assert.assertTrue(readOutput(true).contains("\"$schema\""), "the report format is applied");

            new TestRunnerScanCmd(gateIssuesProject, printStream, null).execute();
            String output = readOutput(true);
            Assert.assertFalse(output.contains("bal scan: error:"), output);
            Assert.assertFalse(output.contains("\"$schema\""), output);
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    @Test(description = "test the gate is off when an analysis platform triggers the scan")
    void testGateOffWhenPlatformTriggered() throws IOException {
        ScanRun run = scan(gateConfigProject, "--platform-triggered");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertEquals(run.errorReport(), "", run.output());
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

        // The project has one active issue, so the gate would be tripped as well
        run = scan(testResourcesRoot.resolve("bal-project-with-suppressions"), "--require-suppression-reason",
                "--fail-on-count=1");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.errorReport().startsWith("bal scan: error: 1 invalid suppression\n"), run.output());
        Assert.assertFalse(run.output().contains("at or above"), run.output());
    }

    @Test(description = "test errors and warnings are written to standard error, and results to standard output")
    void testOutputStreams() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setProperty("user.dir", gateConfigProject.toString());
        try (PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
             PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            ScanCmd scanCmd = new ScanCmd(outStream, errStream);
            new CommandLine(scanCmd).parseArgs("--fail-on-severity=INFO");
            scanCmd.execute();
            Assert.assertEquals(scanCmd.exitCode(), 1);
        } finally {
            System.setProperty("user.dir", userDir);
        }
        String output = out.toString(StandardCharsets.UTF_8);
        String error = err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        Assert.assertEquals(JsonParser.parseString(output).getAsJsonArray().size(), TOTAL_ISSUES, output);
        Assert.assertTrue(error.contains("bal scan: warning: --fail-on-severity=INFO overrides "), error);
        Assert.assertTrue(error.contains("View scan results at:"), error);
        Assert.assertTrue(error.trim().endsWith("issues at or above INFO, limit 1 (" + ALL_ISSUES + ")"), error);
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

    private static class TestRunnerScanCmd extends ScanCmd {
        TestRunnerScanCmd(Path projectPath, PrintStream outputStream, ReportFormat format) {
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
