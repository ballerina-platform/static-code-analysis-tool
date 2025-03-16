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
 * Unused private class fields analyzer tests.
 *
 * @since 0.5.0
 */
public class Rule011Test extends StaticCodeAnalyzerTest {
    private static final String UNUSED_CLASS_PRIVATE_FIELDS = "Unused class private fields";

    @Test(description = "test unused private class fields")
    void testUnusedPrivateFieldsAnalyzer() {
        String documentName = "rule011_unused_class_fields.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_PRIVATE_CLASS_FIELD.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext,
                document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 5);
        assertIssue(issues.get(0), documentName, 17, 4, 17, 26, "ballerina:11", 11,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 23, 4, 23, 26, "ballerina:11", 11,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 31, 4, 31, 30, "ballerina:11", 11,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 33, 4, 33, 30, "ballerina:11", 11,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 75, 4, 75, 26, "ballerina:11", 11,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
    }
}
