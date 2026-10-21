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
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @AfterClass
    void cleanup() {
        for (Path project : List.of(suppressionsProject, requirementsProject, invalidSuppressionsProject,
                analyzerSuppressionsProject, disabledSuppressionsProject, sameConstructSuppressionsProject)) {
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
        Assert.assertTrue(console.contains("WARNING [main.bal:(23:5,23:71)] suppression of 'ballerina:7' did "
                + "not match any issue"), console);
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

    @Test(description = "test a rule-specific suppression takes precedence over a blanket one on the same construct")
    void testRuleSpecificSuppressionPreferredOnSameConstruct() throws IOException {
        String console = runScan(sameConstructSuppressionsProject);

        JsonArray issues = readReport(sameConstructSuppressionsProject, "scan_results.json").getAsJsonArray();
        Assert.assertEquals(issues.size(), 2);
        for (JsonElement issue : issues) {
            JsonObject suppression = issue.getAsJsonObject().getAsJsonArray("suppressions").get(0).getAsJsonObject();
            Assert.assertEquals(suppression.get("justification").getAsString(), "Rule-specific");
        }
        Assert.assertTrue(console.contains("0 issue(s), 2 suppressed"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(20:5,20:39)] suppression of all rules did not "
                + "match any issue"), console);
        Assert.assertTrue(console.contains("WARNING [main.bal:(26:5,26:39)] suppression of all rules did not "
                + "match any issue"), console);
        Assert.assertFalse(console.contains("suppression of 'ballerina:1' did not match any issue"), console);
    }

    @Test(description = "test --ignore-suppressions reports every issue as active")
    void testIgnoreSuppressions() throws IOException {
        String console = runScan(suppressionsProject, "--ignore-suppressions");

        JsonArray issues = readReport(suppressionsProject, "scan_results.json").getAsJsonArray();
        Assert.assertEquals(issues.size(), 3);
        issues.forEach(issue -> Assert.assertFalse(issue.getAsJsonObject().has("suppressions")));
        Assert.assertFalse(console.contains("suppressed"));
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

    @Test(description = "test a suppression of an excluded rule is reported as unmatched")
    void testSuppressionOfExcludedRuleIsUnmatched() throws IOException {
        String console = runScan(suppressionsProject, "--exclude-rules=ballerina:1");

        Assert.assertEquals(readReport(suppressionsProject, "scan_results.json").getAsJsonArray().size(), 0);
        Assert.assertTrue(console.contains("suppression of 'ballerina:1' did not match any issue"), console);
        Assert.assertTrue(console.contains("suppression of all rules did not match any issue"), console);
    }

    @Test(description = "test the command-line requirement fails the scan but still suppresses")
    void testRequireReasonFromCommandLine() throws IOException {
        System.setProperty("user.dir", suppressionsProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs("--require-suppression-reason");
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        Assert.assertTrue(exception.getDetailedMessages().get(0)
                .contains("1 suppression(s) do not meet the configured suppression requirements"));
        String console = readOutput(true);
        Assert.assertTrue(console.contains("ERROR [main.bal:(28:1,28:15)] suppression has no reason"), console);
        Assert.assertTrue(console.contains("1 issue(s), 2 suppressed"), "the suppressions still apply");
    }

    @Test(description = "test the Scan.toml requirements fail the scan")
    void testRequirementsFromScanToml() throws IOException {
        System.setProperty("user.dir", requirementsProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        Assert.assertTrue(exception.getDetailedMessages().get(0)
                .contains("2 suppression(s) do not meet the configured suppression requirements"));
        String console = readOutput(true);
        Assert.assertTrue(console.contains("ERROR [main.bal:(20:5,20:44)] suppression has no reason"), console);
        Assert.assertTrue(console.contains("ERROR [main.bal:(22:5,22:51)] suppression names no rule"), console);
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
    }

    @Test(description = "test the ballerina/scan compiler plugin validates rule IDs")
    void testCompilerPluginRuleIdValidation() {
        BuildProject project = BuildProject.load(invalidSuppressionsProject.toAbsolutePath());
        List<String> diagnostics = new ArrayList<>();
        for (Diagnostic diagnostic : project.currentPackage().getCompilation().diagnosticResult().diagnostics()) {
            if (diagnostic.diagnosticInfo().code().startsWith("SCAN_SUPPRESS_")) {
                DiagnosticSeverity severity = diagnostic.diagnosticInfo().severity();
                diagnostics.add(severity + " " + (diagnostic.location().lineRange().startLine().line() + 1) + " "
                        + diagnostic.message());
            }
        }

        Assert.assertEquals(diagnostics, List.of(
                "ERROR 25 unknown rule 'ballerina:999': 'ballerina' does not define this rule",
                "ERROR 27 unknown rule 'ballerina:999': 'ballerina' does not define this rule",
                "ERROR 29 invalid rule ID 'http:1': expected 'ballerina:<number>' or '<org>/<package>:<number>'",
                "WARNING 31 cannot verify rule 'acme/lint:1': 'acme/lint' is not a direct dependency with scan "
                        + "rules; it is checked again when 'bal scan' runs",
                "WARNING 33 rule 'ballerina:1' is listed more than once",
                "WARNING 35 blank suppression reason; it is treated as absent",
                "ERROR 37 empty rule list; omit 'rules' to suppress every rule"));
    }

    @Test(description = "test rules of an analyzer declared only in Scan.toml are checked when the scan runs")
    void testScanTomlAnalyzerRulesCheckedUnderScan() throws IOException {
        BuildProject project = BuildProject.load(analyzerSuppressionsProject.toAbsolutePath());
        long buildWarnings = project.currentPackage().getCompilation().diagnosticResult().diagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().code().equals("SCAN_SUPPRESS_103"))
                .count();
        Assert.assertEquals(buildWarnings, 2, "the analyzer is not a dependency of an ordinary build");

        System.setProperty("user.dir", analyzerSuppressionsProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);

        String console = readOutput(true);
        Assert.assertTrue(console.contains("unknown rule 'exampleOrg/example_module_static_code_analyzer:99': "
                + "'exampleOrg/example_module_static_code_analyzer' does not define this rule"), console);
        Assert.assertFalse(console.contains("example_module_static_code_analyzer:1'"),
                "a rule the analyzer defines is accepted");
    }

    private String runScan(Path project, String... args) throws IOException {
        System.setProperty("user.dir", project.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        return readOutput(true);
    }

    private static JsonElement readReport(Path project, String fileName) throws IOException {
        return JsonParser.parseString(Files.readString(project.resolve("target").resolve("report")
                .resolve(fileName), StandardCharsets.UTF_8));
    }
}
