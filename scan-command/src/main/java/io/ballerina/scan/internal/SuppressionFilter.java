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

import io.ballerina.projects.Project;
import io.ballerina.scan.Issue;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the {@code @scan:Suppress} annotations of a package to its issues. Shared by the command line and the
 * language server entry points, so that both suppress the same issues.
 *
 * @since 0.13.0
 */
final class SuppressionFilter {

    private SuppressionFilter() {
    }

    /**
     * Splits the issues into active and suppressed ones and reports diagnostics about the suppressions. The issues
     * are expected to have passed rule selection already, so a suppression naming an excluded rule is unmatched.
     *
     * @param project the project the issues were reported for
     * @param issues  the issues remaining after rule selection
     * @param options how the suppressions are applied
     * @return the active and suppressed issues, and diagnostics about the suppressions
     */
    static SuppressionOutcome apply(Project project, List<Issue> issues, SuppressionOptions options) {
        List<SuppressionAnnotation> suppressions = SuppressionCollector.collect(project);
        if (suppressions.isEmpty()) {
            return new SuppressionOutcome(issues, List.of(), List.of());
        }

        List<SuppressionDiagnostic> diagnostics = new ArrayList<>(requirementViolations(suppressions, options));
        if (options.ignoreSuppressions()) {
            return new SuppressionOutcome(issues, List.of(), diagnostics);
        }

        List<Issue> activeIssues = new ArrayList<>();
        List<SuppressedIssue> suppressedIssues = new ArrayList<>();
        Map<SuppressionAnnotation, Set<String>> matchedRules = new HashMap<>();
        for (Issue issue : issues) {
            SuppressionAnnotation suppression = innermostSuppression(issue, suppressions);
            if (suppression == null) {
                activeIssues.add(issue);
                continue;
            }
            matchedRules.computeIfAbsent(suppression, key -> new HashSet<>()).add(issue.rule().id());
            suppressedIssues.add(new SuppressedIssue(issue, suppression.rules(),
                    suppression.justification().orElse(null)));
        }
        diagnostics.addAll(unmatchedSuppressions(suppressions, matchedRules));
        return new SuppressionOutcome(activeIssues, suppressedIssues, diagnostics);
    }

    private static SuppressionAnnotation innermostSuppression(Issue issue, List<SuppressionAnnotation> suppressions) {
        if (!(issue instanceof IssueImpl issueImpl)) {
            return null;
        }
        SuppressionAnnotation innermost = null;
        for (SuppressionAnnotation suppression : suppressions) {
            if (!suppression.filePath().equals(issueImpl.filePath())
                    || !suppression.appliesTo(issue.rule().id())
                    || !suppression.covers(issue.location().lineRange())) {
                continue;
            }
            if (innermost == null || isPreferred(suppression, innermost)) {
                innermost = suppression;
            }
        }
        return innermost;
    }

    // On the same construct, an annotation naming the rule is preferred over one naming none; ties keep source order.
    private static boolean isPreferred(SuppressionAnnotation candidate, SuppressionAnnotation current) {
        if (candidate.scopeLength() != current.scopeLength()) {
            return candidate.scopeLength() < current.scopeLength();
        }
        return !candidate.rules().isEmpty() && current.rules().isEmpty();
    }

    private static List<SuppressionDiagnostic> requirementViolations(List<SuppressionAnnotation> suppressions,
                                                                     SuppressionOptions options) {
        List<SuppressionDiagnostic> violations = new ArrayList<>();
        for (SuppressionAnnotation suppression : suppressions) {
            if (options.requireReason() && suppression.justification().isEmpty()) {
                violations.add(error(suppression, DiagnosticLog.error(DiagnosticCode.SUPPRESSION_MISSING_REASON)));
            }
            if (options.requireRuleId() && suppression.rules().isEmpty()) {
                violations.add(error(suppression, DiagnosticLog.error(DiagnosticCode.SUPPRESSION_MISSING_RULE_ID)));
            }
        }
        return violations;
    }

    private static List<SuppressionDiagnostic> unmatchedSuppressions(List<SuppressionAnnotation> suppressions,
                                                                     Map<SuppressionAnnotation, Set<String>> matched) {
        List<SuppressionDiagnostic> unmatched = new ArrayList<>();
        for (SuppressionAnnotation suppression : suppressions) {
            Set<String> matchedRules = matched.getOrDefault(suppression, Set.of());
            if (suppression.rules().isEmpty() && matchedRules.isEmpty()) {
                unmatched.add(warning(suppression,
                        DiagnosticLog.warning(DiagnosticCode.UNMATCHED_BLANKET_SUPPRESSION)));
            }
            for (String rule : new LinkedHashSet<>(suppression.rules())) {
                if (!matchedRules.contains(rule)) {
                    unmatched.add(warning(suppression,
                            DiagnosticLog.warning(DiagnosticCode.UNMATCHED_SUPPRESSION, rule)));
                }
            }
        }
        return unmatched;
    }

    private static SuppressionDiagnostic error(SuppressionAnnotation suppression, String message) {
        return new SuppressionDiagnostic(DiagnosticSeverity.ERROR, message, suppression.fileName(),
                suppression.location());
    }

    private static SuppressionDiagnostic warning(SuppressionAnnotation suppression, String message) {
        return new SuppressionDiagnostic(DiagnosticSeverity.WARNING, message, suppression.fileName(),
                suppression.location());
    }
}
