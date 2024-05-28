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
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ProjectAnalyzerTest extends BaseTest {
    private ProjectAnalyzer projectAnalyzer;
    private Project project;
    private final String userDir = System.getProperty("user.dir");

    @BeforeTest
    void initialize() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        System.setProperty("user.dir", ballerinaProject.toString());
        project = BuildProject.load(ballerinaProject);
    }

    @BeforeMethod
    void initializeMethod() {
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream);
        projectAnalyzer = new ProjectAnalyzer(scanTomlFile);
    }

    @AfterTest
    void cleanup() {
        System.setProperty("user.dir", userDir);
    }

    @Test(description = "Test analyzing project with core analyzer")
    void testAnalyzingProjectWithCoreAnalyzer() {
        List<Issue> issues = projectAnalyzer.analyze(project, List.of(CoreRule.AVOID_CHECKPANIC.rule()));
        Assert.assertEquals(issues.size(), 1);
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), "main.bal");
        Assert.assertEquals(location.startLine().line(), 20);
        Assert.assertEquals(location.startLine().offset(), 17);
        Assert.assertEquals(location.endLine().line(), 20);
        Assert.assertEquals(location.endLine().offset(), 39);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), "ballerina:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "Avoid checkpanic");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "Test analyzing project with external analyzers")
    void testAnalyzingProjectWithExternalAnalyzers() throws IOException {
        // TODO: Implement mock external analyzer issue assertions.
        List<Issue> issues = projectAnalyzer.runExternalAnalyzers(project);
        Assert.assertEquals(issues.size(), 0);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        String result = document.textDocument().toString();
        Assert.assertTrue(result.contains("import exampleOrg/exampleName as _;"));
        Assert.assertTrue(result.contains("import ballerina/example_module_static_code_analyzer as _;"));
        Assert.assertTrue(result.contains("import ballerinax/example_module_static_code_analyzer as _;"));
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            result = ballerinaToml.tomlDocument().textDocument().toString();
        }
        Assert.assertTrue(result.contains("""
                [[dependency]]
                org='ballerina'
                name='example_module_static_code_analyzer'
                version='0.1.0'
                """));
        Assert.assertTrue(result.contains("""
                [[dependency]]
                org='ballerinax'
                name='example_module_static_code_analyzer'
                version='0.1.0'
                repository='local'
                """));
    }
}