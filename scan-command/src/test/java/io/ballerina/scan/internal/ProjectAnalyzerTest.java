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

import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;

/**
 * Project analyzer tests.
 *
 * @since 0.1.0
 */
public class ProjectAnalyzerTest extends BaseTest {
    private final Project project = BuildProject.load(testResources.resolve("test-resources")
            .resolve("bal-project-with-config-file"));
    private ProjectAnalyzer projectAnalyzer = null;

    @BeforeMethod
    void initializeMethod() throws RuntimeException {
        System.setProperty("user.dir", project.sourceRoot().toString());
        Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream);
        System.setProperty("user.dir", userDir);
        if (scanTomlFile.isEmpty()) {
            throw new RuntimeException("Failed to load scan toml file!");
        }
        projectAnalyzer = new ProjectAnalyzer(project, scanTomlFile.get());
    }

    @Test(description = "Test analyzing project with core analyzer")
    void testAnalyzingProjectWithCoreAnalyzer() {
        List<Issue> issues = projectAnalyzer.analyze(List.of(CoreRule.AVOID_CHECKPANIC.rule()));
        Assert.assertEquals(issues.size(), 1);
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), "main.bal");
        Assert.assertEquals(location.startLine().line(), 21);
        Assert.assertEquals(location.startLine().offset(), 18);
        Assert.assertEquals(location.endLine().line(), 21);
        Assert.assertEquals(location.endLine().offset(), 40);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), "ballerina:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "Avoid checkpanic");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "Test analyzing project with external analyzers")
    void testAnalyzingProjectWithExternalAnalyzers() {
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        Assert.assertFalse(externalAnalyzers.isEmpty());
        List<Issue> issues = projectAnalyzer.runExternalAnalyzers(externalAnalyzers);
        Assert.assertEquals(issues.size(), 3);

        assertIssue(issues.get(0), "main.bal", 17, 1, 22, 2, Source.BUILT_IN,
                "ballerina/example_module_static_code_analyzer:1", 1, "rule 1",
                RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), "main.bal", 17, 1, 22, 2, Source.EXTERNAL,
                "exampleOrg/example_module_static_code_analyzer:1", 1, "rule 1",
                RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), "main.bal", 17, 1, 22, 2, Source.BUILT_IN,
                "ballerinax/example_module_static_code_analyzer:1", 1, "rule 1",
                RuleKind.CODE_SMELL);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = null;
        for (DocumentId documentId : defaultModule.documentIds()) {
            document = defaultModule.document(documentId);
            if (!document.name().equals("main.bal")) {
                break;
            }
        }
        Assert.assertNotNull(document);
        String result = document.textDocument().toString().replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertTrue(result.contains("import exampleOrg/example_module_static_code_analyzer as _;"));
        Assert.assertTrue(result.contains("import ballerina/example_module_static_code_analyzer as _;"));
        Assert.assertTrue(result.contains("import ballerinax/example_module_static_code_analyzer as _;"));
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            result = ballerinaToml.tomlDocument().textDocument().toString()
                    .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        }
        Assert.assertTrue(result.contains("""
                [[dependency]]
                org = 'ballerina'
                name = 'example_module_static_code_analyzer'
                version = '0.1.0'
                """));
        Assert.assertTrue(result.contains("""
                [[dependency]]
                org = 'ballerinax'
                name = 'example_module_static_code_analyzer'
                version = '0.1.0'
                repository = 'local'
                """));
    }

    private void assertIssue(Issue issue, String fileName, int startLine, int startOffset, int endLine,
                             int endOffset, Source source, String id, int numericId, String description,
                             RuleKind kind) {
        Assert.assertEquals(issue.source(), source);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), fileName);
        Assert.assertEquals(location.startLine().line(), startLine);
        Assert.assertEquals(location.startLine().offset(), startOffset);
        Assert.assertEquals(location.endLine().line(), endLine);
        Assert.assertEquals(location.endLine().offset(), endOffset);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), id);
        Assert.assertEquals(rule.numericId(), numericId);
        Assert.assertEquals(rule.description(), description);
        Assert.assertEquals(rule.kind(), kind);
    }

    @Test(description = "Test analyzing project with invalid external analyzer rules.json configurations")
    void testAnalyzingProjectWithInvalidExternalAnalyzerRules() throws IOException {
        Project invalidProject = BuildProject.load(testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-external-analyzer-rules"));
        System.setProperty("user.dir", invalidProject.sourceRoot().toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(invalidProject, printStream)
                .orElse(null);
        Assert.assertNotNull(scanTomlFile);
        System.setProperty("user.dir", userDir);
        projectAnalyzer = new ProjectAnalyzer(invalidProject, scanTomlFile);
        Map<String, List<Rule>> externalAnalyzers = null;
        String result = null;
        try {
            externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        } catch (ScanToolException ex) {
            result = ex.getMessage();
        }
        Assert.assertNull(externalAnalyzers);
        String expected = getExpectedOutput("invalid-json-format-for-rules.txt");
        Assert.assertEquals(result, expected);
    }

    @Test(description = "Test analyzing project with invalid external analyzer rule format")
    void testAnalyzingProjectWithInvalidExternalAnalyzerRuleFormat() throws IOException {
        Project invalidProject = BuildProject.load(testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-external-analyzer-rule-format"));
        System.setProperty("user.dir", invalidProject.sourceRoot().toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(invalidProject, printStream)
                .orElse(null);
        Assert.assertNotNull(scanTomlFile);
        System.setProperty("user.dir", userDir);
        projectAnalyzer = new ProjectAnalyzer(invalidProject, scanTomlFile);
        Map<String, List<Rule>> externalAnalyzers = null;
        String result = null;
        try {
            externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        } catch (ScanToolException ex) {
            result = ex.getMessage();
        }
        Assert.assertNull(externalAnalyzers);
        String expected = getExpectedOutput("invalid-json-rule-format.txt");
        Assert.assertEquals(result, expected);
    }

    @Test(description = "Test analyzing project with invalid external analyzer rule kind")
    void testAnalyzingProjectWithInvalidExternalAnalyzerRuleKind() throws IOException {
        Project invalidProject = BuildProject.load(testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-external-analyzer-rule-kind"));
        System.setProperty("user.dir", invalidProject.sourceRoot().toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(invalidProject, printStream)
                .orElse(null);
        Assert.assertNotNull(scanTomlFile);
        System.setProperty("user.dir", userDir);
        projectAnalyzer = new ProjectAnalyzer(invalidProject, scanTomlFile);
        Map<String, List<Rule>> externalAnalyzers = null;
        String result = null;
        try {
            externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        } catch (ScanToolException ex) {
            result = ex.getMessage();
        }
        Assert.assertNull(externalAnalyzers);
        String expected = getExpectedOutput("invalid-json-rule-kind.txt");
        Assert.assertEquals(result, expected);
    }
}
