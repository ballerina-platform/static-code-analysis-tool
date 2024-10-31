package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.utils.RuleDescription;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class NonIsolatedPublicConstructAnalyzerTest extends StaticCodeAnalyzerTest {

    @Test(description = "test non isolated public functions analyzer")
    void testNonIsolatedConstructsAnalyzer() {
        String documentName = "rule_non_isolated_public_functions.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List
                .of(CoreRule.PUBLIC_NON_ISOLATED_CONSTRUCT.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext,
                document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();

        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 7);
        assertIssue(issues.get(0), documentName, 16, 0, 18, 1, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 58, 0, 78, 1, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 63, 4, 65, 5, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 67, 4, 69, 5, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 75, 4, 77, 5, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 89, 4, 91, 5, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 97, 4, 99, 5, "ballerina:3", 3,
                RuleDescription.PUBLIC_NON_ISOLATED_CONSTRUCT, RuleKind.CODE_SMELL);
    }
}
