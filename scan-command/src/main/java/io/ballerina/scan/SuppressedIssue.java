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

package io.ballerina.scan;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an issue accepted in the source by a {@code @scan:Suppress} annotation. A suppressed issue is not
 * counted as an active issue, but remains in the scan results together with its justification.
 *
 * @since 0.13.0
 */
public class SuppressedIssue {
    private final Issue issue;
    private final List<String> rules;
    private final String justification;

    public SuppressedIssue(Issue issue, List<String> rules, String justification) {
        this.issue = Objects.requireNonNull(issue, "issue must not be null");
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
        this.justification = justification;
    }

    /**
     * Returns the {@link Issue} that was suppressed.
     *
     * @return the suppressed issue
     */
    public Issue issue() {
        return issue;
    }

    /**
     * Returns the rules named by the suppression that accepted the issue. An empty list means the suppression
     * applies to every rule.
     *
     * @return rule identifiers named by the suppression
     */
    public List<String> rules() {
        return rules;
    }

    /**
     * Returns the reason given for accepting the issue, if any.
     *
     * @return the justification of the suppression
     */
    public Optional<String> justification() {
        return Optional.ofNullable(justification);
    }
}
