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

import io.ballerina.scan.utils.ScanTomlFile;

/**
 * Options controlling how {@code @scan:Suppress} annotations are applied.
 *
 * @param ignoreSuppressions disregard every suppression, reporting the unsuppressed picture
 * @param requireReason      report a suppression without a reason as an error
 * @param requireRuleId      report a suppression that names no rule as an error
 * @since 0.11.1
 */
record SuppressionOptions(boolean ignoreSuppressions, boolean requireReason, boolean requireRuleId) {

    /**
     * Combines the scan configuration with the command-line options. A command-line option can disable
     * suppressions or switch a requirement on for a single run, but cannot undo the scan configuration.
     */
    static SuppressionOptions of(ScanTomlFile scanTomlFile, boolean ignoreSuppressions, boolean requireReason,
                                 boolean requireRuleId) {
        return new SuppressionOptions(ignoreSuppressions || !scanTomlFile.isSuppressionEnabled(),
                requireReason || scanTomlFile.isSuppressionReasonRequired(),
                requireRuleId || scanTomlFile.isSuppressionRuleIdRequired());
    }
}
