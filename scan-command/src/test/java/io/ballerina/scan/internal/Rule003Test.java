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
 * Non isolated public constructs usage tests.
 *
 * @since 0.1.0
 */
public class Rule003Test extends StaticCodeAnalyzerTest {
    public static final String PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT =
            "Non isolated public function or method";
    public static final String PUBLIC_NON_ISOLATED_CLASS_OR_OBJECT_CONSTRUCT =
            "Non isolated public class or object";

    @Test(description = "test non isolated public functions analyzer")
    void testNonIsolatedPublicFunctionOrMethodConstructsUsage() {
        String documentName = "rule003_rule_non_isolated_public_functions_or_methods.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext,
                document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();

        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 6);
        assertIssue(issues.get(0), documentName, 16, 0, 18, 1, "ballerina:3", 3,
                PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 63, 4, 65, 5, "ballerina:3", 3,
                PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 67, 4, 69, 5, "ballerina:3", 3,
                PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 75, 4, 77, 5, "ballerina:3", 3,
                PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 89, 4, 91, 5, "ballerina:3", 3,
                PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 97, 4, 99, 5, "ballerina:3", 3,
                PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT, RuleKind.CODE_SMELL);
    }

    @Test(description = "test non isolated public class or object analyzer")
    void testNonIsolatedPublicClassOrObjectConstructsUsage() {
        String documentName = "rule004_rule_non_isolated_public_classes_or_objects.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.PUBLIC_NON_ISOLATED_FUNCTIONS_OR_METHOD_CONSTRUCT.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext,
                document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();

        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 2);
        assertIssue(issues.get(0), documentName, 20, 0, 22, 1, "ballerina:4", 4,
                PUBLIC_NON_ISOLATED_CLASS_OR_OBJECT_CONSTRUCT, RuleKind.CODE_SMELL);
    }
}
