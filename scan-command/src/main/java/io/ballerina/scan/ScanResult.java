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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the response of a security scan, containing active, excluded and suppressed issues.
 *
 * @since 0.11.1
 */
public class ScanResult {
    private final List<Issue> activeIssues;
    private final List<ExcludedIssue> excludedIssues;
    private final List<SuppressedIssue> suppressedIssues;

    public ScanResult(List<Issue> activeIssues, List<ExcludedIssue> excludedIssues) {
        this(activeIssues, excludedIssues, List.of());
    }

    /**
     * Creates a scan result that also carries the issues accepted by in-source suppressions.
     *
     * @param activeIssues     issues that are neither excluded nor suppressed
     * @param excludedIssues   issues excluded by rule selection
     * @param suppressedIssues issues accepted by {@code @scan:Suppress} annotations
     * @since 0.13.0
     */
    public ScanResult(List<Issue> activeIssues, List<ExcludedIssue> excludedIssues,
                      List<SuppressedIssue> suppressedIssues) {
        this.activeIssues = new ArrayList<>(activeIssues);
        this.excludedIssues = new ArrayList<>(excludedIssues);
        this.suppressedIssues = new ArrayList<>(suppressedIssues);
    }

    /**
     * Returns the list of active security issues found.
     *
     * @return list of active issues
     */
    public List<Issue> activeIssues() {
        return Collections.unmodifiableList(activeIssues);
    }

    /**
     * Returns the list of security issues that were excluded based on Scan.toml configurations.
     *
     * @return list of excluded issues
     */
    public List<ExcludedIssue> excludedIssues() {
        return Collections.unmodifiableList(excludedIssues);
    }

    /**
     * Returns the list of issues accepted by {@code @scan:Suppress} annotations in the source.
     *
     * @return list of suppressed issues
     * @since 0.13.0
     */
    public List<SuppressedIssue> suppressedIssues() {
        return Collections.unmodifiableList(suppressedIssues);
    }
}
