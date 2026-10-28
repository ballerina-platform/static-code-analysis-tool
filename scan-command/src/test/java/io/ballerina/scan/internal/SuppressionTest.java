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
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.scan.ScanReportTestUtils.readScanReportData;

/**
 * In-source suppression tests.
 *
 * @since 0.13.0
 */
public class SuppressionTest extends BaseTest {
    private final Path suppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-suppressions");
    private final Path requirementsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-suppression-requirements");
    private final Path invalidSuppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-invalid-suppressions");
    private final Path analyzerSuppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-analyzer-suppressions");
    private final Path disabledSuppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-disabled-suppressions");
    private final Path sameConstructSuppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-same-construct-suppressions");
    private final Path unresolvedSuppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-unresolved-suppressions");
    private final Path unknownCoreRuleProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-unknown-core-rule");
    private final Path partSuppressionsProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-part-suppressions");
    private final Path requirementsWorkspace = testResources.resolve("test-resources")
            .resolve("workspace-with-suppression-requirements");

    @AfterClass
    void cleanup() {
        for (Path project : List.of(suppressionsProject, requirementsProject, invalidSuppressionsProject,
                analyzerSuppressionsProject, disabledSuppressionsProject, sameConstructSuppressionsProject,
                unresolvedSuppressionsProject, unknownCoreRuleProject, partSuppressionsProject, requirementsWorkspace,
                requirementsWorkspace.resolve("clean-package"),
                requirementsWorkspace.resolve("requirements-package"))) {
            Path target = project.resolve("target");
            if (Files.exists(target)) {
                ProjectUtils.deleteDirectory(target);
            }
        }
    }

    @Test(description = "test suppressed issues are marked in the JSON report and omitted from the console")
    void testSuppressionsInJsonReport() throws IOException {
        String console = runScan(suppressionsProject);

        JsonArray issues = readReport(suppressionsProject, "scan_results.json").getAsJsonArray();
        Assert.assertEquals(issues.size(), 3);
        List<String> justifications = new ArrayList<>();
        int suppressedCount = 0;
        for (JsonElement issue : issues) {
            Assert.assertEquals(issue.getAsJsonObject().getAsJsonObject("rule").get("id").getAsString(),
                    "ballerina:1");
            JsonArray suppressions = issue.getAsJsonObject().getAsJsonArray("suppressions");
            if (suppressions == null) {
                continue;
            }
            suppressedCount++;
            JsonObject suppression = suppressions.get(0).getAsJsonObject();
            Assert.assertEquals(suppression.get("kind").getAsString(), "inSource");
            Assert.assertEquals(suppression.get("status").getAsString(), "accepted");
            justifications.add(suppression.has("justification")
                    ? suppression.get("justification").getAsString() : null);
        }
        Assert.assertEquals(suppressedCount, 2);
        Assert.assertTrue(justifications.contains("Literal input that always parses"));
        Assert.assertTrue(justifications.contains(null), "a suppression without a reason has no justification");

        String consoleIssues = console.substring(console.indexOf("\n[") + 1, console.lastIndexOf(']') + 1);
        Assert.assertEquals(JsonParser.parseString(consoleIssues).getAsJsonArray().size(), 1);
        Assert.assertTrue(console.contains("1 issue(s), 2 suppressed"));
        Assert.assertTrue(console.contains("WARNING [main.bal:(23:29,23:42)] suppression of 'ballerina:7' did "
                + "not match any issue"), console);
    }

    @Test(description = "test only the issues go to standard output, and the status and summary to standard error")
    void testConsoleOutputStreams() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setProperty("user.dir", suppressionsProject.toString());
        new ScanCmd(new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8)).execute();
        System.setProperty("user.dir", userDir);

        String stdout = out.toString(StandardCharsets.UTF_8);
        String stderr = err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
        Assert.assertEquals(JsonParser.parseString(stdout).getAsJsonArray().size(), 1, stdout);
        Assert.assertTrue(stderr.contains("Running Scans"), stderr);
        Assert.assertTrue(stderr.contains("did not match any issue"), stderr);
        Assert.assertTrue(stderr.contains("View scan results at:"), stderr);
        Assert.assertTrue(stderr.endsWith("\n1 issue(s), 2 suppressed"), "the summary is the last line: " + stderr);
    }

    @Test(description = "test the HTML report lists suppressed issues with their suppression")
    void testSuppressionsInHtmlReport() throws IOException {
        runScan(suppressionsProject, "--scan-report");

        JsonObject scanData = readScanReportData(suppressionsProject.resolve("target").resolve("report")
                .resolve("index.html"));
        List<JsonObject> issues = new ArrayList<>();
        scanData.getAsJsonArray("scannedFiles").forEach(file -> file.getAsJsonObject().getAsJsonArray("issues")
                .forEach(issue -> issues.add(issue.getAsJsonObject())));
        Assert.assertEquals(issues.size(), 3);
        List<JsonObject> suppressions = issues.stream().filter(issue -> issue.has("suppression"))
                .map(issue -> issue.getAsJsonObject("suppression")).toList();
        Assert.assertEquals(suppressions.size(), 2);

        JsonObject withReason = suppressions.stream().filter(suppression -> suppression.has("justification"))
                .findFirst().orElseThrow();
        Assert.assertEquals(withReason.get("justification").getAsString(), "Literal input that always parses");
        Assert.assertEquals(withReason.getAsJsonArray("rules").get(0).getAsString(), "ballerina:1");
        JsonObject everyRule = suppressions.stream().filter(suppression -> !suppression.has("justification"))
                .findFirst().orElseThrow();
        Assert.assertTrue(everyRule.getAsJsonArray("rules").isEmpty(), "a suppression without rules covers all");
    }

    @Test(description = "test suppressed issues carry an accepted inSource suppression in the SARIF report")
    void testSuppressionsInSarifReport() throws IOException {
        runScan(suppressionsProject, "--format=sarif");

        JsonArray results = readReport(suppressionsProject, "scan_results.sarif").getAsJsonObject()
                .getAsJsonArray("runs").get(0).getAsJsonObject().getAsJsonArray("results");
        Assert.assertEquals(results.size(), 3);
        long suppressed = 0;
        for (JsonElement result : results) {
            // SARIF requires every result of the run to carry the property once any result does
            JsonArray suppressions = result.getAsJsonObject().getAsJsonArray("suppressions");
            Assert.assertNotNull(suppressions);
            if (suppressions.isEmpty()) {
                continue;
            }
            suppressed++;
            JsonObject suppression = suppressions.get(0).getAsJsonObject();
            Assert.assertEquals(suppression.get("kind").getAsString(), "inSource");
            Assert.assertEquals(suppression.get("status").getAsString(), "accepted");
        }
        Assert.assertEquals(suppressed, 2);
    }

    @Test(description = "test a SARIF report without suppressions carries no suppression information")
    void testSarifReportWithoutSuppressions() throws IOException {
        runScan(suppressionsProject, "--format=sarif", "--ignore-suppressions");

        JsonArray results = readReport(suppressionsProject, "scan_results.sarif").getAsJsonObject()
                .getAsJsonArray("runs").get(0).getAsJsonObject().getAsJsonArray("results");
        Assert.assertEquals(results.size(), 3);
        results.forEach(result -> Assert.assertFalse(result.getAsJsonObject().has("suppressions")));
    }

    @Test(description = "test a rule-specific suppression wins on a construct and a repeated rule is reported")
    void testRuleSpecificSuppressionPreferredOnSameConstruct() throws IOException {
        String console = runScan(sameConstructSuppressionsProject);

        JsonArray issues = readReport(sameConstructSuppressionsProject, "scan_results.json").getAsJsonArray();
        List<String> justifications = new ArrayList<>();
        for (JsonElement issue : issues) {
            JsonObject suppression = issue.getAsJsonObject().getAsJsonArray("suppressions").get(0).getAsJsonObject();
            justifications.add(suppression.get("justification").getAsString());
        }
        Assert.assertEquals(justifications, List.of("Rule-specific", "Rule-specific", "First", "First"));
        Assert.assertTrue(console.contains("0 issue(s), 4 suppressed"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(20:5,20:39)] suppression of all rules did not "
                + "match any issue"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(26:5,26:39)] suppression of all rules did not "
                + "match any issue"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(31:29,31:42)] rule 'ballerina:1' is already "
                + "suppressed on this construct"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(36:5,36:48)] every rule is already suppressed on "
                + "this construct"), console);
        Assert.assertFalse(console.contains("WARNING [main.bal:(36:5,36:48)] suppression of all rules"), console);
        Assert.assertFalse(console.contains("suppression of 'ballerina:1' did not match any issue"), console);
    }

    @Test(description = "test suppressions on a tuple member, an included record parameter and an anonymous "
            + "function's return type are collected, and an escaped field name is read as the field")
    void testSuppressionsOnPartsOfConstructs() throws IOException {
        String console = runScan(partSuppressionsProject);

        Assert.assertTrue(console.contains("WARNING [main.bal:(23:36,23:49)] suppression of 'ballerina:1' did not "
                + "match any issue"), "the tuple member's suppression is collected: " + console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(25:42,25:55)] suppression of 'ballerina:1' did not "
                + "match any issue"), "the included record parameter's suppression is collected: " + console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(29:61,29:74)] suppression of 'ballerina:1' did not "
                + "match any issue"), "the anonymous function's suppression is collected: " + console);
        Assert.assertTrue(console.contains("1 issue(s), 1 suppressed"), "the escaped rules field applies: " + console);
    }

    @Test(description = "test rules and reasons are read from the compiler-resolved annotation value")
    void testRuleValuesAreResolvedByTheCompiler() throws IOException {
        String console = runScan(unresolvedSuppressionsProject, "--require-suppression-rule-id",
                "--require-suppression-reason");

        JsonArray issues = readReport(unresolvedSuppressionsProject, "scan_results.json").getAsJsonArray();
        Assert.assertEquals(issues.size(), 6);
        Assert.assertTrue(console.contains("1 issue(s), 5 suppressed"),
                "constant, template, escaped, commented and worker suppressions all apply: " + console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(30:21,30:26)] suppression of 'ballerina:7' did not "
                + "match any issue"), "a shorthand rules field names the rules of its constant");
        Assert.assertFalse(console.contains("suppression names no rule"), console);
        Assert.assertFalse(console.contains("suppression has no reason"), "a commented reason is still read");
    }

    @Test(description = "test --ignore-suppressions reports every issue as active")
    void testIgnoreSuppressions() throws IOException {
        String console = runScan(suppressionsProject, "--ignore-suppressions");

        JsonArray issues = readReport(suppressionsProject, "scan_results.json").getAsJsonArray();
        Assert.assertEquals(issues.size(), 3);
        issues.forEach(issue -> Assert.assertFalse(issue.getAsJsonObject().has("suppressions")));
        Assert.assertFalse(console.contains("suppressed"));
        Assert.assertTrue(console.contains("3 issue(s)"), console);
        Assert.assertFalse(console.contains("did not match any issue"),
                "the audit run does not report unmatched suppressions");
    }

    @Test(description = "test suppressions disabled in Scan.toml report every issue as active")
    void testSuppressionsDisabledInScanToml() throws IOException {
        String console = runScan(disabledSuppressionsProject);

        JsonArray issues = readReport(disabledSuppressionsProject, "scan_results.json").getAsJsonArray();
        Assert.assertEquals(issues.size(), 3);
        issues.forEach(issue -> Assert.assertFalse(issue.getAsJsonObject().has("suppressions")));
        Assert.assertFalse(console.contains("suppressed"));
        Assert.assertFalse(console.contains("did not match any issue"));

        String response = ScanLanguageServerTool.runScan(disabledSuppressionsProject.toAbsolutePath().toString(),
                Map.of());
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        Assert.assertEquals(result.getAsJsonArray("activeIssues").size(), 3, response);
        Assert.assertEquals(result.getAsJsonArray("suppressedIssues").size(), 0);
    }

    @Test(description = "test a suppression of an excluded rule is not reported as unmatched")
    void testSuppressionOfExcludedRuleIsNotUnmatched() throws IOException {
        String console = runScan(suppressionsProject, "--exclude-rules=ballerina:1");

        Assert.assertEquals(readReport(suppressionsProject, "scan_results.json").getAsJsonArray().size(), 0);
        Assert.assertFalse(console.contains("suppression of 'ballerina:1' did not match any issue"), console);
        Assert.assertFalse(console.contains("suppression of all rules did not match any issue"), console);
        Assert.assertTrue(console.contains("suppression of 'ballerina:7' did not match any issue"), console);
    }

    @Test(description = "test only suppressions of the included rules are reported as unmatched")
    void testSuppressionOfRuleNotIncludedIsNotUnmatched() throws IOException {
        String console = runScan(suppressionsProject, "--include-rules=ballerina:7");

        Assert.assertEquals(readReport(suppressionsProject, "scan_results.json").getAsJsonArray().size(), 0);
        Assert.assertFalse(console.contains("suppression of 'ballerina:1' did not match any issue"), console);
        Assert.assertFalse(console.contains("suppression of all rules did not match any issue"), console);
        Assert.assertTrue(console.contains("suppression of 'ballerina:7' did not match any issue"), console);
    }

    @Test(description = "test a suppression of an unknown rule fails the scan even when the rule is not included")
    void testUnknownRuleFailsTheScanWhenNotIncluded() throws IOException {
        System.setProperty("user.dir", unknownCoreRuleProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs("--include-rules=ballerina:1");
        Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        String console = readOutput(true);
        Assert.assertTrue(console.contains("unknown rule 'ballerina:999'"), console);
    }

    @Test(description = "test the command-line requirement fails the scan before anything is reported")
    void testRequireReasonFromCommandLine() throws IOException {
        deleteTarget(suppressionsProject);
        System.setProperty("user.dir", suppressionsProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs("--require-suppression-reason");
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        Assert.assertTrue(exception.getDetailedMessages().get(0)
                .contains("1 suppression(s) are invalid or do not meet the configured suppression requirements"));
        String console = readOutput(true);
        Assert.assertTrue(console.contains("ERROR [main.bal:(28:1,28:15)] suppression has no reason"), console);
        assertNothingReported(suppressionsProject, console);
    }

    @Test(description = "test the Scan.toml requirements fail the scan before anything is reported")
    void testRequirementsFromScanToml() throws IOException {
        deleteTarget(requirementsProject);
        System.setProperty("user.dir", requirementsProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs("--scan-report");
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        Assert.assertTrue(exception.getDetailedMessages().get(0)
                .contains("2 suppression(s) are invalid or do not meet the configured suppression requirements"));
        String console = readOutput(true);
        Assert.assertTrue(console.contains("ERROR [main.bal:(20:5,20:44)] suppression has no reason"), console);
        Assert.assertTrue(console.contains("ERROR [main.bal:(22:5,22:51)] suppression names no rule"), console);
        assertNothingReported(requirementsProject, console);
    }

    @Test(description = "test a workspace package that misses a requirement fails the scan before any package is "
            + "reported")
    void testRequirementsFailWorkspaceBeforeAnyReport() throws IOException {
        deleteTarget(requirementsWorkspace);
        System.setProperty("user.dir", requirementsWorkspace.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs("--scan-report");
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        Assert.assertTrue(exception.getDetailedMessages().get(0)
                .contains("2 suppression(s) are invalid or do not meet the configured suppression requirements"));
        String console = readOutput(true);
        Assert.assertTrue(console.contains("suppression has no reason"), console);
        assertNothingReported(requirementsWorkspace, console);
        Assert.assertFalse(Files.exists(requirementsWorkspace.resolve("clean-package").resolve("target")
                .resolve("report")), "the package without suppression errors is not reported either");
        Assert.assertFalse(Files.exists(requirementsWorkspace.resolve("requirements-package").resolve("target")
                .resolve("report")));
    }

    @Test(description = "test the scan tool rejects a rule it does not know before anything is reported")
    void testUnknownRulesFailTheScan() throws IOException {
        deleteTarget(unknownCoreRuleProject);
        System.setProperty("user.dir", unknownCoreRuleProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        Assert.assertTrue(exception.getDetailedMessages().get(0)
                .contains("1 suppression(s) are invalid or do not meet the configured suppression requirements"));
        String console = readOutput(true);
        Assert.assertTrue(console.contains("ERROR [main.bal:(20:29,20:44)] unknown rule 'ballerina:999': "
                + "'ballerina' does not define this rule"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(24:29,24:42)] cannot verify rule 'acme/lint:1': "
                + "'acme/lint' provides no scan rules to this project"), console);
        Assert.assertFalse(console.contains("did not match"), console);
        assertNothingReported(unknownCoreRuleProject, console);
    }

    @Test(description = "test ignored suppressions do not fail the scan on suppression errors")
    void testIgnoredSuppressionsDoNotFailTheScan() throws IOException {
        String console = runScan(requirementsProject, "--ignore-suppressions");
        Assert.assertFalse(console.contains("suppression has no reason"), console);
        Assert.assertFalse(console.contains("suppression names no rule"), console);

        console = runScan(unknownCoreRuleProject, "--ignore-suppressions");
        Assert.assertFalse(console.contains("unknown rule 'ballerina:999'"), console);
        Assert.assertFalse(console.contains("suppressed"), console);
    }

    @Test(description = "test the language server returns suppressed issues and suppression diagnostics")
    void testLanguageServerSuppressions() {
        String response = ScanLanguageServerTool.runScan(suppressionsProject.toAbsolutePath().toString(), Map.of());

        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        Assert.assertTrue(result.get("success").getAsBoolean(), response);
        Assert.assertEquals(result.getAsJsonArray("activeIssues").size(), 1);
        JsonArray suppressedIssues = result.getAsJsonArray("suppressedIssues");
        Assert.assertEquals(suppressedIssues.size(), 2);
        Assert.assertEquals(suppressedIssues.get(0).getAsJsonObject().get("justification").getAsString(),
                "Literal input that always parses");
        JsonArray diagnostics = result.getAsJsonArray("suppressionDiagnostics");
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.get(0).getAsJsonObject().get("severity").getAsString(), "WARNING");
        Assert.assertEquals(diagnostics.get(0).getAsJsonObject().get("filePath").getAsString(),
                suppressedIssues.get(0).getAsJsonObject().getAsJsonObject("issueContext").get("filePath")
                        .getAsString());
        // The unmatched rule's diagnostic points at the rule, not at the whole annotation
        JsonObject unmatched = diagnostics.get(0).getAsJsonObject();
        Assert.assertEquals(List.of(unmatched.get("startLine").getAsInt(), unmatched.get("startColumn").getAsInt(),
                unmatched.get("endLine").getAsInt(), unmatched.get("endColumn").getAsInt()), List.of(22, 28, 22, 41));
    }

    @Test(description = "test rules of an analyzer declared only in Scan.toml are checked when the scan runs")
    void testScanTomlAnalyzerRulesCheckedUnderScan() throws IOException {
        BuildProject project = BuildProject.load(analyzerSuppressionsProject.toAbsolutePath());
        long buildDiagnostics = project.currentPackage().getCompilation().diagnosticResult().diagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().code().startsWith("SCAN_SUPPRESS_"))
                .count();
        Assert.assertEquals(buildDiagnostics, 0, "a build checks only the form of the rule IDs");

        System.setProperty("user.dir", analyzerSuppressionsProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        String console = readOutput(true);
        Assert.assertTrue(console.contains("unknown rule 'exampleOrg/example_module_static_code_analyzer:99': "
                + "'exampleOrg/example_module_static_code_analyzer' does not define this rule"), console);
        Assert.assertFalse(console.contains("unknown rule 'exampleOrg/example_module_static_code_analyzer:1'"),
                console);
    }

    private String runScan(Path project, String... args) throws IOException {
        System.setProperty("user.dir", project.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        return readOutput(true);
    }

    private static void assertNothingReported(Path project, String console) {
        Assert.assertFalse(console.contains("\"rule\""), "no issue is printed: " + console);
        Assert.assertFalse(console.contains("issue(s)"), "no summary is printed: " + console);
        Assert.assertFalse(console.contains("View scan"), "no report is saved: " + console);
        Assert.assertFalse(Files.exists(project.resolve("target").resolve("report")), "no report is saved");
    }

    private static void deleteTarget(Path project) {
        Path target = project.resolve("target");
        if (Files.exists(target)) {
            ProjectUtils.deleteDirectory(target);
        }
    }

    private static JsonElement readReport(Path project, String fileName) throws IOException {
        return JsonParser.parseString(Files.readString(project.resolve("target").resolve("report")
                .resolve(fileName), StandardCharsets.UTF_8));
    }
}
