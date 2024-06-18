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

import io.ballerina.cli.utils.OsUtils;
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
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;
import static io.ballerina.scan.internal.ScanToolConstants.BALLERINAX_ORG;
import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_ORG;

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
        if (Files.exists(resultsDirectoryPath)) {
            ProjectUtils.deleteDirectory(resultsDirectoryPath);
        }
    }

    @Test(description = "test scan command override methods")
    void testScanCommandOverrideMethods() throws IOException {
        ScanCmd scanCmd = new ScanCmd();
        String result = scanCmd.getName();
        String expected = "scan";
        Assert.assertEquals(result, expected);

        StringBuilder usageResult = new StringBuilder();
        scanCmd.printUsage(usageResult);
        expected = "Tool providing static code analysis support for Ballerina";
        Assert.assertEquals(usageResult.toString(), expected);

        StringBuilder longDescription = new StringBuilder();
        scanCmd.printLongDesc(longDescription);
        Path helpTextPath = testResources.resolve("command-outputs").resolve("tool-help.txt");
        expected = Files.readString(helpTextPath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR).trim();
        Assert.assertEquals(longDescription.toString()
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR), expected);
    }

    @Test(description = "test scan command with help flag")
    void testScanCommandWithHelpFlag() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--help"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true);
        Path helpTextPath = testResources.resolve("command-outputs").resolve("tool-help.txt");
        String expected = Files.readString(helpTextPath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test scan command with Ballerina project")
    void testScanCommandProject() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        System.setProperty("user.dir", validBalProject.toString());
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String scanLog = readOutput(true);
        String expected = "Running Scans...";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with an empty Ballerina project")
    void testScanCommandEmptyProject() throws IOException {
        Path emptyBalProject = testResources.resolve("test-resources").resolve("empty-bal-project");
        ScanCmd scanCmd = new ScanCmd(printStream);
        System.setProperty("user.dir", emptyBalProject.toString());
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String scanLog = readOutput(true);
        String expected = "ballerina: Package is empty. Please add at least one .bal file.";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with Ballerina project with single file as argument")
    void testScanCommandProjectWithArgument() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {validBalProject.resolve("main.bal").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        System.setProperty("user.dir", validBalProject.toString());
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String scanLog = readOutput(true);
        String expected = "The source file '" + validBalProject.resolve("main.bal") +
                "' belongs to a Ballerina package.";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with single file project with single file as argument")
    void testScanCommandSingleFileProject() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {validBalProject.resolve("main.bal").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true);
        String expected = "Running Scans...";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with single file project with project directory as argument")
    void testScanCommandSingleFileProjectWithDirectoryAsArgument() throws IOException {
        Path parentDirectory = testResources.resolve("test-resources").toAbsolutePath();
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {parentDirectory.resolve("valid-single-file-project").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true).trim();
        String expected = "Invalid Ballerina package directory: " +
                parentDirectory.resolve("valid-single-file-project") + ", cannot find 'Ballerina.toml' file.";
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test scan command with single file project without arguments")
    void testScanCommandSingleFileProjectWithoutArgument() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        ScanCmd scanCmd = new ScanCmd(printStream);
        System.setProperty("user.dir", validBalProject.toString());
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String scanLog = readOutput(true).trim();
        String expected = "Invalid Ballerina package directory: " + validBalProject +
                ", cannot find 'Ballerina.toml' file.";
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test scan command with single file project with too many arguments")
    void testScanCommandSingleFileProjectWithTooManyArguments() throws IOException {
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"main.bal", "argument2"};
        new CommandLine(scanCmd).parseArgs(args);
        System.setProperty("user.dir", validBalProject.toString());
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String scanLog = readOutput(true).trim();
        String expected = "Invalid number of arguments, expected one argument received 2";
        Assert.assertEquals(scanLog, expected);
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
        Path validationResultsFilePath;
        if (OsUtils.isWindows()) {
                validationResultsFilePath = testResources.resolve("command-outputs")
                .resolve("issues-report.txt");
        } else {
                validationResultsFilePath = testResources.resolve("command-outputs")
                .resolve("ubuntu").resolve("issues-report.txt");
        }
        String expected = Files.readString(validationResultsFilePath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
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
        Path validationResultsFilePath;
        if (OsUtils.isWindows()) {
                validationResultsFilePath = testResources.resolve("command-outputs")
                .resolve("issues-html-report.txt");
        } else {
                validationResultsFilePath = testResources.resolve("command-outputs")
                .resolve("ubuntu").resolve("issues-html-report.txt");
        }
        String expected = Files.readString(validationResultsFilePath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for printing static code analysis rules to the console")
    void testPrintRulesToConsole() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNotNull(scanTomlFile);
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(project, scanTomlFile);
        ExternalAnalyzerResult externalAnalyzerResult = projectAnalyzer.getExternalAnalyzers(printStream);
        Assert.assertFalse(externalAnalyzerResult.hasAnalyzerPluginIssue());
        List<Rule> rules = CoreRule.rules();
        externalAnalyzerResult.externalAnalyzers().values().forEach(rules::addAll);
        ScanUtils.printRulesToConsole(rules, printStream);
        Path validationResultsFilePath;
        if (OsUtils.isWindows()) {
            validationResultsFilePath = testResources.resolve("command-outputs")
                    .resolve("print-rules-to-console.txt");
        } else {
            validationResultsFilePath = testResources.resolve("command-outputs")
                    .resolve("ubuntu").resolve("print-rules-to-console.txt");
        }
        String expected = Files.readString(validationResultsFilePath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String result = readOutput(true).trim();
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test scan command with list rules flag")
    void testScanCommandWithListRulesFlag() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--list-rules"};
        new CommandLine(scanCmd).parseArgs(args);
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        System.setProperty("user.dir", ballerinaProject.toString());
        scanCmd.execute();
        System.setProperty("user.dir", userDir);
        String result = readOutput(true);
        Path validationResultsFilePath;
        if (OsUtils.isWindows()) {
            validationResultsFilePath = testResources.resolve("command-outputs")
                    .resolve("list-rules-output.txt");
        } else {
            validationResultsFilePath = testResources.resolve("command-outputs")
                    .resolve("ubuntu").resolve("list-rules-output.txt");
        }
        String expected = Files.readString(validationResultsFilePath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for sorting static code analysis rules in specified order")
    void testSorRules() {
        List<Rule> rules = new ArrayList<>(List.of(
                RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, BALLERINA_ORG, "exampleModule"),
                RuleFactory.createRule(3, "rule 3", RuleKind.BUG, BALLERINAX_ORG, "exampleModule"),
                RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, "wso2", "exampleModule"),
                RuleFactory.createRule(3, "rule 3", RuleKind.BUG),
                RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, "exampleOrg", "exampleModule"),
                RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY),
                RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, BALLERINAX_ORG, "exampleModule"),
                RuleFactory.createRule(3, "rule 3", RuleKind.BUG, BALLERINA_ORG, "exampleModule"),
                RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, BALLERINAX_ORG, "exampleModule"),
                RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL, "wso2", "exampleModule"),
                RuleFactory.createRule(3, "rule 3", RuleKind.BUG, "exampleOrg", "exampleModule"),
                RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, "exampleOrg", "exampleModule"),
                RuleFactory.createRule(3, "rule 3", RuleKind.BUG, "wso2", "exampleModule"),
                RuleFactory.createRule(2, "rule 2", RuleKind.VULNERABILITY, BALLERINA_ORG, "exampleModule"),
                RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL)
        ));
        ScanUtils.sortRules(rules);
        Assert.assertEquals(rules.get(0).id(), "ballerina:1");
        Assert.assertEquals(rules.get(1).id(), "ballerina:2");
        Assert.assertEquals(rules.get(2).id(), "ballerina:3");
        Assert.assertEquals(rules.get(3).id(), "ballerina/exampleModule:1");
        Assert.assertEquals(rules.get(4).id(), "ballerina/exampleModule:2");
        Assert.assertEquals(rules.get(5).id(), "ballerina/exampleModule:3");
        Assert.assertEquals(rules.get(6).id(), "ballerinax/exampleModule:1");
        Assert.assertEquals(rules.get(7).id(), "ballerinax/exampleModule:2");
        Assert.assertEquals(rules.get(8).id(), "ballerinax/exampleModule:3");
        Assert.assertEquals(rules.get(9).id(), "wso2/exampleModule:1");
        Assert.assertEquals(rules.get(10).id(), "wso2/exampleModule:2");
        Assert.assertEquals(rules.get(11).id(), "wso2/exampleModule:3");
        Assert.assertEquals(rules.get(12).id(), "exampleOrg/exampleModule:1");
        Assert.assertEquals(rules.get(13).id(), "exampleOrg/exampleModule:2");
        Assert.assertEquals(rules.get(14).id(), "exampleOrg/exampleModule:3");
    }
}
