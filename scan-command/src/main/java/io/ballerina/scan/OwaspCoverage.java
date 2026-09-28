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
 * {@code OwaspCoverage} represents the OWASP Top 10 categories a rule maps to for a single
 * edition/year.
 *
 * @since 0.1.0
 */
public final class OwaspCoverage {
    private final int year;
    private final List<Integer> categories;

    public OwaspCoverage(int year, List<Integer> categories) {
        this.year = year;
        this.categories = categories != null ? List.copyOf(categories) : null;
    }

    /**
     * Returns the OWASP Top 10 edition year (e.g. {@code 2025}).
     *
     * @return the edition year
     */
    public int year() {
        return year;
    }

    /**
     * Returns the OWASP Top 10 category numbers within {@link #year()} that the rule maps to
     * (e.g. {@code 1} for {@code A01}).
     *
     * @return the category numbers, or an empty list when none
     */
    public List<Integer> categories() {
        return categories != null ? categories : List.of();
    }
}
