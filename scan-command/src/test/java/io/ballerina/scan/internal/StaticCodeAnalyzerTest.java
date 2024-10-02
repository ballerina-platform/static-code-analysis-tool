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

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.Constants;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

/**
 * Core analyzer tests.
 *
 * @since 0.1.0
 */
public class StaticCodeAnalyzerTest extends BaseTest {
    private final Path coreRuleBalFiles = testResources.resolve("test-resources").resolve("core-rules");

    private Document loadDocument(String documentName) {
        Project project = SingleFileProject.load(coreRuleBalFiles.resolve(documentName));
        Module defaultModule = project.currentPackage().getDefaultModule();
        return defaultModule.document(defaultModule.documentIds().iterator().next());
    }

    @Test(description = "test checkpanic analyzer")
    void testCheckpanicAnalyzer() {
        String documentName = "rule_checkpanic.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.AVOID_CHECKPANIC.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 1);
        assertIssue(issues.get(0), documentName, 20, 17, 20, 39, "ballerina:1", 1,
                Constants.RuleDescription.AVOID_CHECKPANIC, RuleKind.CODE_SMELL);
    }

    @Test(description = "test checkpanic analyzer")
    void testUnusedFunctionParameterAnalyzer() {
        String documentName = "unused_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETERS.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 55);
        assertIssue(issues.get(0), documentName, 8, 29, 8, 34, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 12, 29, 12, 34, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 14, 29, 14, 34, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 14, 36, 14, 41, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 18, 29, 18, 34, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 18, 36, 18, 41, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 24, 29, 24, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 28, 29, 28, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 32, 23, 32, 28, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 32, 30, 32, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 34, 23, 34, 28, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 34, 30, 34, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 45, 33, 45, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 49, 33, 49, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 51, 33, 51, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 51, 40, 51, 45, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 55, 33, 55, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 55, 40, 55, 45, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 61, 33, 61, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 65, 33, 65, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 69, 27, 69, 32, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(21), documentName, 69, 34, 69, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(22), documentName, 71, 27, 71, 32, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(23), documentName, 71, 34, 71, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(24), documentName, 83, 46, 83, 51, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(25), documentName, 87, 47, 87, 52, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(26), documentName, 89, 47, 89, 52, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(27), documentName, 89, 54, 89, 59, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(28), documentName, 93, 47, 93, 52, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(29), documentName, 93, 54, 93, 59, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(30), documentName, 99, 47, 99, 56, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(31), documentName, 103, 47, 103, 56, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(32), documentName, 107, 41, 107, 46, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(33), documentName, 107, 48, 107, 56, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(34), documentName, 109, 41, 109, 46, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(35), documentName, 109, 48, 109, 56, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(36), documentName, 121, 33, 121, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(37), documentName, 125, 33, 125, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(38), documentName, 127, 33, 127, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(39), documentName, 127, 40, 127, 45, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(40), documentName, 131, 33, 131, 38, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(41), documentName, 131, 40, 131, 45, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(42), documentName, 137, 33, 137, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(43), documentName, 141, 33, 141, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(44), documentName, 145, 27, 145, 32, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(45), documentName, 145, 34, 145, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(46), documentName, 147, 27, 147, 32, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(47), documentName, 147, 34, 147, 42, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(48), documentName, 152, 28, 152, 33, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(49), documentName, 154, 18, 154, 31, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(50), documentName, 158, 19, 158, 24, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(51), documentName, 162, 18, 162, 23, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(52), documentName, 162, 32, 162, 37, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(53), documentName, 163, 22, 163, 28, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(54), documentName, 163, 30, 163, 36, "ballerina:4", 4,
                Constants.RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
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
