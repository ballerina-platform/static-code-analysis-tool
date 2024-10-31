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

        assertIssue(issues.get(0), documentName, 29, 30, 29, 35, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 37, 30, 37, 39, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 41, 30, 41, 39, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 43, 23, 43, 28, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 43, 30, 43, 38, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 45, 23, 45, 28, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 45, 30, 45, 38, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 54, 34, 54, 39, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 58, 34, 58, 43, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 62, 27, 62, 32, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 62, 34, 62, 42, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 73, 20, 73, 25, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 77, 19, 77, 24, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 77, 33, 77, 38, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 78, 23, 78, 29, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 78, 31, 78, 37, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 84, 45, 84, 64, "ballerina:2", 2,
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

        assertIssue(issues.get(0), documentName, 17, 35, 17, 40, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 17, 42, 17, 47, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 18, 18, 18, 25, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 26, 27, 26, 32, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 28, 51, 28, 58, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 30, 49, 30, 50, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 32, 62, 32, 67, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 34, 27, 34, 32, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 34, 58, 34, 65, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 40, 27, 40, 32, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 47, 13, 47, 14, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 48, 18, 48, 23, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 54, 29, 54, 34, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 57, 20, 57, 27, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 61, 20, 61, 25, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 67, 47, 67, 48, "ballerina:2", 2,
                UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);

    }
}
