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

/**
 * Hard-coded secret analyzer tests.
 *
 * @since 0.1.0
 */
public class Rule003Test extends StaticCodeAnalyzerTest {
    @Test
    void testHardcodedModuleLevelSecretVariable() {
        String documentName = "rule003_hardcoded_module_level_secret_variable.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 6);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(16, 18, 16, 28),
                new Location(17, 16, 17, 24),
                new Location(18, 13, 18, 18),
                new Location(19, 20, 19, 32),
                new Location(20, 16, 20, 24),
                new Location(21, 22, 21, 36)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedLocalSecretVariable() {
        String documentName = "rule003_hardcoded_local_secret_variable.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 6);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(17, 22, 17, 32),
                new Location(18, 20, 18, 28),
                new Location(19, 17, 19, 22),
                new Location(20, 24, 20, 36),
                new Location(21, 20, 21, 28),
                new Location(22, 26, 22, 40)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedDefaultRecordField() {
        String documentName = "rule003_hardcoded_default_record_field_value.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(18, 22, 18, 32),
                new Location(23, 26, 23, 34)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedSecretRecordFieldInArgument() {
        String documentName = "rule003_hardcoded_secret_record_field_in_argument.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 3);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(28, 70, 28, 78),
                new Location(30, 57, 30, 65),
                new Location(31, 89, 31, 94)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedSecretFunctionArgument() {
        String documentName = "rules003_hardcoded_secret_function_argument.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 7);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(19, 38, 19, 48),
                new Location(20, 49, 20, 57),
                new Location(23, 36, 23, 44),
                new Location(24, 47, 24, 55),
                new Location(27, 41, 27, 51),
                new Location(44, 37, 44, 45),
                new Location(45, 59, 45, 67)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedSecretObjectFields() {
        String documentName = "rules003_hardcoded_secret_object_field.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(17, 26, 17, 42),
                new Location(18, 23, 18, 36)
        ));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedSecretMapField() {
        String documentName = "rules003_hardcode_secret_for_map_field.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 3);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(28, 60, 28, 70),
                new Location(29, 74, 29, 79),
                new Location(30, 56, 30, 66)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedDefaultSecretParameter() {
        String documentName = "rules003_hardcoded_default_secret_parameter.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(18, 58, 18, 75),
                new Location(23, 55, 23, 63)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testUrlWithHardcodedCredentials() {
        String documentName = "rules003_url_with_hardcoded_credentials.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(16, 27, 16, 65),
                new Location(20, 31, 20, 89)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedSecretConstant() {
        String documentName = "rule003_hardcoded_secret_constant.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 2);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(16, 17, 16, 27),
                new Location(17, 16, 17, 51)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }

    @Test
    void testHardcodedConstantAssignment() {
        String documentName = "rule003_hardcoded_constant_assignment.bal";
        List<Issue> issues = analyze(documentName, List.of(HARD_CODED_SECRET.rule()));
        Assert.assertEquals(issues.size(), 8);
        List<Location> expectedIssueLocations = new ArrayList<>(List.of(
                new Location(19, 28, 19, 66),
                new Location(23, 22, 23, 35),
                new Location(31, 70, 31, 83),
                new Location(33, 57, 33, 70),
                new Location(34, 89, 34, 102),
                new Location(35, 56, 35, 69),
                new Location(59, 58, 59, 71),
                new Location(64, 55, 64, 68)));
        issues.forEach(issue -> assertIssue(issue, documentName,
                expectedIssueLocations.remove(0), HARD_CODED_SECRET.rule()));
    }
}
