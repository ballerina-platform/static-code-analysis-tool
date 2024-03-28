/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
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

import io.ballerina.tools.diagnostics.Location;

/**
 * {@code Issue} represents a reported static code analysis issue.
 *
 * @since 0.1.0
 */
public interface Issue {
    /**
     * Returns the {@link Location} of reported issue.
     *
     * @return Location of reported issue
     */
    Location location();

    /**
     * Returns the fully qualified rule id of reported issue.
     *
     * @return fully qualified rule id of reported issue
     */
    String ruleId();

    /**
     * Returns {@link IssueType} of reported issue.
     *
     * @return IssueType of issue
     */
    IssueType issueType();

    /**
     * Returns {@link Severity} of reported issue.
     *
     * @return Severity of issue
     */
    Severity issueSeverity();
}
