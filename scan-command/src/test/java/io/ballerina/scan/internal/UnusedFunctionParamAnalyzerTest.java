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
        Assert.assertEquals(issues.size(), 59);

        assertIssue(issues.get(0), documentName, 24, 29, 24, 34, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(1), documentName, 28, 29, 28, 34, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(2), documentName, 30, 29, 30, 34, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(3), documentName, 30, 36, 30, 41, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(4), documentName, 34, 29, 34, 34, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(5), documentName, 34, 36, 34, 41, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(6), documentName, 40, 29, 40, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(7), documentName, 44, 29, 44, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(8), documentName, 48, 23, 48, 28, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(9), documentName, 48, 30, 48, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(10), documentName, 50, 23, 50, 28, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(11), documentName, 50, 30, 50, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(12), documentName, 61, 33, 61, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(13), documentName, 65, 33, 65, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(14), documentName, 67, 33, 67, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(15), documentName, 67, 40, 67, 45, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(16), documentName, 71, 33, 71, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(17), documentName, 71, 40, 71, 45, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(18), documentName, 77, 33, 77, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(19), documentName, 81, 33, 81, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(20), documentName, 85, 27, 85, 32, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(21), documentName, 85, 34, 85, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(22), documentName, 87, 27, 87, 32, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(23), documentName, 87, 34, 87, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(24), documentName, 91, 48, 91, 61, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(25), documentName, 108, 46, 108, 51, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(26), documentName, 112, 47, 112, 52, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(27), documentName, 114, 47, 114, 52, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(28), documentName, 114, 54, 114, 59, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(29), documentName, 118, 47, 118, 52, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(30), documentName, 118, 54, 118, 59, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(31), documentName, 124, 47, 124, 56, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(32), documentName, 128, 47, 128, 56, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(33), documentName, 132, 41, 132, 46, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(34), documentName, 132, 48, 132, 56, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(35), documentName, 134, 41, 134, 46, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(36), documentName, 134, 48, 134, 56, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(37), documentName, 138, 48, 138, 61, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(38), documentName, 155, 33, 155, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(39), documentName, 159, 33, 159, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(40), documentName, 161, 33, 161, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(41), documentName, 161, 40, 161, 45, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(42), documentName, 165, 33, 165, 38, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(43), documentName, 165, 40, 165, 45, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(44), documentName, 171, 33, 171, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(45), documentName, 175, 33, 175, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(46), documentName, 179, 27, 179, 32, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(47), documentName, 179, 34, 179, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(48), documentName, 181, 27, 181, 32, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(49), documentName, 181, 34, 181, 42, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(50), documentName, 185, 48, 185, 61, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(51), documentName, 195, 28, 195, 33, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(52), documentName, 197, 18, 197, 25, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(53), documentName, 201, 19, 201, 24, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(54), documentName, 205, 18, 205, 23, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(55), documentName, 205, 32, 205, 37, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(56), documentName, 206, 22, 206, 28, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(57), documentName, 206, 30, 206, 36, "ballerina:2", 2,
                RuleDescription.UNUSED_FUNCTION_PARAMETERS, RuleKind.CODE_SMELL);
        assertIssue(issues.get(58), documentName, 212, 44, 212, 57, "ballerina:2", 2,
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
