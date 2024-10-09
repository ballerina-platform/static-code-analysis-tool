package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.utils.RuleDescription;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class CheckPanicAnalyzerTest extends StaticCodeAnalyzerTest {

    @Test(description = "test checkpanic analyzer")
    void testCheckpanicAnalyzer() {
        String documentName = "rule_checkpanic.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.AVOID_CHECKPANIC.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 1);

        assertIssue(issues.get(0), documentName, 20, 17, 20, 39, "ballerina:1", 1,
                RuleDescription.AVOID_CHECKPANIC, RuleKind.CODE_SMELL);
    }
}
