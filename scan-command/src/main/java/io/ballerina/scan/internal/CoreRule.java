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

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.scan.RuleKind.CODE_SMELL;
import static io.ballerina.scan.RuleKind.VULNERABILITY;
import static io.ballerina.scan.internal.RuleFactory.createRule;

/**
 * {@code CoreRule} contains the core static code analysis rules.
 *
 * @since 0.1.0
 */
enum CoreRule {
    AVOID_CHECKPANIC(createRule(1, "Avoid checkpanic", CODE_SMELL)),
    UNUSED_FUNCTION_PARAMETER(createRule(2, "Unused function parameter", CODE_SMELL)),
    HARD_CODED_SECRET(createRule(3, "Hard-coded secrets are security-sensitive", VULNERABILITY)),
    NON_CONFIGURABLE_SECRET(createRule(4, "Non configurable secrets are security-sensitive", VULNERABILITY));

    private final Rule rule;

    CoreRule(Rule rule) {
        this.rule = rule;
    }

    Rule rule() {
        return rule;
    }

    static List<Rule> rules() {
        List<Rule> coreRules = new ArrayList<>();
        for (CoreRule coreRule: CoreRule.values()) {
            coreRules.add(coreRule.rule());
        }
        return coreRules;
    }
}
