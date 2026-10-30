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
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;

/**
 * Tests which values ballerina:13 and ballerina:14 check, by type, by shape and by content.
 *
 * @since 0.13.0
 */
public class SecretValueChecksTest extends StaticCodeAnalyzerTest {
    private static final List<Rule> SECRET_RULES = List.of(HARD_CODED_SECRET.rule(), NON_CONFIGURABLE_SECRET.rule());

    @Test
    void testValueTypes() {
        assertSecretIssues("rule013_value_checks_types.bal", List.of(
                hardCoded(39, 22, 39, 33),
                hardCoded(40, 24, 40, 35),
                hardCoded(41, 32, 41, 43),
                hardCoded(42, 18, 42, 26),
                hardCoded(43, 28, 43, 36),
                hardCoded(44, 24, 44, 35),
                hardCoded(45, 24, 45, 35),
                hardCoded(46, 22, 46, 33),
                hardCoded(47, 24, 47, 35),
                hardCoded(48, 32, 48, 43),
                hardCoded(49, 27, 49, 38),
                hardCoded(50, 30, 50, 41),
                hardCoded(51, 37, 51, 48)));
    }

    @Test
    void testListAndMapMembers() {
        assertSecretIssues("rule013_value_checks_collections.bal", List.of(
                hardCoded(27, 27, 27, 39),
                hardCoded(27, 53, 27, 64),
                hardCoded(28, 26, 28, 34),
                hardCoded(29, 27, 29, 43),
                hardCoded(30, 38, 30, 50),
                hardCoded(31, 25, 31, 39),
                hardCoded(32, 28, 32, 41),
                hardCoded(33, 35, 33, 47),
                hardCoded(33, 81, 33, 92),
                hardCoded(34, 53, 34, 65),
                hardCoded(35, 37, 35, 49),
                hardCoded(36, 39, 36, 52),
                hardCoded(38, 45, 38, 57),
                hardCoded(41, 31, 41, 43)));
    }

    @Test
    void testPlaceholderValuesInEverySlot() {
        assertSecretIssues("rule013_value_checks_placeholders.bal", List.of(
                hardCoded(59, 24, 59, 36),
                hardCoded(60, 27, 60, 34),
                hardCoded(61, 26, 61, 34),
                hardCoded(62, 26, 62, 42),
                hardCoded(63, 28, 63, 41)));
    }

    @Test
    void testUrlsWithCredentials() {
        assertSecretIssues("rule013_value_checks_urls.bal", List.of(
                hardCoded(24, 8, 24, 39),
                hardCoded(25, 8, 25, 58),
                hardCoded(26, 8, 26, 48),
                hardCoded(27, 8, 27, 48),
                hardCoded(28, 8, 28, 45),
                hardCoded(29, 8, 29, 41),
                hardCoded(37, 29, 37, 76)));
    }

    @Test
    void testByteConstantsUrlSchemesAndEscapes() {
        assertSecretIssues("rule013_value_checks_review_fixes.bal", List.of(
                hardCoded(24, 23, 24, 32),
                hardCoded(25, 29, 25, 35),
                hardCoded(26, 22, 26, 61),
                hardCoded(27, 18, 27, 58),
                hardCoded(28, 17, 28, 69),
                hardCoded(29, 18, 29, 63)));
    }

    @Test
    void testSpreadMembersAndNumberTuples() {
        assertSecretIssues("rule013_value_checks_branches.bal", List.of(hardCoded(22, 32, 22, 47)));
    }

    @Test
    void testSecretSlots() {
        assertSecretIssues("rule013_value_checks_slots.bal", List.of(
                hardCoded(23, 16, 23, 32),
                hardCoded(24, 22, 24, 33),
                hardCoded(29, 22, 29, 37),
                hardCoded(30, 23, 30, 34),
                hardCoded(35, 20, 35, 34),
                hardCoded(36, 20, 36, 31),
                hardCoded(46, 26, 46, 42),
                hardCoded(47, 25, 47, 36),
                hardCoded(49, 18, 49, 32),
                hardCoded(50, 18, 50, 29),
                hardCoded(52, 36, 52, 50),
                hardCoded(53, 36, 53, 47),
                hardCoded(56, 52, 56, 66),
                hardCoded(65, 60, 65, 76),
                hardCoded(65, 93, 65, 104)));
    }

    @Test
    void testDirectConfigurableReferences() {
        assertSecretIssues("rule014_value_checks_configurables.bal", List.of());
    }

    @Test
    void testValuesThatCannotHoldSecrets() {
        assertSecretIssues("rule014_value_checks_non_secret_types.bal", List.of());
    }

    // The ballerina:13 and ballerina:14 issues of the document, in source order, must be hard-coded secrets here.
    private static void assertSecretIssues(String documentName, List<Location> expectedLocations) {
        List<Issue> issues = analyze(documentName, SECRET_RULES).stream()
                .filter(issue -> SECRET_RULES.stream().anyMatch(rule -> rule.id().equals(issue.rule().id())))
                .sorted(Comparator.comparingInt((Issue issue) -> issue.location().lineRange().startLine().line())
                        .thenComparingInt(issue -> issue.location().lineRange().startLine().offset()))
                .toList();
        Assert.assertEquals(issues.size(), expectedLocations.size(), describe(issues));
        for (int i = 0; i < issues.size(); i++) {
            assertIssue(issues.get(i), documentName, expectedLocations.get(i), HARD_CODED_SECRET.rule());
        }
    }

    private static String describe(List<Issue> issues) {
        return issues.stream().map(issue -> {
            LineRange range = issue.location().lineRange();
            return issue.rule().id() + "@" + range.startLine().line() + ":" + range.startLine().offset() + "-"
                    + range.endLine().line() + ":" + range.endLine().offset();
        }).toList().toString();
    }

    private static Location hardCoded(int startLine, int startOffset, int endLine, int endOffset) {
        return new Location(startLine, startOffset, endLine, endOffset);
    }
}
