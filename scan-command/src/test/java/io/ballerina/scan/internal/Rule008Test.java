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
 * Always self value evaluates tests tests.
 *
 * @since 0.1.0
 */
public class Rule008Test extends StaticCodeAnalyzerTest {
    public static final String OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE =
            "This operation always evaluate to the same value";

    @Test(description = "test evaluate to the same value")
    void testSelfEvaluates() {
        String documentName = "rule_008_evaluates_same_value.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule()));

        SemanticModel semanticModel = document.module().getCompilation().getSemanticModel();
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext, semanticModel);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 18);

        assertIssue(issues.get(0), documentName, 16, 10, 16, 15, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 17, 10, 17, 15, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 18, 10, 18, 16, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 19, 10, 19, 16, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 20, 10, 20, 19, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 21, 10, 21, 19, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 22, 10, 22, 20, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 23, 10, 23, 20, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 24, 10, 24, 51, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 25, 10, 25, 51, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 26, 10, 26, 25, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 27, 10, 27, 25, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 28, 10, 28, 26, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 29, 10, 29, 26, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 30, 13, 30, 22, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 31, 13, 31, 23, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 32, 18, 32, 23, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 33, 14, 33, 20, "ballerina:8", 8,
                OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE, RuleKind.CODE_SMELL);
    }
}
