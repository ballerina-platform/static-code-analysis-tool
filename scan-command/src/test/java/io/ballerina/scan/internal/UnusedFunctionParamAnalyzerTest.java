package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.utils.RuleDescription;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class UnusedFunctionParamAnalyzerTest extends StaticCodeAnalyzerTest {

    @Test(description = "test unused function parameters analyzer")
    void testUnusedFunctionParameterAnalyzer() {
        String documentName = "unused_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETERS.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation().getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 21);

        assertIssue(issues.get(0), documentName, 28, 29, 28, 34, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 36, 29, 36, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 40, 29, 40, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 42, 22, 42, 27, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 42, 29, 42, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 44, 22, 44, 27, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 44, 29, 44, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 53, 33, 53, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 57, 33, 57, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 61, 26, 61, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 61, 33, 61, 41, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 63, 48, 63, 67, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 80, 48, 80, 61, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 90, 28, 90, 33, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 92, 18, 92, 25, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 96, 19, 96, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 100, 18, 100, 23, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 100, 32, 100, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 101, 22, 101, 28, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 101, 30, 101, 36, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 107, 44, 107, 57, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
    }

    @Test(description = "test unused anonymous function parameters analyzer")
    void testUnusedAnonymousFunctionParameterAnalyzer() {
        String documentName = "unused_anonymous_func_parameters.bal";
        Document document = loadDocument(documentName);
        ScannerContextImpl scannerContext = new ScannerContextImpl(List.of(CoreRule.UNUSED_FUNCTION_PARAMETERS.rule()));
        StaticCodeAnalyzer staticCodeAnalyzer = new StaticCodeAnalyzer(document,
                scannerContext, document.module().getCompilation()
                .getSemanticModel());
        staticCodeAnalyzer.analyze();
        List<Issue> issues = scannerContext.getReporter().getIssues();
        Assert.assertEquals(issues.size(), 13);

        assertIssue(issues.get(0), documentName, 16, 34, 16, 39, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 16, 41, 16, 46, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 17, 17, 17, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 25, 26, 25, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 27, 50, 27, 57, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 29, 48, 29, 49, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 31, 61, 31, 66, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 33, 26, 33, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 33, 57, 33, 64, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 39, 26, 39, 31, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 44, 19, 44, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 50, 12, 50, 13, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 51, 17, 51, 22, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
    }
}
