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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

/**
 * Static code analyzer test.
 *
 * @since 0.1.0
 */
public class StaticCodeAnalyzerTest extends BaseTest {
    private final Path coreRuleBalFiles = testResources.resolve("test-resources").resolve("core-rules");

    Document loadDocument(String documentName) {
        Project project = SingleFileProject.load(coreRuleBalFiles.resolve(documentName));
        Module defaultModule = project.currentPackage().getDefaultModule();
        return defaultModule.document(defaultModule.documentIds().iterator().next());
    }

    @Test(description = "test self comparisons")
    void testSelfComparisonAnalyzer() {
        String documentName = "self_comparison_rule.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule(),
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule(),
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule()));

        SemanticModel semanticModel = document.module().getCompilation().getSemanticModel();
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext, semanticModel);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 46);

        assertIssue(issues.get(0), documentName, 9, 10, 9, 16, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 10, 10, 10, 16, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 11, 10, 11, 16, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 12, 10, 12, 16, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 13, 10, 13, 15, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 14, 10, 14, 15, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 15, 10, 15, 17, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 16, 10, 16, 17, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 17, 10, 17, 15, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 18, 10, 18, 15, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 19, 10, 19, 16, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 20, 10, 20, 16, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 31, 10, 31, 20, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 32, 10, 32, 20, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 33, 10, 33, 20, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 34, 10, 34, 20, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 35, 10, 35, 19, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 36, 10, 36, 19, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 37, 10, 37, 21, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 38, 10, 38, 21, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 39, 10, 39, 19, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(21), documentName, 40, 10, 40, 19, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(22), documentName, 41, 10, 41, 20, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(23), documentName, 42, 10, 42, 20, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(24), documentName, 53, 10, 53, 52, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(25), documentName, 54, 10, 54, 52, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(26), documentName, 55, 10, 55, 52, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(27), documentName, 56, 10, 56, 52, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(28), documentName, 57, 10, 57, 51, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(29), documentName, 58, 10, 58, 51, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(30), documentName, 59, 10, 59, 53, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(31), documentName, 60, 10, 60, 53, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(32), documentName, 61, 10, 61, 51, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(33), documentName, 62, 10, 62, 51, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(34), documentName, 72, 10, 72, 26, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(35), documentName, 73, 10, 73, 26, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(36), documentName, 74, 10, 74, 26, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(37), documentName, 75, 10, 75, 26, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(38), documentName, 76, 10, 76, 25, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(39), documentName, 77, 10, 77, 25, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(40), documentName, 78, 10, 78, 27, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(41), documentName, 79, 10, 79, 27, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(42), documentName, 80, 10, 80, 25, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(43), documentName, 81, 10, 81, 25, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(44), documentName, 82, 10, 82, 26, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(45), documentName, 83, 10, 83, 26, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
    }

    @Test(description = "test trivial operations")
    void testTrivialOperationsAnalyzer() {
        String documentName = "trivial_operations.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule(),
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule(),
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule()));

        SemanticModel semanticModel = document.module().getCompilation().getSemanticModel();
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext, semanticModel);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();

        Assert.assertEquals(issues.size(), 14);
        assertIssue(issues.get(0), documentName, 6, 13, 6, 30, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 7, 13, 7, 31, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 9, 13, 9, 31, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 10, 13, 10, 30, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 12, 13, 12, 32, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 13, 13, 13, 31, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 17, 13, 17, 23, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 18, 13, 18, 22, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 19, 13, 19, 22, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 20, 13, 20, 23, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 23, 18, 23, 23, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 24, 14, 24, 20, "ballerina:8", 8,
                "This operation always evaluate to the same value", RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 25, 14, 25, 20, "ballerina:6", 6,
                "This operation always evaluate to true", RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 26, 14, 26, 19, "ballerina:7", 7,
                "This operation always evaluate to false", RuleKind.CODE_SMELL);
    }

    @Test(description = "test self assignment")
    void testSelfAssignmentAnalyzer() {
        String documentName = "rule009_self_assignment.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.SELF_ASSIGNMENT.rule()));
        SemanticModel semanticModel = document.module().getCompilation().getSemanticModel();
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext, semanticModel);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();

        Assert.assertEquals(issues.size(), 33);

        assertIssue(issues.get(0), documentName, 5, 4, 5, 10, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 6, 4, 6, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 7, 4, 7, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 8, 4, 8, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 9, 4, 9, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 10, 4, 10, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 11, 4, 11, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 12, 4, 12, 11, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 13, 4, 13, 12, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 14, 4, 14, 12, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 15, 4, 15, 13, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 28, 4, 28, 14, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 29, 4, 29, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 30, 4, 30, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 31, 4, 31, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 32, 4, 32, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 33, 4, 33, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 34, 4, 34, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 35, 4, 35, 15, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 36, 4, 36, 16, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 37, 4, 37, 16, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(21), documentName, 38, 4, 38, 17, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(22), documentName, 62, 4, 62, 20, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(23), documentName, 63, 4, 63, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(24), documentName, 64, 4, 64, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(25), documentName, 65, 4, 65, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(26), documentName, 66, 4, 66, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(27), documentName, 67, 4, 67, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(28), documentName, 68, 4, 68, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(29), documentName, 69, 4, 69, 21, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(30), documentName, 70, 4, 70, 22, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(31), documentName, 71, 4, 71, 22, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);
        assertIssue(issues.get(32), documentName, 72, 4, 72, 23, "ballerina:9", 9,
                "Self assignment", RuleKind.CODE_SMELL);

    }

    void assertIssue(Issue issue, String documentName, int startLine, int startOffset, int endLine, int endOffset,
                     String ruleId, int numericId, String description, RuleKind ruleKind) {
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), documentName);
        Assert.assertEquals(location.startLine().line(), startLine);
        Assert.assertEquals(location.startLine().offset(), startOffset);
        Assert.assertEquals(location.endLine().line(), endLine);
        Assert.assertEquals(location.endLine().offset(), endOffset);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), ruleId);
        Assert.assertEquals(rule.numericId(), numericId);
        Assert.assertEquals(rule.description(), description);
        Assert.assertEquals(rule.kind(), ruleKind);
    }
}
