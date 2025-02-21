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
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Self assignment tests.
 *
 * @since 0.1.0
 */
public class Rule009Test extends StaticCodeAnalyzerTest {
    public static final String SELF_ASSIGNMENT = "Self assignment";

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
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 6, 4, 6, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 7, 4, 7, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 8, 4, 8, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 9, 4, 9, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 10, 4, 10, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 11, 4, 11, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 12, 4, 12, 11, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 13, 4, 13, 12, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 14, 4, 14, 12, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 15, 4, 15, 13, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 28, 4, 28, 14, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 29, 4, 29, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 30, 4, 30, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 31, 4, 31, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 32, 4, 32, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 33, 4, 33, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 34, 4, 34, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 35, 4, 35, 15, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 36, 4, 36, 16, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 37, 4, 37, 16, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(21), documentName, 38, 4, 38, 17, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(22), documentName, 62, 4, 62, 20, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(23), documentName, 63, 4, 63, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(24), documentName, 64, 4, 64, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(25), documentName, 65, 4, 65, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(26), documentName, 66, 4, 66, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(27), documentName, 67, 4, 67, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(28), documentName, 68, 4, 68, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(29), documentName, 69, 4, 69, 21, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(30), documentName, 70, 4, 70, 22, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(31), documentName, 71, 4, 71, 22, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(32), documentName, 72, 4, 72, 23, "ballerina:9", 9,
                SELF_ASSIGNMENT, RuleKind.CODE_SMELL);

    }
}
