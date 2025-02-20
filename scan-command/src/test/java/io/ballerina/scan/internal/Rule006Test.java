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
 * Unused function parameters analyzer tests.
 *
 * @since 0.1.0
 */
public class Rule006Test extends StaticCodeAnalyzerTest {
    private static final String INVALID_RANGE_OPERATOR = "Invalid range operator";

    @Test(description = "test invalid range operator")
    void testRangeOperatorAnalyzer() {
        String documentName = "rule006_invalid_range_expression.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.INVALID_RANGE_OPERATOR.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 9);
        assertIssue(issues.get(0), documentName, 9, 21, 9, 27, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 13, 21, 13, 27, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 17, 21, 17, 28, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 41, 21, 41, 27, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 45, 21, 45, 27, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 49, 21, 49, 28, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 53, 21, 53, 26, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 57, 21, 57, 28, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 61, 21, 61, 28, "ballerina:6", 10,
                INVALID_RANGE_OPERATOR, RuleKind.CODE_SMELL);
    }
}
