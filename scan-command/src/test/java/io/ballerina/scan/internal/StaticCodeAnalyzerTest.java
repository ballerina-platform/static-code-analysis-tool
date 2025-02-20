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
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;

import java.nio.file.Path;

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

    @Test(description = "test checkpanic analyzer")
    void testRangeOperatorAnalyzer() {
        String documentName = "invalid_range_operator.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.INVALID_RANGE_OPERATOR.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 9);
        assertIssue(issues.get(0), documentName, 9, 21, 9, 27, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 13, 21, 13, 27, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 17, 21, 17, 28, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 41, 21, 41, 27, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 45, 21, 45, 27, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 49, 21, 49, 28, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 53, 21, 53, 26, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 57, 21, 57, 28, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 61, 21, 61, 28, "ballerina:10", 10,
                Constants.RuleDescription.INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
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
