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
import io.ballerina.scan.Severity;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The gate that fails a scan when the number of issues at or above a severity reaches a count. The gate only decides
 * the exit code; reports always hold every issue.
 *
 * @since 0.13.0
 */
final class QualityGate {
    static final QualityGate OFF = new QualityGate(null, 0);

    private final Severity threshold;
    private final int limit;

    private QualityGate(Severity threshold, int limit) {
        this.threshold = threshold;
        this.limit = limit;
    }

    /**
     * Returns the gate for the resolved settings. A count without a severity counts every severity, and a severity
     * without a count fails on the first issue.
     *
     * @param severity a severity name, {@code NONE}, or {@code null} when not set
     * @param count    a count of 1 or more, or {@code null} when not set
     * @return the gate, which is {@link #OFF} when neither is set or the severity is {@code NONE}
     */
    static QualityGate of(String severity, Integer count) {
        if (ScanUtils.GATE_SEVERITY_NONE.equals(severity) || (severity == null && count == null)) {
            return OFF;
        }
        return new QualityGate(severity == null ? Severity.INFO : Severity.valueOf(severity),
                count == null ? 1 : count);
    }

    boolean isOff() {
        return threshold == null;
    }

    /**
     * Counts the issues at or above the threshold.
     *
     * @param issues the issues left after rule selection and suppressions
     * @return the gate failure message, or empty when the gate is off or not tripped
     */
    Optional<String> evaluate(Collection<Issue> issues) {
        if (isOff()) {
            return Optional.empty();
        }
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Issue issue : issues) {
            Severity severity = ScanUtils.effectiveSeverity(issue.rule());
            if (severity.compareTo(threshold) <= 0) {
                counts.merge(severity, 1, Integer::sum);
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total < limit) {
            return Optional.empty();
        }
        String breakdown = counts.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        return Optional.of(DiagnosticLog.error(DiagnosticCode.GATE_FAILED, total, threshold, String.valueOf(limit),
                breakdown, String.valueOf(total)));
    }
}
