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

import java.util.List;

/**
 * {@code Rule} represents a static code analysis rule.
 *
 * @since 0.1.0
 */
public interface Rule {
    /**
     * Retrieve the fully qualified identifier of the rule.
     *
     * @return fully qualified identifier of the rule
     */
    String id();

    /**
     * Returns the numeric identifier of the rule.
     *
     * @return numeric identifier of the rule
     */
    int numericId();

    /**
     * Returns the description of the rule.
     *
     * @return description of the rule
     */
    String description();

    /**
     * Returns {@link RuleKind} of the rule.
     *
     * @return rule kind of the rule
     */
    RuleKind kind();

    /**
     * Returns the human-readable name of the rule. Defaults to {@link #description()} when not
     * overridden.
     *
     * @return name of the rule
     */
    default String name() {
        return description();
    }

    /**
     * Returns the full/long description of the rule. Defaults to {@link #description()} when not
     * overridden.
     *
     * @return full description of the rule
     */
    default String details() {
        return description();
    }

    /**
     * Returns a URI pointing to further documentation on the rule, or {@code null} when unavailable.
     *
     * @return help URI of the rule, or {@code null}
     */
    default String helpUri() {
        return null;
    }

    /**
     * Returns the severity of the rule, or {@code null} when unavailable.
     *
     * @return {@link Severity} of the rule, or {@code null}
     */
    default Severity severity() {
        return null;
    }

    /**
     * Returns the tags associated with the rule (e.g. category references).
     *
     * @return tags of the rule, or an empty list when unavailable
     */
    default List<String> tags() {
        return List.of();
    }

    /**
     * Returns the structured CWE/OWASP coverage of the rule, or {@code null} when unavailable.
     *
     * @return {@link Standards} of the rule, or {@code null}
     */
    default Standards standards() {
        return null;
    }
}
