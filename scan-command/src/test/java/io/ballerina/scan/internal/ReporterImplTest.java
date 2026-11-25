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
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Test(description = "test reporting an issue with a rule that the scan tool did not create, which the public "
            + "Reporter API accepts")
    void testReporterWithRuleImplementedElsewhere() {
        Rule rule = new Rule() {
            @Override
            public String id() {
                return "exampleOrg/exampleName:7";
            }

            @Override
            public int numericId() {
                return 7;
            }

            @Override
            public String description() {
                return "rule implemented by an analyzer";
            }

            @Override
            public RuleKind kind() {
                return RuleKind.BUG;
            }
        };
        ReporterImpl reporter = new ReporterImpl(List.of());
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        Location location = document.syntaxTree().rootNode().location();

        reporter.reportIssue(document, location, rule);
        Issue issue = reporter.getIssues().get(0);
        Assert.assertEquals(issue.rule().id(), "exampleOrg/exampleName:7");
        Assert.assertEquals(issue.rule().kind(), RuleKind.BUG);
        Assert.assertEquals(issue.rule().description(), "rule implemented by an analyzer");
        Assert.assertEquals(issue.source(), Source.EXTERNAL);
        // The reports write the rule in the same shape as the scan tool's own rules
        String json = ScanUtils.convertIssuesToJsonString(List.of(issue), project, Map.of());
        Assert.assertTrue(json.contains("""
                    "rule": {
                      "id": "exampleOrg/exampleName:7",
                      "numericId": 7,
                      "description": "rule implemented by an analyzer",
                      "ruleKind": "BUG"
                    },
                """), json);
        Assert.assertTrue(ScanUtils.convertIssuesToSarifString(List.of(issue), project)
                .contains("\"ruleId\": \"exampleOrg/exampleName:7\""));
    }

    @Test(description = "test a rule implemented elsewhere without what the reports need fails where it is reported",
            dataProvider = "incomplete-rules")
    void testReporterWithIncompleteRule(String id, RuleKind kind, String description, String error) {
        Rule rule = new Rule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public int numericId() {
                return 7;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public RuleKind kind() {
                return kind;
            }
        };
        ReporterImpl reporter = new ReporterImpl(List.of());
        Document document = mainDocument();

        ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                () -> reporter.reportIssue(document, document.syntaxTree().rootNode().location(), rule));
        Assert.assertEquals(exception.getMessage(), error);
        Assert.assertTrue(reporter.getIssues().isEmpty());
    }

    @DataProvider(name = "incomplete-rules")
    Object[][] incompleteRules() {
        return new Object[][]{
                {null, RuleKind.BUG, "rule", "rule '7' cannot be reported: its ID is null"},
                {"exampleOrg/exampleName:7", null, "rule",
                        "rule 'exampleOrg/exampleName:7' cannot be reported: its kind is null"},
                {"exampleOrg/exampleName:7", RuleKind.BUG, null,
                        "rule 'exampleOrg/exampleName:7' cannot be reported: its description is null"},
        };
    }

    @Test(description = "test a report the wrapped reporter rejects is not recorded for the scan")
    void testRecordingReporterWithRejectingDelegate() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        Reporter rejecting = new Reporter() {
            @Override
            public void reportIssue(Document reportedDocument, Location location, int ruleId) {
                throw new IllegalStateException("rejected " + ruleId);
            }

            @Override
            public void reportIssue(Document reportedDocument, Location location, Rule reportedRule) {
                throw new IllegalStateException("rejected " + reportedRule.id());
            }
        };
        RecordingScannerContext context = new RecordingScannerContext(new ScannerContext() {
            @Override
            public Reporter getReporter() {
                return rejecting;
            }
        }, List.of(rule));
        Document document = mainDocument();
        Location location = document.syntaxTree().rootNode().location();

        Assert.expectThrows(IllegalStateException.class, () -> context.getReporter().reportIssue(document, location,
                101));
        Assert.expectThrows(IllegalStateException.class, () -> context.getReporter().reportIssue(document, location,
                rule));
        Assert.assertTrue(context.issues().isEmpty());
    }

    @Test(description = "test the scan tool's own context records each report once")
    void testRecordingScannerContextOfScanTool() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        ScannerContextImpl ownContext = new ScannerContextImpl(List.of(rule), false);
        RecordingScannerContext context = new RecordingScannerContext(ownContext, List.of(rule));
        Document document = mainDocument();

        context.getReporter().reportIssue(document, document.syntaxTree().rootNode().location(), 101);

        Assert.assertEquals(context.issues().size(), 1);
        Assert.assertTrue(ownContext.getReporter().getIssues().isEmpty());
    }

    private static Document mainDocument() {
        Path validBalProject = Paths.get("src", "test", "resources", "test-resources",
                "valid-single-file-project", "main.bal");
        Project project = SingleFileProject.load(validBalProject);
        Module defaultModule = project.currentPackage().getDefaultModule();
        return defaultModule.document(defaultModule.documentIds().iterator().next());
    }

    @Test(description = "test creating a reporter and reporting an issue with non-existent numeric rule identifier")
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
        try {
            reporter.reportIssue(document, issueLocation, 101);
        } catch (RuntimeException ex) {
            Assert.assertEquals(ex.getMessage(), DiagnosticLog.error(DiagnosticCode.RULE_NOT_FOUND, 101));
        }
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
