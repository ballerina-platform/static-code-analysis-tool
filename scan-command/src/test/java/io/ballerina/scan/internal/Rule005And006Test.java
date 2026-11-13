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
import io.ballerina.projects.Module;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        Assert.assertEquals(issues.size(), 5);
        assertIssue(issues.get(0), documentName, 20, 0, 20, 6, "ballerina:5", 5,
                PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 37, 0, 37, 6, "ballerina:6", 6,
                PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 57, 0, 57, 6, "ballerina:6", 6,
                PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 74, 0, 74, 6, "ballerina:5", 5,
                PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 93, 0, 93, 6, "ballerina:6", 6,
                PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT, RuleKind.CODE_SMELL);
    }

    @Test(description = "test the public API isolation rules skip test sources")
    void testPublicConstructsInTestSources() {
        Path packagePath = Paths.get("src", "test", "resources", "test-resources",
                "bal-project-with-public-test-constructs");
        Module module = ProjectLoader.loadProject(packagePath).currentPackage().getDefaultModule();
        List<Issue> issues = new ArrayList<>();
        Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream()).forEach(documentId -> {
            Document document = module.document(documentId);
            ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(
                    CoreRule.PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT.rule(),
                    CoreRule.PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT.rule(),
                    CoreRule.PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT.rule(),
                    CoreRule.PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT.rule()));
            new StaticCodeAnalyzer(document, scannerContext, module.getCompilation().getSemanticModel()).analyze();
            issues.addAll(scannerContext.getReporter().getIssues());
        });

        Assert.assertEquals(issues.size(), 2, issues.toString());
        assertIssue(issues.get(0), "main.bal", 16, 0, 16, 6, "ballerina:5", 5,
                PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), "main.bal", 19, 4, 19, 10, "ballerina:4", 4,
                "Non isolated public method", RuleKind.CODE_SMELL);
    }
}
