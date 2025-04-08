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

import java.util.List;

/**
 * Test utility class to run a scan command and return the issues.
 *
 * @since 0.10.0
 */
public class ScanTestRunner {
    private final TestScanCmd scanCmd;

    /**
     * Create a new {@code ScanTestRunner} instance.
     *
     * @param testScanOptions options to perform the scan
     */
    public ScanTestRunner(TestScanOptions testScanOptions) {
        scanCmd = new TestScanCmd(testScanOptions);
    }

    /**
     * Perform a scan.
     *
     */
    public void performScan() {
        scanCmd.execute();
    }

    /**
     * Get the issues found during the scan.
     *
     * @return list of issues found during the scan
     */
    public List<Issue> getIssues() {
        return scanCmd.getProjectAnalyzer().getReporters().stream()
                .flatMap(reporter1 -> reporter1.getIssues().stream()).toList();
    }

    /**
     * Get the rules registered for the scan.
     *
     * @return list of rules registered for the scan
     */
    public List<Rule> getRules() {
        return scanCmd.getAllRules();
    }
}
