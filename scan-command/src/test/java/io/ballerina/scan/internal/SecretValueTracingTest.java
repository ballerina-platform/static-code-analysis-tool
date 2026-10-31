/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;

/**
 * Tests how the value given to a secret-named target is traced to its origin for ballerina:13 and ballerina:14.
 *
 * @since 0.13.0
 */
public class SecretValueTracingTest extends StaticCodeAnalyzerTest {
    private static final String VARIABLES = "rule014_tracing_variables.bal";
    private static final String EXPRESSIONS = "rule013_tracing_expressions.bal";
    private static final String FIELDS = "rule014_tracing_fields.bal";
    private static final String SCOPES = "rule014_tracing_scopes.bal";
    private static final String CONVERSIONS = "rule014_tracing_conversions.bal";
    private static final String MULTI_FILE_PROJECT = "bal-project-with-secret-tracing";
    private static final int END_OF_FILE = Integer.MAX_VALUE;

    private final Map<String, List<Issue>> secretIssuesByDocument = new HashMap<>();

    @Test
    void testVariableChainIsFollowedUpToDepthFive() {
        assertSecretIssues(VARIABLES, List.of(lines(0, 35)), fixed(33, 23, 27));
    }

    @Test
    void testFinalAndPublicModuleVariables() {
        assertSecretIssues(VARIABLES, List.of(lines(36, 41)),
                fixed(36, 23, 33), fixed(37, 23, 33), fixed(38, 23, 39), fixed(41, 23, 33));
    }

    @Test
    void testCompoundAssignmentStopsTracing() {
        assertSecretIssues(VARIABLES, List.of(lines(42, 44)));
    }

    @Test
    void testSameVariableTwiceInOneExpression() {
        assertSecretIssues(VARIABLES, List.of(lines(45, 47)), fixed(46, 23, 34));
    }

    @Test
    void testShadowedNamesAreTracedBySymbol() {
        assertSecretIssues(VARIABLES, List.of(lines(48, 80)),
                fixed(48, 23, 36), fixed(68, 23, 35), fixed(79, 23, 31));
    }

    @Test
    void testIsolatedVariablesAssignedInLock() {
        assertSecretIssues(VARIABLES, List.of(lines(81, END_OF_FILE)), fixed(84, 27, 40));
    }

    @Test
    void testConstantsOfConstants() {
        assertSecretIssues(EXPRESSIONS, List.of(lines(0, 28), lines(44, 49)),
                hardCoded(44, 23, 36), hardCoded(46, 23, 38), hardCoded(47, 23, 37), fixed(49, 23, 34));
    }

    @Test
    void testConcatenationMixingLiteralFixedAndConfigurableParts() {
        assertSecretIssues(EXPRESSIONS, List.of(lines(29, 35)),
                hardCoded(29, 23, 51), fixed(30, 23, 45), hardCoded(33, 23, 45), fixed(34, 23, 59));
    }

    @Test
    void testTemplates() {
        assertSecretIssues(EXPRESSIONS, List.of(lines(36, 43)),
                fixed(36, 23, 45), hardCoded(37, 23, 61), fixed(40, 23, 52), hardCoded(41, 23, 56));
    }

    @Test
    void testConversionsAndByteArraysOfHardCodedValues() {
        assertSecretIssues(EXPRESSIONS, List.of(lines(50, END_OF_FILE)),
                hardCoded(50, 26, 52), hardCoded(51, 23, 60), hardCoded(52, 23, 60), hardCoded(53, 23, 40),
                hardCoded(54, 23, 62), hardCoded(55, 26, 51), hardCoded(56, 26, 38), fixed(58, 26, 38),
                fixed(60, 26, 33));
    }

    @Test
    void testConversionsOfFixedValuesAreNotHardCoded() {
        assertSecretIssues(CONVERSIONS, List.of(lines(0, END_OF_FILE)),
                fixed(22, 26, 46), fixed(23, 23, 54), fixed(24, 23, 48), fixed(25, 23, 55), fixed(27, 26, 49));
    }

    @Test
    void testConfigurableRecordsAndDefaults() {
        assertSecretIssues(FIELDS, List.of(lines(0, 48), lines(73, END_OF_FILE)),
                hardCoded(22, 41, 62), hardCoded(76, 23, 44), hardCoded(79, 23, 45));
    }

    @Test
    void testNestedRecordsAndOptionalFieldAccess() {
        assertSecretIssues(FIELDS, List.of(lines(49, 59)),
                fixed(51, 23, 45), fixed(52, 23, 46), fixed(55, 23, 46), fixed(59, 23, 38));
    }

    @Test
    void testMapsAndSecretNamedFieldsAreReportedWhereWritten() {
        assertSecretIssues(FIELDS, List.of(lines(60, 66)),
                fixed(61, 23, 40), fixed(62, 37, 49), fixed(63, 54, 66), fixed(65, 24, 41));
    }

    @Test
    void testConfigurableFieldsAndChainedFieldAccess() {
        assertSecretIssues("rule014_tracing_review_fixes.bal", List.of(lines(0, END_OF_FILE)),
                fixed(40, 23, 44), fixed(42, 23, 43));
    }

    @Test
    void testAssignmentsConcatenationsConversionsAndBranches() {
        assertSecretIssues("rule014_tracing_review_round_two.bal", List.of(lines(0, END_OF_FILE)),
                hardCoded(27, 22, 38), hardCoded(32, 13, 29), hardCoded(37, 23, 69), hardCoded(38, 23, 47),
                fixed(41, 23, 40), hardCoded(43, 15, 32), hardCoded(46, 31, 46), fixed(49, 23, 37),
                fixed(51, 23, 36), hardCoded(53, 36, 51), fixed(55, 23, 35));
    }

    @Test
    void testConstantsPlaceholderPartsAndConversions() {
        assertSecretIssues("rule013_tracing_constants_and_parts.bal", List.of(lines(0, END_OF_FILE)),
                hardCoded(34, 53, 73), hardCoded(44, 23, 45), hardCoded(45, 23, 45), hardCoded(46, 23, 40),
                hardCoded(49, 23, 43), hardCoded(50, 23, 40), hardCoded(51, 23, 44), hardCoded(52, 23, 54),
                hardCoded(53, 23, 38), hardCoded(54, 23, 42), hardCoded(58, 22, 36), hardCoded(59, 16, 34),
                fixed(61, 23, 44));
    }

    @Test
    void testFieldMutationStopsTracing() {
        assertSecretIssues(FIELDS, List.of(lines(67, 72)), fixed(71, 21, 33));
    }

    @Test
    void testClassAndServiceFields() {
        assertSecretIssues(SCOPES, List.of(lines(0, 43)),
                hardCoded(23, 36, 56), fixed(24, 40, 52), fixed(28, 27, 38), hardCoded(36, 34, 56));
    }

    @Test
    void testClosures() {
        assertSecretIssues(SCOPES, List.of(lines(44, 60)), fixed(47, 27, 35), fixed(58, 27, 40));
    }

    @Test
    void testLetExpressionsAreNotTraced() {
        assertSecretIssues(SCOPES, List.of(lines(61, 64)));
    }

    @Test
    void testQueryVariablesAreNotTraced() {
        assertSecretIssues(SCOPES, List.of(lines(65, END_OF_FILE)), fixed(76, 27, 34));
    }

    @Test
    void testVariablesAcrossFilesOfOneModule() {
        Module module = ProjectLoader.load(testResources.resolve("test-resources").resolve(MULTI_FILE_PROJECT))
                .project().currentPackage().getDefaultModule();
        Map<String, List<String>> issuesByDocument = new HashMap<>();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            issuesByDocument.put(document.name(), analyzeDocument(module, document).stream()
                    .filter(SecretValueTracingTest::isSecretIssue)
                    .map(SecretValueTracingTest::format)
                    .toList());
        }
        Assert.assertEquals(issuesByDocument, Map.of(
                "main.bal", List.of(fixed(24, 23, 34), fixed(25, 23, 36), hardCoded(28, 23, 44)),
                "rotation.bal", List.of(fixed(23, 23, 34))));
    }

    private static List<Issue> analyzeDocument(Module module, Document document) {
        ScannerContextImpl scannerContext = new ScannerContextImpl(
                List.of(HARD_CODED_SECRET.rule(), NON_CONFIGURABLE_SECRET.rule()));
        new SensitiveParameterTracker(document.syntaxTree(), scannerContext,
                module.getCompilation().getSemanticModel()).scan();
        new StaticCodeAnalyzer(document, scannerContext, module.getCompilation().getSemanticModel()).analyze();
        return scannerContext.getReporter().getIssues();
    }

    private List<Issue> secretIssues(String documentName) {
        return secretIssuesByDocument.computeIfAbsent(documentName, name ->
                analyze(name, List.of(HARD_CODED_SECRET.rule(), NON_CONFIGURABLE_SECRET.rule())).stream()
                        .filter(SecretValueTracingTest::isSecretIssue)
                        .toList());
    }

    private void assertSecretIssues(String documentName, List<int[]> lineRanges, String... expectedIssues) {
        List<String> actualIssues = secretIssues(documentName).stream()
                .filter(issue -> {
                    Assert.assertEquals(issue.source(), Source.BUILT_IN);
                    Assert.assertEquals(issue.location().lineRange().fileName(), documentName);
                    int line = issue.location().lineRange().startLine().line();
                    return lineRanges.stream().anyMatch(range -> range[0] <= line && line <= range[1]);
                })
                .map(SecretValueTracingTest::format)
                .toList();
        Assert.assertEquals(actualIssues, List.of(expectedIssues));
    }

    private static boolean isSecretIssue(Issue issue) {
        return issue.rule().equals(HARD_CODED_SECRET.rule()) || issue.rule().equals(NON_CONFIGURABLE_SECRET.rule());
    }

    private static String format(Issue issue) {
        LineRange range = issue.location().lineRange();
        return format(issue.rule(), range.startLine().line(), range.startLine().offset(), range.endLine().line(),
                range.endLine().offset());
    }

    private static String format(Rule rule, int startLine, int startOffset, int endLine, int endOffset) {
        return String.format("%s (%d:%d,%d:%d)", rule.id(), startLine, startOffset, endLine, endOffset);
    }

    private static String hardCoded(int line, int startOffset, int endOffset) {
        return format(HARD_CODED_SECRET.rule(), line, startOffset, line, endOffset);
    }

    private static String fixed(int line, int startOffset, int endOffset) {
        return format(NON_CONFIGURABLE_SECRET.rule(), line, startOffset, line, endOffset);
    }

    private static int[] lines(int fromLine, int toLine) {
        return new int[]{fromLine, toLine};
    }
}
