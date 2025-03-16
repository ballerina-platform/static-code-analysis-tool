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
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Invalid range expression analyzer tests.
 *
 * @since 0.1.0
 */
public class Rule012Test extends StaticCodeAnalyzerTest {
    private static final String INVALID_RANGE_EXPRESSION = "Invalid range expression";

    @Test(description = "test invalid range expresion")
    void testRangeOperatorAnalyzer() {
        String documentName = "rule012_invalid_range_expression.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.INVALID_RANGE_EXPRESSION.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 12);
        assertIssue(issues.get(0), documentName, 25, 21, 25, 27, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 29, 21, 29, 27, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 33, 21, 33, 28, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 57, 21, 57, 27, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 61, 21, 61, 27, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 65, 21, 65, 28, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 69, 21, 69, 26, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 73, 21, 73, 28, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 77, 21, 77, 28, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 81, 22, 81, 29, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 83, 22, 83, 29, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 87, 14, 87, 22, "ballerina:12", 12,
                INVALID_RANGE_EXPRESSION, RuleKind.CODE_SMELL);
    }
}
