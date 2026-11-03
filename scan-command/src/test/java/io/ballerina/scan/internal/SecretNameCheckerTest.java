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
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;

/**
 * Tests that secret names are matched in every slot that ballerina:13 checks, and only there.
 *
 * @since 0.13.0
 */
public class SecretNameCheckerTest extends StaticCodeAnalyzerTest {
    @Test
    void testVariableAndConstantNames() {
        assertHardCodedSecrets("rule013_name_checks_variables.bal", List.of(
                new Location(16, 20, 16, 32),
                new Location(17, 22, 17, 34),
                new Location(18, 17, 18, 29),
                new Location(19, 33, 19, 45),
                new Location(36, 25, 36, 39),
                new Location(37, 26, 37, 40),
                new Location(38, 22, 38, 36),
                new Location(39, 35, 39, 49),
                new Location(40, 23, 40, 44)));
    }

    @Test
    void testRecordAndObjectFieldNames() {
        assertHardCodedSecrets("rule013_name_checks_fields.bal", List.of(
                new Location(18, 22, 18, 38),
                new Location(19, 21, 19, 37),
                new Location(20, 35, 20, 51),
                new Location(21, 20, 21, 43),
                new Location(30, 26, 30, 40),
                new Location(31, 27, 31, 41),
                new Location(32, 23, 32, 44),
                new Location(49, 18, 49, 33),
                new Location(52, 31, 52, 46)));
    }

    @Test
    void testMapKeyNames() {
        assertHardCodedSecrets("rule013_name_checks_map_keys.bal", List.of(
                new Location(23, 21, 23, 33),
                new Location(24, 24, 24, 36),
                new Location(25, 19, 25, 31),
                new Location(26, 25, 26, 37),
                new Location(27, 31, 27, 63),
                new Location(29, 38, 29, 57),
                new Location(30, 16, 30, 28),
                new Location(31, 17, 31, 29)));
    }

    @Test
    void testArgumentAndParameterNames() {
        assertHardCodedSecrets("rule013_name_checks_arguments.bal", List.of(
                new Location(21, 42, 21, 60),
                new Location(22, 27, 22, 52),
                new Location(22, 71, 22, 89),
                new Location(34, 63, 34, 77),
                new Location(36, 21, 36, 42),
                new Location(39, 66, 39, 80)));
    }

    private static void assertHardCodedSecrets(String documentName, List<Location> expectedLocations) {
        List<Issue> secretIssues = analyze(documentName,
                List.of(HARD_CODED_SECRET.rule(), NON_CONFIGURABLE_SECRET.rule())).stream()
                .filter(issue -> issue.rule().numericId() == HARD_CODED_SECRET.rule().numericId()
                        || issue.rule().numericId() == NON_CONFIGURABLE_SECRET.rule().numericId())
                .sorted(Comparator.comparingInt((Issue issue) -> issue.location().lineRange().startLine().line())
                        .thenComparingInt(issue -> issue.location().lineRange().startLine().offset()))
                .toList();
        List<Location> actualLocations = secretIssues.stream().map(SecretNameCheckerTest::toLocation).toList();
        Assert.assertEquals(actualLocations, expectedLocations);
        for (int i = 0; i < secretIssues.size(); i++) {
            assertIssue(secretIssues.get(i), documentName, expectedLocations.get(i), HARD_CODED_SECRET.rule());
        }
    }

    private static Location toLocation(Issue issue) {
        LineRange lineRange = issue.location().lineRange();
        return new Location(lineRange.startLine().line(), lineRange.startLine().offset(),
                lineRange.endLine().line(), lineRange.endLine().offset());
    }
}
