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

import java.util.ArrayList;
import java.util.List;

/**
 * {@code Standards} represents the structured CWE/OWASP coverage of a static code analysis rule.
 *
 * @since 0.1.0
 */
public final class Standards {
    private final List<Integer> cwe;
    private final List<OwaspCoverage> owasp;

    public Standards(List<Integer> cwe, List<OwaspCoverage> owasp) {
        this.cwe = cwe != null ? List.copyOf(cwe) : null;
        this.owasp = owasp != null ? List.copyOf(owasp) : null;
    }

    /**
     * Returns the CWE weakness numbers the rule maps to (e.g. {@code 22} for CWE-22).
     *
     * @return the CWE numbers, or an empty list when none
     */
    public List<Integer> cwe() {
        return cwe != null ? cwe : List.of();
    }

    /**
     * Returns the OWASP Top 10 coverage of the rule, grouped by edition year.
     *
     * @return the OWASP coverage entries, or an empty list when none
     */
    public List<OwaspCoverage> owasp() {
        return owasp != null ? owasp : List.of();
    }

    /**
     * Returns this coverage formatted as SARIF {@code properties.tags} entries:
     * {@code external/cwe/cwe-*} for each {@link #cwe()} entry and
     * {@code external/owasp/owasp-a*-*} for each {@link #owasp()} entry.
     *
     * @return the SARIF tag entries for this standards coverage, or an empty list when none
     */
    public List<String> toSarifTags() {
        List<String> sarifTags = new ArrayList<>();
        for (Integer cwe : cwe()) {
            sarifTags.add("external/cwe/cwe-" + cwe);
        }
        for (OwaspCoverage coverage : owasp()) {
            for (Integer category : coverage.categories()) {
                sarifTags.add(String.format("external/owasp/owasp-a%02d-%d", category, coverage.year()));
            }
        }
        return sarifTags;
    }
}
