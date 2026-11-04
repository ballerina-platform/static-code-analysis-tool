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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        String expected = "bal scan: error: " + error(EMPTY_PACKAGE);
        Assert.assertEquals(readOutput(true).trim().split("\n")[0], expected);
        Assert.assertEquals(scanCmd.exitCode(), 1);
    }

    @Test(description = "test scan command with Ballerina project with single file as argument")
    void testScanCommandProjectWithArgument() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {validBalProject.resolve("main.bal").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = "bal scan: error: cannot scan a file inside a package: " + validBalProject.resolve("main.bal")
                + "; scan the package at " + validBalProject.toAbsolutePath().normalize();
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 3);
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
        String[] args = {parentDirectory.resolve("valid-single-file-project").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String expected = "bal scan: error: not a Ballerina package or source file: "
                + parentDirectory.resolve("valid-single-file-project");
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 3);
    }

    @Test(description = "test scan command with single file project without arguments")
    void testScanCommandSingleFileProjectWithoutArgument() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = "bal scan: error: not a Ballerina package or source file: " + validBalProject;
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 3);
    }

    @Test(description = "test scan command with single file project with too many arguments")
    void testScanCommandSingleFileProjectWithTooManyArguments() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"main.bal", "argument2"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = String.join(LINUX_LINE_SEPARATOR,
                "bal scan: error: too many arguments: expected one package or source file, but received 2: "
                        + "main.bal argument2",
                "Usage: bal scan [OPTIONS] [<package>|<source-file>]",
                "Try 'bal scan --help' for more information.");
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 2);
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
        Project project = ProjectLoader.loadProject(validBalProject);
        Path resultsFile = ScanUtils.saveToDirectory(issues, project, null);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("issues-report.txt");
        Assert.assertEquals(result, expected);
    }

    @Test(description =
            "test scan command with method for creating html analysis report when analysis issues are present")
    void testScanCommandGenerateScanReportMethodWhenIssuePresent() throws IOException {
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
        Project project = ProjectLoader.loadProject(validBalProject);
        Path resultsFile = ScanUtils.generateScanReport(issues, project, null);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = getExpectedOutput("issues-html-report.txt");
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for printing static code analysis rules to the console")
    void testPrintRulesToConsole() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        Project project = BuildProject.load(ballerinaProject);
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
        String expected = getExpectedOutput("print-rules-to-console.txt");
        Assert.assertEquals(readOutput(true).trim(), expected);
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
        String expected = getExpectedOutput("list-rules-output.txt");
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
        String expected = getExpectedOutput("include-rules-issues-report.txt");
        Assert.assertEquals(result, expected);
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
        String expected = getExpectedOutput("exclude-rules-issues-report.txt");
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
        String expected = getExpectedOutput("include-exclude-rules.txt");
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 2, "both options on the command line are a usage error");
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
        String expected = getExpectedOutput("toml-include-rules-issues-report.txt");
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
        String expected = getExpectedOutput("toml-exclude-rules-issues-report.txt");
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command with include and exclude rules Scan.toml configurations")
    void testScanCommandWithIncludeAndExcludeRulesScanTomlConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-include-exclude-rule-configurations");
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = getExpectedOutput("toml-include-exclude-rules.txt");
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 1, "both in Scan.toml is a configuration error");
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
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ScanCmd scanCmd = new ScanCmd(printStream, new PrintStream(err, true, StandardCharsets.UTF_8));
        new CommandLine(scanCmd).parseArgs("--platforms=examplePlatform");
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        Assert.assertEquals(scanCmd.exitCode(), 0);

        // The results are saved and summarised before the platform runs
        String stderr = err.toString(StandardCharsets.UTF_8).replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        int resultsAt = stderr.indexOf("View scan results at:");
        int summaryAt = stderr.indexOf(" issue(s)");
        int platformAt = stderr.indexOf("Reporting issues to: examplePlatform");
        Assert.assertTrue(resultsAt >= 0 && resultsAt < summaryAt && summaryAt < platformAt, stderr);

        Path result = ballerinaProject.resolve("analysis-issues.json");
        String platformIssuesOutput = Files.readString(result, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        removeFile(result);
        result = ballerinaProject.resolve("platform-arguments.json");
        String platformArgumentsOutput = Files.readString(result, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        removeFile(result);

        String expected = getExpectedOutput("platform-plugin-issue-output.txt");
        Assert.assertEquals(platformIssuesOutput, expected);

        expected = getExpectedOutput("platform-plugin-arguments-output.txt");
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
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        scanCmd.execute();
        String output = readOutput(true).trim();
        Assert.assertEquals(scanCmd.exitCode(), 0, "a declared platform that is not named is never loaded: " + output);
        Assert.assertFalse(output.contains("invalidExamplePlatform"), output);

        scanCmd = new ScanCmd(printStream, printStream);
        new CommandLine(scanCmd).parseArgs("--platforms=invalidExamplePlatform");
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String expected = getExpectedOutput("invalid-platform-plugin-configurations.txt")
                .replace("<PLUGIN_JAR>", platformPluginPath.toAbsolutePath().toString());
        Assert.assertEquals(readOutput(true).trim(), expected);
        Assert.assertEquals(scanCmd.exitCode(), 1);
    }

    @Test(description = "test a platform-triggered scan reports to the declared platforms when none is named")
    void testPlatformTriggeredScanReportsToDeclaredPlatforms() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-platform-configurations");
        Files.writeString(ballerinaProject.resolve("Scan.toml"), platformConfiguration(""),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        new CommandLine(scanCmd).parseArgs("--platform-triggered");
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String output = readOutput(true);
        Assert.assertEquals(scanCmd.exitCode(), 0, output);
        Assert.assertTrue(output.contains("Reporting issues to: examplePlatform"), output);
        Path result = ballerinaProject.resolve("analysis-issues.json");
        Assert.assertTrue(Files.exists(result), "the declared platform received the issues");
        removeFile(result);
        removeFile(ballerinaProject.resolve("platform-arguments.json"));
    }

    @Test(description = "test a platform named in --platforms must be declared in Scan.toml, checked before the scan")
    void testUndeclaredPlatformFailsBeforeTheScan() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-platform-configurations");
        Files.writeString(ballerinaProject.resolve("Scan.toml"), platformConfiguration(""),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        new CommandLine(scanCmd).parseArgs("--platforms=examplePlatform,unknown");
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String output = readOutput(true);
        Assert.assertEquals(scanCmd.exitCode(), 1, output);
        Assert.assertTrue(output.contains("bal scan: error: platform 'unknown' is not available; add its plugin to "
                + "the [[platform]] table of Scan.toml"), output);
        Assert.assertFalse(output.contains("Running Scans"), output);
        Assert.assertFalse(Files.exists(ballerinaProject.resolve("analysis-issues.json")), output);
    }

    @Test(description = "test a failed platform fails the scan after the results are saved, and wins over the gate")
    void testPlatformFailureFailsTheScan() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-platform-configurations").toAbsolutePath();
        Path ballerinaToml = ballerinaProject.resolve("Ballerina.toml");
        String originalBallerinaToml = Files.readString(ballerinaToml, StandardCharsets.UTF_8);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            Files.writeString(ballerinaProject.resolve("Scan.toml"), platformConfiguration(""),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            // The Scan.toml file is found through configPath, as the working directory points elsewhere
            Files.writeString(ballerinaToml, originalBallerinaToml + System.lineSeparator() + """
                    [scan]
                    configPath = "Scan.toml"
                    """);
            // The test platform writes to the working directory, so a missing one makes it fail
            System.setProperty("user.dir", ballerinaProject.resolve("missing-directory").toString());
            ScanCmd scanCmd = new ScanCmd(printStream, new PrintStream(err, true, StandardCharsets.UTF_8));
            new CommandLine(scanCmd).parseArgs(ballerinaProject.toString(), "--platforms=examplePlatform",
                    "--fail-on-count=1");
            scanCmd.execute();
            Assert.assertEquals(scanCmd.exitCode(), 1);
        } finally {
            System.setProperty("user.dir", userDir);
            Files.writeString(ballerinaToml, originalBallerinaToml);
        }
        String stderr = err.toString(StandardCharsets.UTF_8).replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertTrue(stderr.contains("View scan results at:"), "the results are saved first: " + stderr);
        Assert.assertTrue(stderr.contains("bal scan: error: 1 platform failed to report the issues\n"
                + "error: platform 'examplePlatform' failed: Error occurred while writing platform arguments to file"),
                stderr);
        Assert.assertFalse(stderr.contains("at or above"), "a failed platform wins over the gate: " + stderr);
    }

    @Test(description = "test platform paths are resolved relative to the Scan.toml file that sets them")
    void testPlatformPathsRelativeToScanToml() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-platform-configurations").toAbsolutePath();
        String relativeJarPath = ballerinaProject.relativize(platformPluginPath()).toString().replace("\\", "/");
        try {
            Files.writeString(ballerinaProject.resolve("Scan.toml"), """
                    [[platform]]
                    name = "examplePlatform"
                    path = "__relative_path__"
                    propertiesPath = "Ballerina.toml"
                    """.replace("__relative_path__", relativeJarPath), StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE);
            String output = scanWithPlatform(ballerinaProject);
            Assert.assertTrue(output.contains("Reporting issues to: examplePlatform"), output);
            String platformArguments = Files.readString(ballerinaProject.resolve("platform-arguments.json"),
                    StandardCharsets.UTF_8);
            Assert.assertEquals(JsonParser.parseString(platformArguments).getAsJsonObject().get("propertiesPath")
                    .getAsString(), ballerinaProject.resolve("Ballerina.toml").normalize().toString());

            Files.writeString(ballerinaProject.resolve("Scan.toml"), platformConfiguration("""
                    outputPath = "out/report.json"
                    rulesPath = "https://example.com/rules.json"
                    contextPath = "/api"
                    """), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            output = scanWithPlatform(ballerinaProject);
            Assert.assertTrue(output.contains("Reporting issues to: examplePlatform"), output);
            JsonObject arguments = JsonParser.parseString(Files.readString(
                    ballerinaProject.resolve("platform-arguments.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            Assert.assertEquals(arguments.get("outputPath").getAsString(), "out/report.json");
            Assert.assertEquals(arguments.get("rulesPath").getAsString(), "https://example.com/rules.json");
            Assert.assertEquals(arguments.get("contextPath").getAsString(), "/api");
        } finally {
            removeFile(ballerinaProject.resolve("analysis-issues.json"));
            removeFile(ballerinaProject.resolve("platform-arguments.json"));
        }
    }

    private String scanWithPlatform(Path ballerinaProject) {
        System.setProperty("user.dir", ballerinaProject.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream);
            new CommandLine(scanCmd).parseArgs("--platforms=examplePlatform");
            scanCmd.execute();
            String output = readOutput(true);
            Assert.assertEquals(scanCmd.exitCode(), 0, output);
            return output;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private Path platformPluginPath() {
        Path rootProject = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        Assert.assertNotNull(rootProject);
        return rootProject.resolve("test-static-code-analysis-platform-plugins")
                .resolve("exampleOrg-static-code-analysis-platform-plugin").resolve("build").resolve("libs")
                .resolve("exampleOrg-static-code-analysis-platform-plugin.jar");
    }

    private String platformConfiguration(String extraArgument) throws IOException {
        return Files.readString(testResources.resolve("test-resources").resolve("platform-plugin-configurations.txt"))
                .replace("__platform_name__", "examplePlatform")
                .replace("__platform_plugin_path__", platformPluginPath().toString().replace("\\", "\\\\"))
                + System.lineSeparator() + extraArgument + System.lineSeparator();
    }

    @Test(description = "test scan command with valid json format flag")
    void testScanCommandWithValidJsonFormatFlag() throws IOException {
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
    void testScanCommandWithInvalidFormatFlag() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--format=xml"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        Assert.assertEquals(readOutput(true).trim(), """
                bal scan: error: invalid value 'xml' for --format; expected json or sarif
                Usage: bal scan [OPTIONS] [<package>|<source-file>]
                Try 'bal scan --help' for more information.""");
        Assert.assertEquals(scanCmd.exitCode(), 2);
    }

    @Test(description = "test scan command accepts json, the format named in the help, for the JSON report")
    void testScanCommandWithJsonFormatName() throws IOException {
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        new CommandLine(scanCmd).parseArgs("--format=json");
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String output = readOutput(true);
        Assert.assertEquals(scanCmd.exitCode(), 0, output);
        Assert.assertTrue(Files.exists(validBalProject.resolve("target").resolve("report")
                .resolve("scan_results.json")), output);
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
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String output = readOutput(true);
        Assert.assertTrue(output.contains("bal scan: error: " + error(COMPILATION_CONTAINS_ERRORS) + "\n"
                + "main.bal:18:17: error: incompatible types: expected 'int', found 'string'"), output);
        Assert.assertEquals(scanCmd.exitCode(), 1);
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
