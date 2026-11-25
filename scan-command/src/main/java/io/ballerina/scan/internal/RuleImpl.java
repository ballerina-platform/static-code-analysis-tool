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
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanToolException;

/**
 * Represents the implementation of the {@link Rule} interface.
 *
 * @since 0.1.0
 * */
public class RuleImpl implements Rule {

    private final String id;
    private final int numericId;
    private final String description;
    private final RuleKind ruleKind;

    RuleImpl(String id, int numericId, String description, RuleKind ruleKind) {
        this.id = id;
        this.numericId = numericId;
        this.description = description;
        this.ruleKind = ruleKind;
    }

    /**
     * Returns a rule as the scan tool's own implementation, copying one that an analyzer implemented itself through
     * the {@link Rule} getters, so that every report writes it the same way. A copied rule must have what the reports
     * need, so that a broken rule fails where the analyzer reports it instead of while the reports are written.
     *
     * @param rule the rule
     * @return the rule, or a copy of it
     */
    static RuleImpl of(Rule rule) {
        if (rule instanceof RuleImpl ruleImpl) {
            return ruleImpl;
        }
        int missing = rule.id() == null ? 0 : rule.kind() == null ? 1 : rule.description() == null ? 2 : -1;
        if (missing >= 0) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.INCOMPLETE_RULE,
                    rule.id() == null ? String.valueOf(rule.numericId()) : rule.id(), missing));
        }
        return new RuleImpl(rule.id(), rule.numericId(), rule.description(), rule.kind());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int numericId() {
        return numericId;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public RuleKind kind() {
        return ruleKind;
    }
}
