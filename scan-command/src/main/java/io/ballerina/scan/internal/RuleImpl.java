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
import io.ballerina.scan.Severity;
import io.ballerina.scan.Standards;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanToolException;

import java.util.List;
import java.util.Objects;

/**
 * Represents the implementation of the {@link Rule} interface.
 *
 * @since 0.1.0
 * */
public class RuleImpl implements Rule {

    private final String id;
    private final int numericId;
    private final String name;
    private final String description;
    private final String details;
    private final String helpUri;
    private final Severity severity;
    private final List<String> tags;
    private final Standards standards;
    private final RuleKind ruleKind;

    private RuleImpl(Builder builder) {
        this.id = builder.id;
        this.numericId = builder.numericId;
        this.description = builder.description;
        this.ruleKind = builder.ruleKind;
        this.name = builder.name;
        this.details = builder.details;
        this.helpUri = builder.helpUri;
        this.severity = builder.severity;
        this.tags = builder.tags;
        this.standards = builder.standards;
    }

    private RuleImpl(RuleImpl staged, String id) {
        this.id = id;
        this.numericId = staged.numericId;
        this.description = staged.description;
        this.ruleKind = staged.ruleKind;
        this.name = staged.name;
        this.details = staged.details;
        this.helpUri = staged.helpUri;
        this.severity = staged.severity;
        this.tags = staged.tags;
        this.standards = staged.standards;
    }

    /**
     * Returns a copy of this rule with its {@code id} replaced. Used by {@link RuleFactory} to fill
     * in the fully qualified id once known, after staging the rest of the rule's metadata through
     * {@link Builder} - reading it back via the {@link Rule} getters below rather than exposing
     * builder-only accessors.
     *
     * @param id the fully qualified rule id to use
     * @return a copy of this rule carrying the given id
     */
    RuleImpl withId(String id) {
        return new RuleImpl(this, id);
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

    @Override
    public String name() {
        return name != null ? name : description;
    }

    @Override
    public String details() {
        return details != null ? details : description;
    }

    @Override
    public String helpUri() {
        return helpUri;
    }

    @Override
    public Severity severity() {
        return severity;
    }

    @Override
    public List<String> tags() {
        return tags != null ? List.copyOf(tags) : List.of();
    }

    @Override
    public Standards standards() {
        return standards;
    }

    static Builder builder() {
        return new Builder();
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
        int missing = missingField(rule);
        if (missing >= 0) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.INCOMPLETE_RULE,
                    rule.id() == null ? String.valueOf(rule.numericId()) : rule.id(), missing));
        }
        return builder().id(rule.id()).numericId(rule.numericId()).name(rule.name())
                .description(rule.description()).details(rule.details()).helpUri(rule.helpUri())
                .ruleKind(rule.kind()).severity(rule.severity()).tags(rule.tags()).standards(rule.standards())
                .build();
    }

    // The choice index of the INCOMPLETE_RULE message, or -1 when the rule is complete
    private static int missingField(Rule rule) {
        if (rule.id() == null) {
            return 0;
        }
        if (rule.kind() == null) {
            return 1;
        }
        if (rule.description() == null) {
            return 2;
        }
        List<String> tags = rule.tags();
        return tags != null && tags.stream().anyMatch(Objects::isNull) ? 3 : -1;
    }

    /**
     * {@code Builder} stages the rich, authored metadata of a rule (numeric id, name, description,
     * severity, tags, standards, helpUri, etc.) before its fully qualified {@code id} is
     * known. That is only computed by {@link RuleFactory} once it knows whether the rule being
     * built is a core rule or an external (compiler-plugin authored) rule.
     */
    static final class Builder {
        private String id;
        private int numericId;
        private String name;
        private String description;
        private String details;
        private String helpUri;
        private RuleKind ruleKind;
        private Severity severity;
        private List<String> tags;
        private Standards standards;

        Builder id(String id) {
            this.id = id;
            return this;
        }

        Builder numericId(int numericId) {
            this.numericId = numericId;
            return this;
        }

        Builder name(String name) {
            this.name = name;
            return this;
        }

        Builder description(String description) {
            this.description = description;
            return this;
        }

        Builder details(String details) {
            this.details = details;
            return this;
        }

        Builder helpUri(String helpUri) {
            this.helpUri = helpUri;
            return this;
        }

        Builder ruleKind(RuleKind ruleKind) {
            this.ruleKind = ruleKind;
            return this;
        }

        Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        Builder standards(Standards standards) {
            this.standards = standards;
            return this;
        }

        RuleImpl build() {
            return new RuleImpl(this);
        }
    }
}
