/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scan;

/**
 * Enum representing the report formats supported by the scan command.
 *
 * @since 0.10.0
 */
public enum ReportFormat {
    JSON("json"),
    SARIF("sarif");

    private final String format;

    ReportFormat(String format) {
        this.format = format;
    }

    /**
     * Returns the format string of the report format.
     *
     * @return the format string
     */
    public String getFormat() {
        return format;
    }

    /**
     * Returns the ReportFormat enum constant corresponding to the given format string.
     *
     * @param format the format string
     * @return the corresponding ReportFormat enum constant
     * @throws IllegalArgumentException if the format string does not match any known format
     */
    public static ReportFormat fromString(String format) {
        for (ReportFormat reportFormat : values()) {
            if (reportFormat.getFormat().equalsIgnoreCase(format)) {
                return reportFormat;
            }
        }
        throw new IllegalArgumentException("Unknown report format: " + format);
    }
}
