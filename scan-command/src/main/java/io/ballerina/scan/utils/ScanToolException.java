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

package io.ballerina.scan.utils;

import java.util.List;

/**
 * {@code ScanToolException} represents a scan tool related runtime exception.
 *
 * @since 0.1.0
 * */
public class ScanToolException extends RuntimeException {
    private final List<String> diagnostics;

    /**
     * Returns a new instance of the ScanToolException with the specified diagnostic message.
     *
     * @param exception diagnostic message
     */
    public ScanToolException(String exception) {
        this(exception, List.of());
    }

    /**
     * Returns a new instance of the ScanToolException with the specified diagnostic message and the diagnostics
     * that explain it, such as the errors found in a configuration file.
     *
     * @param exception   diagnostic message
     * @param diagnostics diagnostics reported under the message, each formatted as one line
     * @since 0.13.0
     */
    public ScanToolException(String exception, List<String> diagnostics) {
        super(exception);
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Returns the diagnostics reported under the message.
     *
     * @return the diagnostics, each formatted as one line
     * @since 0.13.0
     */
    public List<String> diagnostics() {
        return diagnostics;
    }
}
