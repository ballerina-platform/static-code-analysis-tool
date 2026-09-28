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

import java.util.List;

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
