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

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.exceptions.RuleNotFoundException;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static code analysis issue reporter tests.
 *
 * @since 0.1.0
 */
public class ReporterImplTest {
    @Test(description = "test creating a reporter and retrieving issues reported with a numeric rule identifier")
    void testReporterWithNumericId() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        ReporterImpl reporter = new ReporterImpl(Collections.singletonList(rule));
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        Location location = modulePartNode.location();
        LineRange lineRange = location.lineRange();
        TextRange textRange = location.textRange();

        BLangDiagnosticLocation issueLocation = new BLangDiagnosticLocation(lineRange.fileName(),
                lineRange.startLine().line(), lineRange.endLine().line(),
                lineRange.startLine().offset(), lineRange.endLine().offset(),
                textRange.startOffset(), textRange.length());
        reporter.reportIssue(document, issueLocation, 101);
        List<Issue> issues = reporter.getIssues();
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.location(), issueLocation);
        Assert.assertEquals(issue.rule(), rule);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
    }

    @Test(description = "test creating a reporter and retrieving issues reported with a external " +
            "numeric rule identifier")
    void testReporterWithExternalNumericId() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG, "exampleOrg",
                "exampleName");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        ReporterImpl reporter = new ReporterImpl(rules);
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        Location location = modulePartNode.location();
        LineRange lineRange = location.lineRange();
        TextRange textRange = location.textRange();

        BLangDiagnosticLocation issueLocation = new BLangDiagnosticLocation(lineRange.fileName(),
                lineRange.startLine().line(), lineRange.endLine().line(),
                lineRange.startLine().offset(), lineRange.endLine().offset(),
                textRange.startOffset(), textRange.length());
        reporter.reportIssue(document, issueLocation, 101);
        List<Issue> issues = reporter.getIssues();
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.location(), issueLocation);
        Assert.assertEquals(issue.rule(), rule);
        Assert.assertEquals(issue.source(), Source.EXTERNAL);
    }

    @Test(description = "test creating a reporter and reporting an issue with non-existent numeric rule identifier",
    expectedExceptions = RuleNotFoundException.class)
    void testReporterWithNonExistentNumericId() {
        List<Rule> rules = new ArrayList<>();
        ReporterImpl reporter = new ReporterImpl(rules);
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        Location location = modulePartNode.location();
        LineRange lineRange = location.lineRange();
        TextRange textRange = location.textRange();

        BLangDiagnosticLocation issueLocation = new BLangDiagnosticLocation(lineRange.fileName(),
                lineRange.startLine().line(), lineRange.endLine().line(),
                lineRange.startLine().offset(), lineRange.endLine().offset(),
                textRange.startOffset(), textRange.length());
        reporter.reportIssue(document, issueLocation, 101);
    }

    @Test(description = "test creating a reporter and retrieving issues reported with a rule")
    void testReporterWithRule() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        ReporterImpl reporter = new ReporterImpl(rules);
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        ModulePartNode modulePartNode = (ModulePartNode) document.syntaxTree().rootNode();
        Location location = modulePartNode.location();
        LineRange lineRange = location.lineRange();
        TextRange textRange = location.textRange();

        BLangDiagnosticLocation issueLocation = new BLangDiagnosticLocation(lineRange.fileName(),
                lineRange.startLine().line(), lineRange.endLine().line(),
                lineRange.startLine().offset(), lineRange.endLine().offset(),
                textRange.startOffset(), textRange.length());
        reporter.reportIssue(document, issueLocation, rule);
        List<Issue> issues = reporter.getIssues();
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.location(), issueLocation);
        Assert.assertEquals(issue.rule(), rule);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
    }

    @Test(description = "test creating a reporter and retrieving issues reported with an external rule")
    void testReporterWithExternalRule() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG, "exampleOrg",
                "exampleName");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        ReporterImpl reporter = new ReporterImpl(rules);
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        ModulePartNode modulePartNode = (ModulePartNode) document.syntaxTree().rootNode();
        Location location = modulePartNode.location();
        LineRange lineRange = location.lineRange();
        TextRange textRange = location.textRange();

        BLangDiagnosticLocation issueLocation = new BLangDiagnosticLocation(lineRange.fileName(),
                lineRange.startLine().line(), lineRange.endLine().line(),
                lineRange.startLine().offset(), lineRange.endLine().offset(),
                textRange.startOffset(), textRange.length());
        reporter.reportIssue(document, issueLocation, rule);
        List<Issue> issues = reporter.getIssues();
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.location(), issueLocation);
        Assert.assertEquals(issue.rule(), rule);
        Assert.assertEquals(issue.source(), Source.EXTERNAL);
    }
}
