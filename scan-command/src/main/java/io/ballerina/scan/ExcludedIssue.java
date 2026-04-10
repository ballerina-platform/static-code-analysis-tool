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

package io.ballerina.scan;

/**
 * Represents a security issue that has been excluded from the results.
 *
 * @since 0.11.1
 */
public class ExcludedIssue {
    private final Issue issue;
    private final String ruleId;
    private final String filePath;
    private final String symbol;
    private final String lineHash;
    private final boolean isGlobalExclusion;

    public ExcludedIssue(Issue issue, String ruleId, String filePath, String symbol, String lineHash,
                         boolean isGlobalExclusion) {
        this.issue = issue;
        this.ruleId = ruleId;
        this.filePath = filePath;
        this.symbol = symbol;
        this.lineHash = lineHash;
        this.isGlobalExclusion = isGlobalExclusion;
    }

    /**
     * Returns the {@link Issue} that was excluded.
     *
     * @return the excluded issue
     */
    public Issue issue() {
        return issue;
    }

    /**
     * Returns the fully qualified rule identifier of the excluded issue.
     *
     * @return rule identifier
     */
    public String ruleId() {
        return ruleId;
    }

    /**
     * Returns the file path where the issue was found.
     *
     * @return file path
     */
    public String filePath() {
        return filePath;
    }

    /**
     * Returns the enclosing symbol name where the issue was found.
     *
     * @return symbol name
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Returns the hash of the line content where the issue was found.
     *
     * @return line hash
     */
    public String lineHash() {
        return lineHash;
    }

    /**
     * Returns true if the issue was excluded due to a global rule exclusion.
     *
     * @return true if globally excluded
     */
    public boolean isGlobalExclusion() {
        return isGlobalExclusion;
    }
}
