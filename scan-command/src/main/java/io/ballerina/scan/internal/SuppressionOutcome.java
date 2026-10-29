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
import io.ballerina.scan.SuppressedIssue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of applying {@code @scan:Suppress} annotations to the issues of a package.
 *
 * @param activeIssues     issues that no suppression applies to, in their original order
 * @param suppressedIssues issues accepted by a suppression, in their original order
 * @param diagnostics      diagnostics about the suppressions themselves
 * @since 0.13.0
 */
record SuppressionOutcome(List<Issue> activeIssues, List<SuppressedIssue> suppressedIssues,
                          List<SuppressionDiagnostic> diagnostics) {

    SuppressionOutcome {
        activeIssues = List.copyOf(activeIssues);
        suppressedIssues = List.copyOf(suppressedIssues);
        diagnostics = List.copyOf(diagnostics);
    }

    // A suppression can have several errors, such as a missing reason and an unknown rule; it is counted once
    long invalidSuppressionCount() {
        return diagnostics.stream()
                .filter(SuppressionDiagnostic::isError)
                .map(SuppressionDiagnostic::suppression)
                .distinct()
                .count();
    }

    Map<Issue, SuppressedIssue> suppressionsByIssue() {
        Map<Issue, SuppressedIssue> suppressions = new IdentityHashMap<>();
        suppressedIssues.forEach(suppressed -> suppressions.put(suppressed.issue(), suppressed));
        return Collections.unmodifiableMap(suppressions);
    }
}
