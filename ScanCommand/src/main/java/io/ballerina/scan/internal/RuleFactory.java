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

package io.ballerina.scan.internal;

import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;

import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_RULE_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.PATH_SEPARATOR;

/**
 * {@code RuleFactory} contains the logic to create a {@link Rule} with fully qualified identifier.
 *
 * @since 0.1.0
 * */
class RuleFactory {
    /**
     * Returns a core static code analysis {@link Rule} instance.
     *
     * @param numericId   numeric identifier of the static code analysis rule
     * @param description description of the static code analysis rule
     * @param ruleKind    {@link RuleKind} of the static code analysis rule
     *
     * @return a core static code analysis rule instance
     */
    static Rule createRule(int numericId, String description, RuleKind ruleKind) {
        return new RuleImpl(BALLERINA_RULE_PREFIX + numericId, numericId, description, ruleKind);
    }

    /**
     * Returns an external static code analysis {@link Rule} instance.
     *
     * @param numericId   numeric identifier of the static code analysis rule
     * @param description description of the static code analysis rule
     * @param ruleKind    {@link RuleKind} of the static code analysis rule
     * @param org         Ballerina package organisation name of the compiler plugin
     * @param name        Ballerina package name of the compiler plugin
     *
     * @return an external static code analysis rule instance
     */
    static Rule createRule(int numericId, String description, RuleKind ruleKind, String org, String name) {
        String reportedSource = org + PATH_SEPARATOR + name;
        return new RuleImpl(reportedSource + ":" + BALLERINA_RULE_PREFIX + numericId, numericId, description,
                ruleKind);
    }

    private RuleFactory() {
    }
}
