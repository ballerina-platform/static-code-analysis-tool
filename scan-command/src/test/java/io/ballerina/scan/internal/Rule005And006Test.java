/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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
public class Rule005And006Test extends StaticCodeAnalyzerTest {
    public static final String PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT =
            "Non isolated public object";
    public static final String PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT =
            "Non isolated public class";

    @Test(description = "test non isolated public class or object analyzer")
    void testNonIsolatedPublicClassOrObjectConstructsUsage() {
        String documentName = "rule005_and_006_rule_non_isolated_public_classes_or_objects.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                CoreRule.PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT.rule(),
                CoreRule.PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext,
                document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();

        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 4);
        assertIssue(issues.get(0), documentName, 20, 0, 22, 1, "ballerina:5", 5,
                PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 37, 0, 40, 2, "ballerina:6", 6,
                PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 57, 0, 60, 2, "ballerina:6", 6,
                PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 74, 0, 76, 1, "ballerina:5", 5,
                PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT, RuleKind.CODE_SMELL);
    }
}
