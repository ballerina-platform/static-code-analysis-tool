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

import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.Location;

/**
 * {@code Reporter} represents the reporter used for reporting {@link Issue} instances during static code analysis.
 *
 * @since 0.1.0
 * */
public interface Reporter {
    /**
     * Reports an issue identified during static code analysis with the use of the numeric identifier of the rule.
     *
     * @param reportedDocument the Ballerina document for which the issue is reported
     * @param location         location of reported issue
     * @param ruleId           numeric identifier of the violated static code analysis rule
     * */
    void reportIssue(Document reportedDocument, Location location, int ruleId);

    /**
     * Reports an issue identified during static code analysis with the use of the rule.
     *
     * @param reportedDocument the Ballerina document for which the issue is reported
     * @param location         location of reported issue
     * @param rule             static code analysis rule
     * */
    void reportIssue(Document reportedDocument, Location location, Rule rule);
}
