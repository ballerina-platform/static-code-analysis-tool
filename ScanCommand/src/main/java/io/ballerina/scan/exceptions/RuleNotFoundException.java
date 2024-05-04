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

package io.ballerina.scan.exceptions;

/**
 * Represents the exception thrown for an unidentified static code analysis rule during reporting of an analysis issue.
 *
 * @since 0.1.0
 * */
public class RuleNotFoundException extends RuntimeException {

    /**
     * Returns a new instance of the RuleNotFoundException with the specified rule identifier.
     *
     * @param ruleId numeric identifier of the static code analysis rule
     */
    public RuleNotFoundException(int ruleId) {
        super(String.format("Rule not found: Invalid rule numeric identifier '%d'.", ruleId));
    }
}
