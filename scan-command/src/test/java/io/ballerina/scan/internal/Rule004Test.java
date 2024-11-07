/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;

/**
 * Non-configurable secret analyzer tests.
 *
 * @since 0.1.0
 */
public class Rule004Test extends StaticCodeAnalyzerTest {
    @Test
    void testNonConfigVariableAssignmentToSecret() {
        String documentName = "rule004_non_config_variable_assignment_to_secret.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 6);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(19, 22, 19, 34),
                new Location(20, 20, 20, 32),
                new Location(21, 17, 21, 29),
                new Location(22, 24, 22, 36),
                new Location(23, 20, 23, 32),
                new Location(24, 26, 24, 38)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigVariableAssignmentToDefaultRecordField() {
        String documentName = "rule004_non_config_variable_assignment_to_default_record_field.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(20, 22, 20, 34),
                new Location(25, 26, 25, 38)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigVariableAssignmentToRecordFieldArgument() {
        String documentName = "rule004_non_config_variable_assignment_to_record_field_argument.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 3);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(30, 70, 30, 82),
                new Location(32, 57, 32, 69),
                new Location(33, 89, 33, 101)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigSecretAsFunctionArgument() {
        String documentName = "rules004_non_config_secret_as_function_argument.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 7);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(21, 38, 21, 50),
                new Location(22, 49, 22, 61),
                new Location(25, 36, 25, 48),
                new Location(26, 47, 26, 59),
                new Location(29, 41, 29, 53),
                new Location(46, 37, 46, 49),
                new Location(47, 59, 47, 71)
        ));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigSecretInMapField() {
        String documentName = "rules004_non_config_secret_in_map_field.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 3);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(30, 60, 30, 72),
                new Location(31, 74, 31, 86),
                new Location(32, 56, 32, 68)
        ));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigVariableAssignmentToDefaultParameter() {
        String documentName = "rules004_non_config_variable_assignment_to_default_parameter.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(20, 58, 20, 70),
                new Location(25, 55, 25, 67)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigExpressionAssignmentToVariable() {
        String documentName = "rules004_non_config_expression.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 1);
        List<Location> expectedIssueLocations = new ArrayList<>(
                List.of(new Location(20, 22, 20, 27)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigVariableAssignmentToObjectField() {
        String documentName = "rules004_non_config_variable_assignment_to_object_field.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(19, 26, 19, 38),
                new Location(20, 28, 20, 40)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), NON_CONFIGURABLE_SECRET.rule()));
    }

    @Test
    void testNonConfigValueInShortHandFieldNotation() {
        String documentName = "rules004_non_config_value_in_short_hand_field.bal";
        List<Issue> issues = analyze(documentName, List.of(NON_CONFIGURABLE_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        assertIssue(issues.remove(0), documentName, new Location(16, 18, 16, 28),
                HARD_CODED_SECRET.rule());
        assertIssue(issues.remove(0), documentName, new Location(24, 50, 24, 58),
                NON_CONFIGURABLE_SECRET.rule());
    }
}
