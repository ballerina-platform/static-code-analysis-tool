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
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

/**
 * Core analyzer tests.
 *
 * @since 0.1.0
 */
public class StaticCodeAnalyzerTest extends BaseTest {
    private final Path coreRuleBalFiles = testResources.resolve("test-resources").resolve("core-rules");

    private Document loadDocument(String documentName) {
        Project project = SingleFileProject.load(coreRuleBalFiles.resolve(documentName));
        Module defaultModule = project.currentPackage().getDefaultModule();
        return defaultModule.document(defaultModule.documentIds().iterator().next());
    }

    @Test(description = "test checkpanic analyzer")
    void testCheckpanicAnalyzer() {
        String documentName = "rule_checkpanic.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.AVOID_CHECKPANIC.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext);
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 1);
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), documentName);
        Assert.assertEquals(location.startLine().line(), 20);
        Assert.assertEquals(location.startLine().offset(), 17);
        Assert.assertEquals(location.endLine().line(), 20);
        Assert.assertEquals(location.endLine().offset(), 39);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), "ballerina:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "Avoid checkpanic");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }
}
