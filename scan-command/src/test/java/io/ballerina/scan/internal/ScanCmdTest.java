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

package io.ballerina.scan.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.OwaspCoverage;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Severity;
import io.ballerina.scan.Source;
import io.ballerina.scan.Standards;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.scan.ScanReportTestUtils.readScanReportData;
import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;
import static io.ballerina.scan.internal.ScanToolConstants.BALLERINAX_ORG;
import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_ORG;
import static io.ballerina.scan.utils.DiagnosticCode.COMPILATION_CONTAINS_ERRORS;
import static io.ballerina.scan.utils.DiagnosticCode.EMPTY_PACKAGE;
import static io.ballerina.scan.utils.DiagnosticLog.error;

/**
 * Scan command tests.
 *
 * @since 0.1.0
 */
public class ScanCmdTest extends BaseTest {
    private final Path validBalProject = testResources.resolve("test-resources")
            .resolve("valid-bal-project");

    private static final String RESULTS_DIRECTORY = "results";
    // Serialized as startLine 16, startLineOffset 17, endLine 23, endLineOffset 1.
    private static final BLangDiagnosticLocation REPORT_ISSUE_LOCATION = new BLangDiagnosticLocation("main.bal",
            16, 23, 17, 1, 748, 4);

    @AfterTest
    void cleanup() {
        Path resultsDirectoryPath = validBalProject.resolve(RESULTS_DIRECTORY);
        removeFile(resultsDirectoryPath);
    }

    private void removeFile(Path filePath) {
        if (Files.exists(filePath)) {
            ProjectUtils.deleteDirectory(filePath);
        }
    }

    @Test(description = "test scan command override methods")
    void testScanCommandOverrideMethods() throws IOException {
        ScanCmd scanCmd = new ScanCmd();
        String expected = "scan";
        Assert.assertEquals(scanCmd.getName(), expected);

        StringBuilder usageResult = new StringBuilder();
        scanCmd.printUsage(usageResult);
        expected = "Tool providing static code analysis support for Ballerina";
        Assert.assertEquals(usageResult.toString(), expected);

        StringBuilder longDescription = new StringBuilder();
        scanCmd.printLongDesc(longDescription);
        expected = getExpectedOutput("tool-help.txt");
        Assert.assertEquals(longDescription.toString()
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR), expected);
    }

    @Test(description = "test scan command with help flag")
    void testScanCommandWithHelpFlag() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--help"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = getExpectedOutput("tool-help.txt");
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test scan command with Ballerina project")
    void testScanCommandProject() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with an empty Ballerina project")
    void testScanCommandEmptyProject() throws IOException {
        Path emptyBalProject = testResources.resolve("test-resources").resolve("empty-bal-project");
        System.setProperty("user.dir", emptyBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = error(EMPTY_PACKAGE);
        String actual = readOutput(true).trim().split("\n")[0];
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "test scan command with Ballerina project with single file as argument")
    void testScanCommandProjectWithArgument() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String inputPath = validBalProject.resolve("main.bal").toString();
        String[] args = {inputPath};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = "The specified path is not a valid Ballerina project: " + inputPath + ". Please "
                + "provide a valid Ballerina project path and try again.";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with single file project with single file as argument")
    void testScanCommandSingleFileProject() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {validBalProject.resolve("main.bal").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with single file project with project directory as argument")
    void testScanCommandSingleFileProjectWithDirectoryAsArgument() throws IOException {
        Path parentDirectory = testResources.resolve("test-resources").toAbsolutePath();
        ScanCmd scanCmd = new ScanCmd(printStream);
        String inputPath = parentDirectory.resolve("valid-single-file-project").toString();
        String[] args = {inputPath};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = "The specified path is not a valid Ballerina project: " + inputPath + ". Please "
                + "provide a valid Ballerina project path and try again.";
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test scan command with single file project without arguments")
    void testScanCommandSingleFileProjectWithoutArgument() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = "The specified path is not a valid Ballerina project: " + validBalProject + ". Please "
                + "provide a valid Ballerina project path and try again.";
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test scan command with single file project with too many arguments")
    void testScanCommandSingleFileProjectWithTooManyArguments() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"main.bal", "argument2"};
        try {
            new CommandLine(scanCmd).parseArgs(args);
            Assert.fail("Expected CommandLine.UnmatchedArgumentException");
        } catch (CommandLine.UnmatchedArgumentException e) {
            String expected = "picocli.CommandLine$UnmatchedArgumentException: " +
                    "Unmatched argument at index 1: 'argument2'";
            Assert.assertEquals(e.toString(), expected);
        }
        System.setProperty("user.dir", userDir);
    }

    @Test(description = "test scan command with method for saving results to file when analysis issues are present")
    void testScanCommandSaveToDirectoryMethodWhenIssuePresent() throws IOException {
        Rule coreRule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        Rule externalRule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG, "exampleOrg",
                "exampleName");
        BLangDiagnosticLocation location = new BLangDiagnosticLocation("main.bal", 16, 23,
                17, 1, 748, 4);
        List<Issue> issues = new ArrayList<>();
        issues.add(new IssueImpl(location, coreRule, Source.BUILT_IN, "main.bal",
                validBalProject.resolve("main.bal").toString()));
        issues.add(new IssueImpl(location, externalRule, Source.EXTERNAL, "main.bal",
                validBalProject.resolve("main.bal").toString()));
        Project project = ProjectLoader.load(validBalProject).project();
        Path resultsFile = ScanUtils.saveToDirectory(issues, project, null);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("issues-report.txt");
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test html analysis report carries every rule metadata field the report UI renders")
    void testScanReportWithFullRuleMetadata() throws IOException {
        Rule coreRule = RuleFactory.createCoreRule(RuleImpl.builder()
                .numericId(101)
                .name("Avoid SQL injection")
                .description("User input reaches a SQL query")
                .details("Untrusted input is concatenated into a SQL query.\nUse parameterized queries instead.")
                .helpUri("https://ballerina.io/learn/scan-rules/#sql-injection")
                .ruleKind(RuleKind.VULNERABILITY)
                .severity(Severity.BLOCKER)
                .tags(List.of("security", "sql"))
                .standards(new Standards(List.of(89, 20), List.of(
                        new OwaspCoverage(2025, List.of(5)),
                        new OwaspCoverage(2021, List.of(3, 4))))));
        Rule externalRule = RuleFactory.createRule(RuleImpl.builder()
                .numericId(7)
                .name("Unused variable")
                .description("Variable is never read")
                .details("Remove the variable or use it.")
                .helpUri("https://example.org/rules/7")
                .ruleKind(RuleKind.CODE_SMELL)
                .severity(Severity.LOW)
                .tags(List.of("maintainability"))
                .standards(new Standards(List.of(563), List.of())), "exampleOrg", "exampleName");

        JsonArray reportIssues = generateSingleFileReportIssues(List.of(
                new IssueImpl(REPORT_ISSUE_LOCATION, coreRule, Source.BUILT_IN, "main.bal", mainBalPath()),
                new IssueImpl(REPORT_ISSUE_LOCATION, externalRule, Source.EXTERNAL, "main.bal", mainBalPath())));
        Assert.assertEquals(reportIssues.size(), 2);

        JsonObject coreIssue = reportIssues.get(0).getAsJsonObject();
        Assert.assertEquals(coreIssue.keySet(), Set.of("ruleID", "name", "ruleKind", "severity", "issueType",
                "message", "details", "helpUri", "tags", "cwe", "owasp", "textRange"));
        Assert.assertEquals(coreIssue.get("ruleID").getAsString(), "ballerina:101");
        Assert.assertEquals(coreIssue.get("name").getAsString(), "Avoid SQL injection");
        Assert.assertEquals(coreIssue.get("ruleKind").getAsString(), "VULNERABILITY");
        Assert.assertEquals(coreIssue.get("severity").getAsString(), "BLOCKER");
        Assert.assertEquals(coreIssue.get("issueType").getAsString(), "BUILT_IN");
        Assert.assertEquals(coreIssue.get("message").getAsString(), "User input reaches a SQL query");
        Assert.assertEquals(coreIssue.get("details").getAsString(),
                "Untrusted input is concatenated into a SQL query.\nUse parameterized queries instead.");
        Assert.assertEquals(coreIssue.get("helpUri").getAsString(),
                "https://ballerina.io/learn/scan-rules/#sql-injection");
        Assert.assertEquals(toStrings(coreIssue.getAsJsonArray("tags")), List.of("security", "sql"));
        Assert.assertEquals(toInts(coreIssue.getAsJsonArray("cwe")), List.of(89, 20));
        JsonArray owasp = coreIssue.getAsJsonArray("owasp");
        Assert.assertEquals(owasp.size(), 2);
        assertOwaspCoverage(owasp.get(0).getAsJsonObject(), 2025, List.of(5));
        assertOwaspCoverage(owasp.get(1).getAsJsonObject(), 2021, List.of(3, 4));
        assertTextRange(coreIssue);

        JsonObject externalIssue = reportIssues.get(1).getAsJsonObject();
        Assert.assertEquals(externalIssue.get("ruleID").getAsString(), "exampleOrg/exampleName:7");
        Assert.assertEquals(externalIssue.get("name").getAsString(), "Unused variable");
        Assert.assertEquals(externalIssue.get("ruleKind").getAsString(), "CODE_SMELL");
        Assert.assertEquals(externalIssue.get("severity").getAsString(), "LOW");
        Assert.assertEquals(externalIssue.get("issueType").getAsString(), "EXTERNAL");
        Assert.assertEquals(externalIssue.get("message").getAsString(), "Variable is never read");
        Assert.assertEquals(externalIssue.get("details").getAsString(), "Remove the variable or use it.");
        Assert.assertEquals(externalIssue.get("helpUri").getAsString(), "https://example.org/rules/7");
        Assert.assertEquals(toStrings(externalIssue.getAsJsonArray("tags")), List.of("maintainability"));
        Assert.assertEquals(toInts(externalIssue.getAsJsonArray("cwe")), List.of(563));
        // An empty OWASP list is left out rather than written as [].
        Assert.assertFalse(externalIssue.has("owasp"), "Empty OWASP coverage should be omitted");
        assertTextRange(externalIssue);
    }

    @Test(description = "test html analysis report omits rule metadata fields the rule does not define")
    void testScanReportWithMinimalRuleMetadata() throws IOException {
        Rule coreRule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        Rule externalRule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG, "exampleOrg",
                "exampleName");

        JsonArray reportIssues = generateSingleFileReportIssues(List.of(
                new IssueImpl(REPORT_ISSUE_LOCATION, coreRule, Source.BUILT_IN, "main.bal", mainBalPath()),
                new IssueImpl(REPORT_ISSUE_LOCATION, externalRule, Source.EXTERNAL, "main.bal", mainBalPath())));
        Assert.assertEquals(reportIssues.size(), 2);

        List<String> expectedRuleIds = List.of("ballerina:101", "exampleOrg/exampleName:101");
        List<String> expectedIssueTypes = List.of("BUILT_IN", "EXTERNAL");
        for (int i = 0; i < reportIssues.size(); i++) {
            JsonObject issue = reportIssues.get(i).getAsJsonObject();
            // No severity, help URI, tags or standards, and details/name fall back to the description, so
            // only the always-present fields may appear.
            Assert.assertEquals(issue.keySet(),
                    Set.of("ruleID", "name", "ruleKind", "issueType", "message", "textRange"));
            Assert.assertEquals(issue.get("ruleID").getAsString(), expectedRuleIds.get(i));
            Assert.assertEquals(issue.get("issueType").getAsString(), expectedIssueTypes.get(i));
            Assert.assertEquals(issue.get("name").getAsString(), "rule 101");
            Assert.assertEquals(issue.get("message").getAsString(), "rule 101");
            Assert.assertEquals(issue.get("ruleKind").getAsString(), "BUG");
            assertTextRange(issue);
        }
    }

    @Test(description = "test html analysis report leaves out details that just repeat the description")
    void testScanReportOmitsDetailsSameAsDescription() throws IOException {
        Rule rule = RuleFactory.createCoreRule(RuleImpl.builder()
                .numericId(102)
                .name("Function too long")
                .description("Function exceeds the allowed length")
                .details("Function exceeds the allowed length")
                .ruleKind(RuleKind.CODE_SMELL)
                .severity(Severity.MEDIUM)
                .tags(List.of())
                .standards(new Standards(List.of(), List.of())));

        JsonArray reportIssues = generateSingleFileReportIssues(List.of(
                new IssueImpl(REPORT_ISSUE_LOCATION, rule, Source.BUILT_IN, "main.bal", mainBalPath())));
        Assert.assertEquals(reportIssues.size(), 1);

        JsonObject issue = reportIssues.get(0).getAsJsonObject();
        Assert.assertEquals(issue.keySet(),
                Set.of("ruleID", "name", "ruleKind", "severity", "issueType", "message", "textRange"));
        Assert.assertEquals(issue.get("name").getAsString(), "Function too long");
        Assert.assertEquals(issue.get("message").getAsString(), "Function exceeds the allowed length");
        Assert.assertEquals(issue.get("severity").getAsString(), "MEDIUM");
    }

    @Test(description = "test html analysis report groups issues under the file they were reported in")
    void testScanReportGroupsIssuesByFile() throws IOException {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        Path otherFile = testResources.resolve("test-resources").resolve("bal-project-with-config-file")
                .resolve("main.bal");
        BLangDiagnosticLocation otherLocation = new BLangDiagnosticLocation("main.bal", 1, 1, 0, 5, 0, 5);
        List<Issue> issues = List.of(
                new IssueImpl(REPORT_ISSUE_LOCATION, rule, Source.BUILT_IN, "main.bal", mainBalPath()),
                new IssueImpl(otherLocation, rule, Source.BUILT_IN, "main.bal", otherFile.toString()),
                new IssueImpl(REPORT_ISSUE_LOCATION, rule, Source.BUILT_IN, "main.bal", mainBalPath()));
        Project project = ProjectLoader.load(validBalProject).project();
        JsonObject scanData = readScanReportData(ScanUtils.generateScanReport(issues, project, null));

        JsonArray scannedFiles = scanData.getAsJsonArray("scannedFiles");
        Assert.assertEquals(scannedFiles.size(), 2);
        JsonObject mainFile = findScannedFile(scannedFiles, validBalProject.resolve("main.bal"));
        // Project documents come first in analysis order; files only known through issues follow.
        Assert.assertEquals(scannedFiles.get(0).getAsJsonObject(), mainFile);
        JsonObject secondFile = findScannedFile(scannedFiles, otherFile);
        Assert.assertEquals(mainFile.getAsJsonArray("issues").size(), 2);
        Assert.assertEquals(secondFile.getAsJsonArray("issues").size(), 1);
        Assert.assertEquals(secondFile.get("fileContent").getAsString(),
                Files.readString(otherFile, StandardCharsets.UTF_8));
        JsonObject secondRange = secondFile.getAsJsonArray("issues").get(0).getAsJsonObject()
                .getAsJsonObject("textRange");
        Assert.assertEquals(secondRange.get("startLine").getAsInt(), 1);
        Assert.assertEquals(secondRange.get("startLineOffset").getAsInt(), 0);
        Assert.assertEquals(secondRange.get("endLine").getAsInt(), 1);
        Assert.assertEquals(secondRange.get("endLineOffset").getAsInt(), 5);
    }

    @Test(description = "test html analysis report lists every analyzed workspace file, including files without "
            + "issues, and skips the document generated to import external analyzers")
    void testScanReportInWorkspaceListsAllAnalyzedFiles() throws IOException {
        Path workspacePath = testResources.resolve("test-resources").resolve("workspace-project");
        System.setProperty("user.dir", workspacePath.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--scan-report", "--include-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);

        JsonObject scanData = readScanReportData(workspacePath.resolve("target").resolve("report")
                .resolve("index.html"));
        Assert.assertEquals(scanData.get("projectName").getAsString(), "workspace-project");
        Assert.assertFalse(scanData.has("projectVersion"), "A workspace has no single version to show");
        Assert.assertEquals(scanData.get("projectKind").getAsString(), "WORKSPACE_PROJECT");
        List<String> packages = scanData.getAsJsonArray("packages").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .map(pkg -> pkg.get("name").getAsString() + "@" + pkg.get("version").getAsString() + ":"
                        + pkg.get("path").getAsString())
                .sorted()
                .toList();
        Assert.assertEquals(packages, List.of(
                "bal_project_with_analyzer_configurations@0.1.0:bal-project-with-analyzer-configurations",
                "bal_project_with_include_rule_configurations@0.1.0:bal-project-with-include-rule-configurations"));
        List<JsonObject> scannedFiles = scanData.getAsJsonArray("scannedFiles").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .sorted(Comparator.comparing(file -> file.get("relativePath").getAsString()))
                .toList();
        Assert.assertEquals(scannedFiles.stream().map(file -> file.get("fileName").getAsString()).toList(), List.of(
                "bal_project_with_analyzer_configurations" + File.separator + "main.bal",
                "bal_project_with_include_rule_configurations" + File.separator + "main.bal"));
        // Relative paths always use forward slashes so the report can build its folder tree on any OS.
        Assert.assertEquals(scannedFiles.stream().map(file -> file.get("relativePath").getAsString()).toList(),
                List.of("bal-project-with-analyzer-configurations/main.bal",
                        "bal-project-with-include-rule-configurations/main.bal"));
        Assert.assertEquals(scannedFiles.stream().map(file -> file.get("packageName").getAsString()).toList(),
                List.of("bal_project_with_analyzer_configurations", "bal_project_with_include_rule_configurations"));
    }

    private String mainBalPath() {
        return validBalProject.resolve("main.bal").toString();
    }

    // Generates a report for issues that are all in valid-bal-project/main.bal, checks the project and file
    // entries, and returns that file's issues.
    private JsonArray generateSingleFileReportIssues(List<Issue> issues) throws IOException {
        Project project = ProjectLoader.load(validBalProject).project();
        JsonObject scanData = readScanReportData(ScanUtils.generateScanReport(issues, project, null));
        Assert.assertEquals(scanData.get("projectName").getAsString(), "valid_bal_project");

        JsonArray scannedFiles = scanData.getAsJsonArray("scannedFiles");
        Assert.assertEquals(scannedFiles.size(), 1);
        Path mainBal = validBalProject.resolve("main.bal");
        JsonObject scannedFile = findScannedFile(scannedFiles, mainBal);
        Assert.assertEquals(scannedFile.get("fileName").getAsString(), "main.bal");
        Assert.assertEquals(scannedFile.get("fileContent").getAsString(),
                Files.readString(mainBal, StandardCharsets.UTF_8));
        return scannedFile.getAsJsonArray("issues");
    }

    // Compares as normalized paths so the check doesn't depend on the OS path separator.
    private static JsonObject findScannedFile(JsonArray scannedFiles, Path filePath) {
        Path expected = filePath.toAbsolutePath().normalize();
        return scannedFiles.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(file -> Path.of(file.get("filePath").getAsString()).toAbsolutePath().normalize()
                        .equals(expected))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Report has no entry for " + expected));
    }

    private static void assertTextRange(JsonObject issue) {
        JsonObject textRange = issue.getAsJsonObject("textRange");
        Assert.assertEquals(textRange.keySet(), Set.of("startLine", "startLineOffset", "endLine", "endLineOffset"));
        Assert.assertEquals(textRange.get("startLine").getAsInt(), 16);
        Assert.assertEquals(textRange.get("startLineOffset").getAsInt(), 17);
        Assert.assertEquals(textRange.get("endLine").getAsInt(), 23);
        Assert.assertEquals(textRange.get("endLineOffset").getAsInt(), 1);
    }

    private static void assertOwaspCoverage(JsonObject coverage, int year, List<Integer> categories) {
        Assert.assertEquals(coverage.get("year").getAsInt(), year);
        Assert.assertEquals(toInts(coverage.getAsJsonArray("categories")), categories);
    }

    private static List<String> toStrings(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static List<Integer> toInts(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsInt).toList();
    }

    @Test(description = "test method for printing static code analysis rules to the console")
    void testPrintRulesToConsole() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        Project project = ProjectLoader.load(ballerinaProject).project();
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        Assert.assertNotNull(scanTomlFile);
        System.setProperty("user.dir", userDir);
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(project, scanTomlFile);
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        Assert.assertFalse(externalAnalyzers.isEmpty());
        List<Rule> rules = CoreRule.rules();
        externalAnalyzers.values().forEach(rules::addAll);
        ScanUtils.printRulesToConsole(rules, printStream);
        String expected = getExpectedOutput("print-rules-to-console.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString());
        String actual = readOutput(true).trim();
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "test scan command with list rules flag")
    void testScanCommandWithListRulesFlag() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--list-rules"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = getExpectedOutput("list-rules-output.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString());
        String actual = readOutput(true).trim();
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "test scan command with list rules flag when the current directory is not a Ballerina project")
    void testScanCommandWithListRulesFlagWithoutArgumentOutsideBallerinaProject() throws IOException {
        Path nonProjectPath = testResources.resolve("test-resources").toAbsolutePath();
        System.setProperty("user.dir", nonProjectPath.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--list-rules"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = "The specified path is not a valid Ballerina project: " + nonProjectPath + ". Please "
                + "provide a valid Ballerina project path and try again.";
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test scan command with target directory flag on single file project")
    void testScanCommandWithTargetDirFlagOnSingleFileProject() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        Path singleFileProject = testResources.resolve("test-resources")
                .resolve("valid-single-file-project").resolve("main.bal");
        String[] args = {singleFileProject.toString(), "--target-dir=results"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = getExpectedOutput("single-file-report-generation.txt");
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test scan command with scan report flag on single file project")
    void testScanCommandWithScanReportFlagOnSingleFileProject() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        Path singleFileProject = testResources.resolve("test-resources")
                .resolve("valid-single-file-project").resolve("main.bal");
        String[] args = {singleFileProject.toString(), "--scan-report"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = getExpectedOutput("single-file-scan-report-generation.txt");
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test method for sorting static code analysis rules in specified order",
            dataProvider = "RulesProvider")
    void testSortRules(List<Rule> rules, String[] expectedOrder) {
        ScanUtils.sortRules(rules);
        for (int rule = 0; rule < rules.size(); rule++) {
            Assert.assertEquals(rules.get(rule).id(), expectedOrder[rule]);
        }
    }

    @DataProvider(name = "RulesProvider")
    Object[][] rulesProvider() {
        return new Object[][] {
                {
                    new ArrayList<>(List.of(
                            RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, BALLERINA_ORG, "exampleModule"),
                            RuleFactory.createRule(3, "rule 3", RuleKind.BUG, BALLERINAX_ORG, "exampleModule"),
                            RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, "wso2", "exampleModule"),
                            RuleFactory.createRule(3, "rule 3", RuleKind.BUG),
                            RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, "exampleOrg", "exampleModule"),
                            RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY),
                            RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, BALLERINAX_ORG, "exampleModule"),
                            RuleFactory.createRule(3, "rule 3", RuleKind.BUG, BALLERINA_ORG, "exampleModule"),
                            RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, BALLERINAX_ORG,
                                    "exampleModule"),
                            RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, "wso2", "exampleModule"),
                            RuleFactory.createRule(3, "rule 3", RuleKind.BUG, "exampleOrg", "exampleModule"),
                            RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, "exampleOrg", "exampleModule"),
                            RuleFactory.createRule(3, "rule 3", RuleKind.BUG, "wso2", "exampleModule"),
                            RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, BALLERINA_ORG, "exampleModule"),
                            RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL)
                        )),
                        new String[] {
                                "ballerina:1",
                                "ballerina:2",
                                "ballerina:3",
                                "ballerina/exampleModule:1",
                                "ballerina/exampleModule:2",
                                "ballerina/exampleModule:3",
                                "ballerinax/exampleModule:1",
                                "ballerinax/exampleModule:2",
                                "ballerinax/exampleModule:3",
                                "wso2/exampleModule:1",
                                "wso2/exampleModule:2",
                                "wso2/exampleModule:3",
                                "exampleOrg/exampleModule:1",
                                "exampleOrg/exampleModule:2",
                                "exampleOrg/exampleModule:3"
                        }
                }
        };
    }

    @Test(description = "test scan command with include rules flag")
    void testScanCommandWithIncludeRulesFlag() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-analyzer-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--include-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String result = Files.readString(ballerinaProject.resolve("target").resolve("report")
                        .resolve("scan_results.json"), StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("include-rules-issues-report.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString().replace("\\", "\\\\"));
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command in workspace")
    void testScanCommandInWorkspace() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources").resolve("workspace-project");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--include-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String result = Files.readString(ballerinaProject.resolve("target").resolve("report")
                        .resolve("scan_results.json"), StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("workspace-issues.txt")
                .replace("<ABS_SOURCE_ROOT>", ballerinaProject.toAbsolutePath().toString()
                        .replace("\\", "\\\\"));
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command in workspace with sarif format")
    void testScanCommandInWorkspaceWithSarif() throws IOException {
        Path workspacePath = testResources.resolve("test-resources").resolve("workspace-project");
        System.setProperty("user.dir", workspacePath.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=sarif", "--include-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);

        Path sarifReport = workspacePath.resolve("target").resolve("report").resolve("scan_results.sarif");
        Assert.assertTrue(Files.exists(sarifReport), "SARIF report should be created at workspace level");

        String content = Files.readString(sarifReport, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertTrue(content.contains("\"version\": \"2.1.0\""), "SARIF should have version 2.1.0");
        Assert.assertTrue(
                content.contains("\"bal-project-with-analyzer-configurations/main.bal\""),
                "SARIF URI should be relative to workspace root for first sub-project");
        Assert.assertTrue(
                content.contains("\"bal-project-with-include-rule-configurations/main.bal\""),
                "SARIF URI should be relative to workspace root for second sub-project");
        Assert.assertTrue(content.contains("\"fullDescription\""), "SARIF rule should have a fullDescription");
        Assert.assertTrue(content.contains("\"ruleKind\": \"CODE_SMELL\""),
                "SARIF rule properties should include ruleKind");
        Assert.assertTrue(content.contains("\"tags\""), "SARIF rule properties should include tags");
        Assert.assertTrue(content.contains("\"external/cwe/cwe-248\""), "SARIF rule properties.tags should " +
                "include a CWE tag generated from standards");
        Assert.assertTrue(content.contains("\"external/owasp/owasp-a10-2025\""), "SARIF rule properties.tags " +
                "should include an OWASP tag generated from standards");
        Assert.assertFalse(content.contains("\"enabled\""), "SARIF defaultConfiguration should no longer " +
                "include enabled");
        Assert.assertTrue(content.contains("\"ruleIndex\": 0"), "SARIF result should include ruleIndex");
        Assert.assertTrue(content.contains("\"snippet\""), "SARIF region should include a source snippet");
    }

    @Test(description = "test scan command with exclude rules flag")
    void testScanCommandWithExcludeRulesFlag() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-analyzer-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--exclude-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String result = Files.readString(ballerinaProject.resolve("target").resolve("report")
                        .resolve("scan_results.json"), StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("exclude-rules-issues-report.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString().replace("\\", "\\\\"));
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command with include and exclude rules flag")
    void testScanCommandWithIncludeAndExcludeRulesFlags() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-analyzer-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--include-rules=ballerina:1", "--exclude-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = getExpectedOutput("include-exclude-rules.txt").trim().replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString());
        String actual = readOutput(true).trim();
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "test scan command with include rules Scan.toml configurations")
    void testScanCommandWithIncludeRulesScanTomlConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-include-rule-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String result = Files.readString(ballerinaProject.resolve("target").resolve("report")
                        .resolve("scan_results.json"), StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("toml-include-rules-issues-report.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString().replace("\\", "\\\\"));
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command with exclude rules Scan.toml configurations")
    void testScanCommandWithExcludeRulesScanTomlConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-exclude-rule-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--exclude-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String result = Files.readString(ballerinaProject.resolve("target").resolve("report")
                        .resolve("scan_results.json"), StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("toml-exclude-rules-issues-report.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString().replace("\\", "\\\\"));
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command with include and exclude rules Scan.toml configurations")
    void testScanCommandWithIncludeAndExcludeRulesScanTomlConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-include-exclude-rule-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--include-rules=ballerina:1", "--exclude-rules=ballerina:1"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = getExpectedOutput("toml-include-exclude-rules.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString());
        String actual = readOutput(true).trim();
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "test scan command with platform plugin configurations")
    void testScanCommandWithPlatformPluginConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-platform-configurations");
        Path rootProject = Path.of(System.getProperty("user.dir")).getParent();
        Assert.assertNotNull(rootProject);
        Path platformPluginPath = rootProject
                .resolve("test-static-code-analysis-platform-plugins")
                .resolve("exampleOrg-static-code-analysis-platform-plugin")
                .resolve("build")
                .resolve("libs")
                .resolve("exampleOrg-static-code-analysis-platform-plugin.jar");
        Assert.assertNotNull(platformPluginPath);
        String tomlConfigurations = Files.readString(testResources.resolve("test-resources")
                .resolve("platform-plugin-configurations.txt"));
        tomlConfigurations = tomlConfigurations.replace("__platform_name__", "examplePlatform");
        tomlConfigurations = tomlConfigurations.replace("__platform_plugin_path__",
                platformPluginPath.toString().replace("\\", "\\\\"));
        Files.writeString(ballerinaProject.resolve("Scan.toml"), tomlConfigurations,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);

        Path result = ballerinaProject.resolve("analysis-issues.json");
        String platformIssuesOutput = Files.readString(result, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        removeFile(result);
        result = ballerinaProject.resolve("platform-arguments.json");
        String platformArgumentsOutput = Files.readString(result, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        removeFile(result);

        String expected = getExpectedOutput("platform-plugin-issue-output.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString().replace("\\", "\\\\"));
        Assert.assertEquals(platformIssuesOutput, expected);

        expected = getExpectedOutput("platform-plugin-arguments-output.txt").replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString());
        Assert.assertEquals(platformArgumentsOutput, expected);
    }

    @Test(description = "test scan command with invalid platform plugin configurations")
    void testScanCommandWithInvalidPlatformPluginConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-platform-configurations");
        Path rootProject = Path.of(System.getProperty("user.dir")).getParent();
        Assert.assertNotNull(rootProject);
        Path platformPluginPath = rootProject
                .resolve("test-static-code-analysis-platform-plugins")
                .resolve("exampleOrg-static-code-analysis-platform-plugin")
                .resolve("build")
                .resolve("libs")
                .resolve("exampleOrg-static-code-analysis-platform-plugin.jar");
        String tomlConfigurations = Files.readString(testResources.resolve("test-resources")
                .resolve("platform-plugin-configurations.txt"));
        tomlConfigurations = tomlConfigurations.replace("__platform_name__", "invalidExamplePlatform");
        tomlConfigurations = tomlConfigurations.replace("__platform_plugin_path__",
                platformPluginPath.toString().replace("\\", "\\\\"));
        Files.writeString(ballerinaProject.resolve("Scan.toml"), tomlConfigurations,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = getExpectedOutput("invalid-platform-plugin-configurations.txt")
                .replace("<ABS_SOURCE_ROOT>",
                ballerinaProject.toAbsolutePath().toString());
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test scan command with valid json format flag")
    void testScanCommandWithValidJsonFormatFlag() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=json"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();

        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        Path jsonReport = validBalProject.resolve("target").resolve("report").resolve("scan_results.json");
        Assert.assertTrue(Files.exists(jsonReport), "JSON report file should be created");
    }

    @Test(description = "test scan command with valid ballerina format flag")
    void testScanCommandWithValidBallerinaFormatFlag() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=ballerina"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();

        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        Path jsonReport = validBalProject.resolve("target").resolve("report").resolve("scan_results.json");
        Assert.assertTrue(Files.exists(jsonReport), "JSON report file should be created");
    }

    @Test(description = "test scan command with valid sarif format flag")
    void testScanCommandWithValidSarifFormatFlag() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=sarif"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();

        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        Path sarifReport = validBalProject.resolve("target").resolve("report").resolve("scan_results.sarif");
        Assert.assertTrue(Files.exists(sarifReport), "SARIF report file should be created");
    }

    @Test(description = "test scan command with invalid format flag")
    void testScanCommandWithInvalidFormatFlag() {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=xml"};
        try {
            new CommandLine(scanCmd).parseArgs(args);
            Assert.fail("Expected ParameterException to be thrown for invalid format");
        } catch (CommandLine.ParameterException e) {
            Assert.assertTrue(e.getMessage().contains("xml"), "Error message should mention the invalid format");
            Assert.assertTrue(e.getMessage().contains("Unknown report format"),
                    "Error message should indicate unknown format");
        }
        System.setProperty("user.dir", userDir);
    }

    @Test(description = "test scan command with format flag case insensitive")
    void testScanCommandWithFormatFlagCaseInsensitive() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=BALLERINA"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();

        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        System.setProperty("user.dir", validBalProject.toString());
        scanCmd = new ScanCmd(printStream);
        String[] sarifArgs = {"--format=SARIF"};
        new CommandLine(scanCmd).parseArgs(sarifArgs);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with format flag combined with target-dir")
    void testScanCommandWithFormatFlagAndTargetDir() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=sarif", "--target-dir=custom-results"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();

        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        Path sarifReport = validBalProject.resolve("custom-results").resolve("report")
                .resolve("scan_results.sarif");
        Assert.assertTrue(Files.exists(sarifReport), "SARIF report file should be created in custom directory");
        removeFile(validBalProject.resolve("custom-results"));
    }

    @Test(description = "test scan command with a project containing compilation errors")
    void testScanCommandWithCompilationErrors() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-compilation-errors");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        BLauncherException exception = Assert.expectThrows(BLauncherException.class, scanCmd::execute);
        System.setProperty("user.dir", userDir);
        String output = readOutput(true);
        Assert.assertTrue(output.contains("ERROR [main.bal:(18:17,18:33)] incompatible types: expected 'int', " +
                        "found 'string'"),
                "Compilation errors should be reported in the same format as the Ballerina compiler");
        Assert.assertEquals(exception.getMessages(), List.of("error: " + error(COMPILATION_CONTAINS_ERRORS)),
                "Scan should fail with a launcher exception reporting that compilation contains errors");
        Path jsonReport = ballerinaProject.resolve("target").resolve("report").resolve("scan_results.json");
        Assert.assertFalse(Files.exists(jsonReport),
                "No scan report should be generated when compilation contains errors");
    }

    @Test(description = "test scan command default format behavior")
    void testScanCommandDefaultFormatBehavior() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();

        System.setProperty("user.dir", userDir);
        String expected = "Running Scans";
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        Path jsonReport = validBalProject.resolve("target").resolve("report").resolve("scan_results.json");
        Assert.assertTrue(Files.exists(jsonReport), "JSON report file should be created by default");
    }
}
