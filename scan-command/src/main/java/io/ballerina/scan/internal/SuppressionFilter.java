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
import io.ballerina.scan.Rule;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Applies the {@code @scan:Suppress} annotations of a package to its issues. Shared by the command line and the
 * language server entry points, so that both suppress the same issues.
 *
 * @since 0.11.1
 */
final class SuppressionFilter {

    private static final Pattern RULE_ID = Pattern.compile("(ballerina|[A-Za-z0-9_.]+/[A-Za-z0-9_.]+):[1-9][0-9]*");

    private SuppressionFilter() {
    }

    /**
     * Splits the issues into active and suppressed ones and reports diagnostics about the suppressions. The issues
     * are expected to have passed rule selection already. A suppression is not reported as unmatched for a rule the
     * selection leaves out, since whether it is still needed cannot be told from this scan.
     *
     * @param project   the project the issues were reported for
     * @param issues    the issues remaining after rule selection
     * @param rules     every rule the scan can report, before rule selection
     * @param selection the rules the scan reports
     * @param options   how the suppressions are applied
     * @return the active and suppressed issues, and diagnostics about the suppressions
     */
    static SuppressionOutcome apply(Project project, List<Issue> issues, Collection<Rule> rules,
                                    RuleSelection selection, SuppressionOptions options) {
        if (options.ignoreSuppressions()) {
            return new SuppressionOutcome(issues, List.of(), List.of());
        }
        List<SuppressionAnnotation> suppressions = SuppressionCollector.collect(project);
        if (suppressions.isEmpty()) {
            return new SuppressionOutcome(issues, List.of(), List.of());
        }

        KnownRules knownRules = new KnownRules(rules);
        List<SuppressionDiagnostic> diagnostics = new ArrayList<>(unknownRules(suppressions, knownRules));
        diagnostics.addAll(requirementViolations(suppressions, options));

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
        diagnostics.addAll(unusedSuppressions(suppressions, matchedRules, knownRules, selection));
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
        return !candidate.blanket() && current.blanket();
    }

    // The ballerina/scan compiler plugin checks only the format of a rule ID; whether it exists is known only here.
    private static List<SuppressionDiagnostic> unknownRules(List<SuppressionAnnotation> suppressions,
                                                            KnownRules knownRules) {
        List<SuppressionDiagnostic> unknown = new ArrayList<>();
        for (SuppressionAnnotation suppression : suppressions) {
            for (String rule : new LinkedHashSet<>(suppression.rules())) {
                Matcher matcher = RULE_ID.matcher(rule);
                if (!matcher.matches() || knownRules.ids().contains(rule)) {
                    continue;
                }
                String provider = matcher.group(1);
                // Rules of a provider that is not engaged cannot be verified, so they do not fail the scan
                unknown.add(knownRules.providers().contains(provider)
                        ? error(suppression, DiagnosticLog.error(DiagnosticCode.UNKNOWN_RULE, rule, provider))
                        : warning(suppression,
                                DiagnosticLog.warning(DiagnosticCode.UNKNOWN_RULE_PROVIDER, rule, provider)));
            }
        }
        return unknown;
    }

    private static List<SuppressionDiagnostic> requirementViolations(List<SuppressionAnnotation> suppressions,
                                                                     SuppressionOptions options) {
        List<SuppressionDiagnostic> violations = new ArrayList<>();
        for (SuppressionAnnotation suppression : suppressions) {
            if (options.requireReason() && suppression.justification().isEmpty()) {
                violations.add(error(suppression, DiagnosticLog.error(DiagnosticCode.SUPPRESSION_MISSING_REASON)));
            }
            if (options.requireRuleId() && suppression.blanket()) {
                violations.add(error(suppression, DiagnosticLog.error(DiagnosticCode.SUPPRESSION_MISSING_RULE_ID)));
            }
        }
        return violations;
    }

    private static List<SuppressionDiagnostic> unusedSuppressions(List<SuppressionAnnotation> suppressions,
                                                                  Map<SuppressionAnnotation, Set<String>> matched,
                                                                  KnownRules knownRules, RuleSelection selection) {
        List<SuppressionDiagnostic> unused = new ArrayList<>();
        Map<String, Set<String>> rulesOnConstruct = new HashMap<>();
        for (SuppressionAnnotation suppression : suppressions) {
            // An earlier annotation on the same construct wins the tie for its rules, so this one never matches them
            Set<String> earlierRules = rulesOnConstruct.computeIfAbsent(
                    suppression.filePath() + suppression.scope(), construct -> new HashSet<>());
            Set<String> duplicateRules = new LinkedHashSet<>(suppression.rules());
            duplicateRules.retainAll(earlierRules);
            earlierRules.addAll(suppression.rules());
            duplicateRules.forEach(rule -> unused.add(warning(suppression,
                    DiagnosticLog.warning(DiagnosticCode.DUPLICATE_SUPPRESSION_RULE, rule))));

            Set<String> matchedRules = matched.getOrDefault(suppression, Set.of());
            // A blanket suppression may cover issues of the rules left out, so it is judged only on a full scan
            if (suppression.blanket() && matchedRules.isEmpty() && !selection.isPartial()) {
                unused.add(warning(suppression,
                        DiagnosticLog.warning(DiagnosticCode.UNMATCHED_BLANKET_SUPPRESSION)));
            }
            for (String rule : suppression.unresolvedRules()) {
                unused.add(warning(suppression,
                        DiagnosticLog.warning(DiagnosticCode.UNRESOLVED_SUPPRESSION_RULE, rule)));
            }
            for (String rule : new LinkedHashSet<>(suppression.rules())) {
                if (!matchedRules.contains(rule) && !duplicateRules.contains(rule) && knownRules.ids().contains(rule)
                        && selection.selects(rule)) {
                    unused.add(warning(suppression,
                            DiagnosticLog.warning(DiagnosticCode.UNMATCHED_SUPPRESSION, rule)));
                }
            }
        }
        return unused;
    }

    private static SuppressionDiagnostic error(SuppressionAnnotation suppression, String message) {
        return new SuppressionDiagnostic(DiagnosticSeverity.ERROR, message, suppression.fileName(),
                suppression.location());
    }

    private static SuppressionDiagnostic warning(SuppressionAnnotation suppression, String message) {
        return new SuppressionDiagnostic(DiagnosticSeverity.WARNING, message, suppression.fileName(),
                suppression.location());
    }

    private record KnownRules(Set<String> ids, Set<String> providers) {

        private KnownRules(Collection<Rule> rules) {
            this(rules.stream().map(Rule::id).collect(Collectors.toUnmodifiableSet()),
                    rules.stream().map(rule -> rule.id().substring(0, rule.id().lastIndexOf(':')))
                            .collect(Collectors.toUnmodifiableSet()));
        }
    }
}
