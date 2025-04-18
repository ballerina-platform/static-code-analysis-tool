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
 * Represents the kind of rule of a {@link Rule} instance.
 *
 * @since 0.1.0
 */
public enum RuleKind {
    /**
     * Label for marking rules related to code maintainability.
     */
    CODE_SMELL,

    /**
     * Label for marking rules related to coding mistakes that cause errors or unexpected behaviour at runtime.
     */
    BUG,

    /**
     * Label for marking rules related to code susceptible to exploits due to security weaknesses.
     */
    VULNERABILITY
}
