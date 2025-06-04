/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.examplestaticcodeanalyzer;

import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleProvider;

import java.util.List;

public class CustomRuleProvider implements RuleProvider {
    /**
     * Returns all Rule instances provided by this analyzer.
     *
     * @return an iterable of rules provided by this analyzer
     */
    @Override
    public Iterable<Rule> getRules() {
        return List.of(CustomRule.values());
    }
}
