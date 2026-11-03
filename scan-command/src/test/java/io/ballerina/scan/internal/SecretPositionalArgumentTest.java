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

import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;

/**
 * Tests for positional arguments passed to secret-named parameters, which are resolved from the callee's signature.
 *
 * @since 0.13.0
 */
public class SecretPositionalArgumentTest extends StaticCodeAnalyzerTest {
    private static final Rule HARD_CODED = HARD_CODED_SECRET.rule();
    private static final Rule NON_CONFIGURABLE = NON_CONFIGURABLE_SECRET.rule();

    private record ExpectedIssue(Location location, Rule rule) {
    }

    @Test
    void testPositionalArgumentsMappedByParameterKind() {
        String documentName = "rule013_positional_parameter_kinds.bal";
        assertSecretIssues(documentName, List.of(
                hardCoded(26, 23, 26, 40),
                hardCoded(26, 63, 26, 79),
                hardCoded(27, 23, 27, 40),
                hardCoded(28, 37, 28, 57),
                hardCoded(30, 47, 30, 67),
                hardCoded(31, 15, 31, 29),
                hardCoded(31, 31, 31, 46),
                hardCoded(32, 15, 32, 29),
                hardCoded(32, 39, 32, 54),
                hardCoded(33, 24, 33, 36),
                hardCoded(33, 38, 33, 50),
                hardCoded(37, 19, 37, 35),
                hardCoded(38, 13, 38, 30),
                hardCoded(38, 32, 38, 49),
                hardCoded(41, 26, 41, 40)));
    }

    @Test
    void testPositionalArgumentsInCallForms() {
        String documentName = "rule013_positional_call_forms.bal";
        assertSecretIssues(documentName, List.of(
                hardCoded(22, 26, 22, 41),
                hardCoded(25, 31, 25, 46),
                hardCoded(26, 50, 26, 67),
                hardCoded(27, 61, 27, 78),
                hardCoded(28, 35, 28, 51),
                hardCoded(30, 48, 30, 62),
                hardCoded(32, 64, 32, 78),
                hardCoded(36, 25, 36, 40),
                hardCoded(39, 66, 39, 80),
                hardCoded(42, 42, 42, 54),
                hardCoded(43, 57, 43, 69),
                hardCoded(44, 40, 44, 52),
                hardCoded(45, 55, 45, 67),
                hardCoded(52, 34, 52, 53),
                hardCoded(53, 63, 53, 77),
                hardCoded(77, 29, 77, 42)));
    }

    @Test
    void testPositionalArgumentsToLibraryFunctions() {
        String documentName = "rule013_positional_library_calls.bal";
        assertSecretIssues(documentName, List.of(
                hardCoded(24, 44, 24, 59),
                hardCoded(25, 48, 25, 63),
                hardCoded(28, 90, 28, 107),
                hardCoded(31, 48, 31, 64)));
    }

    @Test
    void testCallsThroughFunctionTypedVariablesAndFields() {
        assertSecretIssues("rule013_positional_function_pointers.bal", List.of(
                hardCoded(22, 31, 22, 52),
                hardCoded(28, 22, 28, 38),
                hardCoded(30, 19, 30, 35),
                hardCoded(34, 30, 34, 51)));
    }

    @Test
    void testBranchesReassignmentsAndFunctionTypedParameters() {
        assertSecretIssues("rule013_positional_review_cases.bal", List.of(
                hardCoded(27, 38, 27, 58),
                hardCoded(28, 30, 28, 44),
                nonConfigurable(29, 28, 29, 38),
                hardCoded(29, 41, 29, 63),
                hardCoded(30, 44, 30, 58),
                hardCoded(31, 22, 31, 39),
                hardCoded(32, 15, 32, 34),
                nonConfigurable(39, 21, 39, 26),
                hardCoded(43, 22, 43, 48)));
    }

    @Test
    void testLangLibMethodCallsAreNotReported() {
        assertSecretIssues("rule013_positional_lang_lib_calls.bal", List.of());
    }

    @Test
    void testPositionalArgumentsThatCannotBeSecrets() {
        assertSecretIssues("rule013_positional_non_secret_values.bal", List.of());
    }

    @Test
    void testFixedVariablesAsPositionalArguments() {
        String documentName = "rule014_positional_call_forms.bal";
        assertSecretIssues(documentName, List.of(
                nonConfigurable(27, 21, 27, 31),
                nonConfigurable(28, 21, 28, 31),
                nonConfigurable(29, 21, 29, 32),
                nonConfigurable(30, 24, 30, 34),
                nonConfigurable(30, 36, 30, 46),
                nonConfigurable(34, 31, 34, 41),
                nonConfigurable(35, 50, 35, 60),
                nonConfigurable(36, 42, 36, 52),
                nonConfigurable(37, 47, 37, 57),
                nonConfigurable(39, 48, 39, 58),
                nonConfigurable(42, 25, 42, 35),
                nonConfigurable(45, 44, 45, 54),
                hardCoded(48, 22, 48, 38),
                nonConfigurable(77, 29, 77, 39)));
    }

    private static ExpectedIssue hardCoded(int startLine, int startOffset, int endLine, int endOffset) {
        return new ExpectedIssue(new Location(startLine, startOffset, endLine, endOffset), HARD_CODED);
    }

    private static ExpectedIssue nonConfigurable(int startLine, int startOffset, int endLine, int endOffset) {
        return new ExpectedIssue(new Location(startLine, startOffset, endLine, endOffset), NON_CONFIGURABLE);
    }

    // The analyzer runs every core rule, so only the secret rules are compared.
    private static void assertSecretIssues(String documentName, List<ExpectedIssue> expectedIssues) {
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED, NON_CONFIGURABLE)).stream()
                .filter(issue -> issue.rule().equals(HARD_CODED) || issue.rule().equals(NON_CONFIGURABLE))
                .toList();
        Assert.assertEquals(issues.size(), expectedIssues.size(), "issues: " + issues);
        for (int i = 0; i < issues.size(); i++) {
            ExpectedIssue expected = expectedIssues.get(i);
            assertIssue(issues.get(i), documentName, expected.location(), expected.rule());
        }
    }
}
