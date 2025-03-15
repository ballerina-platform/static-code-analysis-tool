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
 * Always true evaluates tests.
 *
 * @since 0.5.0
 */
public class Rule007Test extends StaticCodeAnalyzerTest {
    public static final String OPERATION_ALWAYS_EVALUATES_TO_TRUE = "This operation always evaluates to true";

    @Test(description = "test always true evaluates")
    void testTrueEvaluates() {
        String documentName = "rule_007_always_true.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE.rule()));

        SemanticModel semanticModel = document.module().getCompilation().getSemanticModel();
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext, semanticModel);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 21);

        assertIssue(issues.get(0), documentName, 15, 10, 15, 16, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 16, 10, 16, 16, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 17, 10, 17, 16, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 18, 10, 18, 17, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 19, 10, 19, 20, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 20, 10, 20, 20, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 21, 10, 21, 20, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 22, 10, 22, 21, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 23, 10, 23, 52, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 24, 10, 24, 52, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 25, 10, 25, 52, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 26, 10, 26, 53, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 27, 10, 27, 26, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 28, 10, 28, 26, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 29, 10, 29, 26, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 30, 10, 30, 27, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 31, 13, 31, 31, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 32, 13, 32, 31, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 33, 13, 33, 32, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 34, 13, 34, 22, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 35, 14, 35, 20, "ballerina:7", 7,
                OPERATION_ALWAYS_EVALUATES_TO_TRUE, RuleKind.CODE_SMELL);
    }
}
