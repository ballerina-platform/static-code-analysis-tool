/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scan.test;

import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import org.testng.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test utility class to assert the issues found during a scan.
 *
 * @since 0.10.0
 */
public class ScanAssertions {

    private ScanAssertions() {
    }

    /**
     * Assert that the list of issues contains an issue with the given rule ID, file name, start line, and end line.
     *
     * @param issues    list of issues from which the issue should be found
     * @param ruleId    rule ID to check for
     * @param fileName  file name to check for
     * @param startLine start line to check for
     * @param endLine   end line to check for
     * @param source    source to check for
     */
    public static void assertHasIssue(List<Issue> issues,
                                      String ruleId,
                                      String fileName,
                                      int startLine,
                                      int endLine,
                                      Source source) {
        boolean found = issues.stream().anyMatch(issue ->
                issue.rule().id().equals(ruleId) &&
                        issue.source().equals(source) &&
                        issue.location().lineRange().fileName().equals(fileName) &&
                        issue.location().lineRange().startLine().line() == startLine &&
                        issue.location().lineRange().endLine().line() == endLine);

        if (!found) {
            String summary = issues.stream()
                    .map(ScanAssertions::formatIssue)
                    .collect(Collectors.joining("\n"));
            Assert.fail(String.format("Expected issue with rule '%s' at %s:(%d:%d) not found.%nFound issues:%n%s",
                    ruleId, fileName, startLine, endLine, summary));
        }
    }

    /**
     * Assert that the list of rules contains a rule with the given ID, description, and kind.
     *
     * @param rules       list of rules from which the rule should be found
     * @param id          rule ID to check for
     * @param description rule description to check for
     * @param kind        rule kind to check for
     */
    public static void assertHasRule(List<Rule> rules, String id, String description, RuleKind kind) {
        boolean found = rules.stream().anyMatch(rule ->
                rule.id().equals(id) &&
                        rule.description().equals(description) &&
                        rule.kind() == kind);

        if (!found) {
            String summary = rules.stream()
                    .map(rule -> String.format("Rule ID: %s, Description: %s, Kind: %s", rule.id(),
                            rule.description(), rule.kind()))
                    .collect(Collectors.joining("\n"));
            Assert.fail(String.format("Expected rule with ID '%s' not found.%nFound rules:%n%s", id, summary));
        }
    }

    private static String formatIssue(Issue issue) {
        return String.format("• [%s] %s:(%d:%d)", issue.rule().id(),
                issue.location().lineRange().fileName(),
                issue.location().lineRange().endLine().line(),
                issue.location().lineRange().startLine().line());
    }
}
