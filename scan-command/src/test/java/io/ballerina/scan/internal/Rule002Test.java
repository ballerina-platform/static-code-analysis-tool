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
        Assert.assertEquals(issues.size(), 20);

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
        assertIssue(issues.get(17), documentName, 104, 41, 104, 46, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 104, 66, 104, 75, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 104, 95, 104, 103, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }

    @Test(description = "test unused anonymous function parameters analyzer")
    void testUnusedAnonymousFunctionParameter() {
        String documentName = "rule002_unused_anonymous_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETER.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 6);

        assertIssue(issues.get(0), documentName, 19, 27, 19, 32, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 19, 34, 19, 39, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 28, 19, 28, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 36, 19, 36, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 56, 21, 56, 26, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 63, 19, 63, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }

    @Test(description = "test parameters whose signature is fixed by an included type or an expected function type")
    void testParametersFixedByContext() {
        String documentName = "rule002_params_fixed_by_context.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETER.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 14);

        assertIssue(issues.get(0), documentName, 37, 31, 37, 44, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 65, 35, 65, 46, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 79, 28, 79, 37, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 81, 27, 81, 36, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 83, 36, 83, 45, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 125, 50, 125, 55, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 181, 31, 181, 40, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 185, 44, 185, 53, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 189, 59, 189, 64, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 192, 31, 192, 40, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 209, 30, 209, 39, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 211, 34, 211, 43, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 213, 42, 213, 51, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 226, 39, 226, 50, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }
}
