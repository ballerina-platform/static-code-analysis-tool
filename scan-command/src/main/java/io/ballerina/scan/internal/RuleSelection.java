/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

import java.util.Collection;
import java.util.Set;

/**
 * The rules a scan reports, chosen with the include and exclude lists of the command line and {@code Scan.toml}.
 *
 * @param include the rules to report; every rule when empty
 * @param exclude the rules not to report
 * @since 0.11.1
 */
record RuleSelection(Set<String> include, Set<String> exclude) {

    RuleSelection(Collection<String> include, Collection<String> exclude) {
        this(Set.copyOf(include), Set.copyOf(exclude));
    }

    boolean selects(String ruleId) {
        return (include.isEmpty() || include.contains(ruleId)) && !exclude.contains(ruleId);
    }

    boolean isPartial() {
        return !include.isEmpty() || !exclude.isEmpty();
    }
}
