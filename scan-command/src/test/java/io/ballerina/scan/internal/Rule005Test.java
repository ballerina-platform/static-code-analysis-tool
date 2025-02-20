package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class Rule005Test extends StaticCodeAnalyzerTest {
    private static final String UNUSED_CLASS_PRIVATE_FIELDS = "Unused class private fields";

    @Test(description = "test checkpanic analyzer")
    void testUnusedClassFieldsAnalyzer() {
        String documentName = "rule005_unused_class_fields.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_CLASS_FIELDS.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document, scannerContext,
                document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 5);
        assertIssue(issues.get(0), documentName, 17, 4, 17, 21, "ballerina:5", 5,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 23, 4, 23, 21, "ballerina:5", 5,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 31, 4, 31, 23, "ballerina:5", 5,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 33, 4, 33, 30, "ballerina:5", 5,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 75, 4, 75, 21, "ballerina:5", 5,
                UNUSED_CLASS_PRIVATE_FIELDS, RuleKind.CODE_SMELL);
    }
}
