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
}
