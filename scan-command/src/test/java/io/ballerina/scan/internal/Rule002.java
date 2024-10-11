package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.utils.RuleDescription;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unused function parameters analyzer tests.
 *
 * @since 0.1.0
 */
public class Rule002 extends StaticCodeAnalyzerTest {

    @Test(description = "test unused function parameters analyzer")
    void testUnusedFunctionParameterAnalyzer() {
        String documentName = "rule002_unused_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETER.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 17);

        assertIssue(issues.get(0), documentName, 28, 29, 28, 34, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 36, 29, 36, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 40, 29, 40, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 42, 22, 42, 27, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 42, 29, 42, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 44, 22, 44, 27, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 44, 29, 44, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 53, 33, 53, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 57, 33, 57, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 61, 26, 61, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 61, 33, 61, 41, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 72, 19, 72, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 76, 18, 76, 23, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 76, 32, 76, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 77, 22, 77, 28, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 77, 30, 77, 36, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 83, 44, 83, 63, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }

    @Test(description = "test unused anonymous function parameters analyzer")
    void testUnusedAnonymousFunctionParameterAnalyzer() {
        String documentName = "rule002_unused_anonymous_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETER.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation()
                .getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 16);

        assertIssue(issues.get(0), documentName, 16, 34, 16, 39, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 16, 41, 16, 46, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 17, 17, 17, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 25, 26, 25, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 27, 50, 27, 57, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 29, 48, 29, 49, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 31, 61, 31, 66, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 33, 26, 33, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 33, 57, 33, 64, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 39, 26, 39, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 46, 12, 46, 13, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 47, 17, 47, 22, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 53, 28, 53, 33, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 56, 19, 56, 26, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 60, 19, 60, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 66, 46, 66, 47, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETER, RuleKind.CODE_SMELL);
    }
}
