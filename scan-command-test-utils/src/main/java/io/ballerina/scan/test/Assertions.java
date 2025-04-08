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
public class Assertions {

    private Assertions() {
    }

    /**
     * Assert that the list of issues contains an issue with the given rule ID, file name, start line, end line,
     * and source at the given index.
     *
     * @param issues    list of issues from which the issue should be found
     * @param index     index of the issue to check
     * @param ruleId    rule ID to check for
     * @param fileName  file name to check for
     * @param startLine start line to check for
     * @param endLine   end line to check for
     * @param source    source to check for
     */
    public static void assertIssue(List<Issue> issues,
                                      int index,
                                      String ruleId,
                                      String fileName,
                                      int startLine,
                                      int endLine,
                                      Source source) {
        Assert.assertTrue(index < issues.size(), "Index out of bounds for the issues list");
        Issue issue = issues.get(index);
        Assert.assertEquals(issue.rule().id(), ruleId, "Rule ID mismatch");
        Assert.assertEquals(issue.source(), source, "Source mismatch");
        Assert.assertEquals(issue.location().lineRange().fileName(), fileName, "File name mismatch");
        Assert.assertEquals(issue.location().lineRange().startLine().line(), startLine, "Start line mismatch");
        Assert.assertEquals(issue.location().lineRange().endLine().line(), endLine, "End line mismatch");
    }

    /**
     * Assert that the list of rules contains a rule with the given ID, description, and kind.
     *
     * @param rules       list of rules from which the rule should be found
     * @param id          rule ID to check for
     * @param description rule description to check for
     * @param kind        rule kind to check for
     */
    public static void assertRule(List<Rule> rules, String id, String description, RuleKind kind) {
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
}
