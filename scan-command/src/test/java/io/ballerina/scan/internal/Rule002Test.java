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
public class Rule002Test extends StaticCodeAnalyzerTest {
    public static final String UNUSED_FUNCTION_PARAMETER = "Unused function parameter";

    @Test(description = "test unused function parameters analyzer")
    void testUnusedFunctionParameter() {
        String documentName = "rule002_unused_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETER.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 17);

        boolean externalFunctionParamFlagged = issues.stream()
                .anyMatch(issue -> issue.location().lineRange().startLine().line() == 94);
        Assert.assertFalse(externalFunctionParamFlagged,
                "parameter 'key' of external function 'externHasKey' must not be reported as unused");

        assertIssue(issues.get(0), documentName, 29, 29, 29, 34, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 37, 29, 37, 38, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 41, 29, 41, 38, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 43, 22, 43, 27, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 43, 29, 43, 37, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 45, 22, 45, 27, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 45, 29, 45, 37, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 54, 33, 54, 38, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 58, 33, 58, 42, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 62, 26, 62, 31, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 62, 33, 62, 41, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 73, 19, 73, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 77, 11, 77, 16, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 77, 25, 77, 30, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 78, 22, 78, 28, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 78, 30, 78, 36, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 84, 44, 84, 63, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }

    @Test(description = "test unused anonymous function parameters analyzer")
    void testUnusedAnonymousFunctionParameter() {
        String documentName = "rule002_unused_anonymous_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETER.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation()
                .getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 16);

        assertIssue(issues.get(0), documentName, 16, 27, 16, 32, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 16, 34, 16, 39, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 17, 17, 17, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 25, 19, 25, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 27, 43, 27, 50, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 29, 48, 29, 49, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 31, 61, 31, 66, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 33, 19, 33, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 33, 50, 33, 57, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 39, 26, 39, 31, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 46, 12, 46, 13, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 47, 17, 47, 22, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 53, 21, 53, 26, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 56, 19, 56, 26, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 60, 19, 60, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 66, 46, 66, 47, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }
}
