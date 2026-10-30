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

import io.ballerina.tools.text.LineRange;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Diagnostic log for scan tool.
 *
 * @since 0.1.0
 */
public class DiagnosticLog {
    private static final String ERROR_PREFIX = "error";
    private static final String WARNING_PREFIX = "warning";
    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("scan", Locale.getDefault());

    private DiagnosticLog() {
    }

    public static String error(DiagnosticCode code, Object... args) {
        String msgKey = MESSAGES.getString(ERROR_PREFIX + "." + code.messageKey());
        return MessageFormat.format(msgKey, args);
    }

    public static String warning(DiagnosticCode code, Object... args) {
        String msgKey = MESSAGES.getString(WARNING_PREFIX + "." + code.messageKey());
        return MessageFormat.format(msgKey, args);
    }

    /**
     * Formats an error found in a file as {@code <path>:<line>:<col>: error: <message>}, with one-based positions.
     *
     * @param path      the path of the file, as shown to the user
     * @param lineRange the range of the error in the file
     * @param message   the error message
     * @return the formatted error
     * @since 0.13.0
     */
    public static String fileError(String path, LineRange lineRange, String message) {
        return fileDiagnostic(path, lineRange, ERROR_PREFIX, message);
    }

    /**
     * Formats a warning found in a file as {@code <path>:<line>:<col>: warning: <message>}, with one-based positions.
     *
     * @param path      the path of the file, as shown to the user
     * @param lineRange the range of the warning in the file
     * @param message   the warning message
     * @return the formatted warning
     * @since 0.13.0
     */
    public static String fileWarning(String path, LineRange lineRange, String message) {
        return fileDiagnostic(path, lineRange, WARNING_PREFIX, message);
    }

    private static String fileDiagnostic(String path, LineRange lineRange, String severity, String message) {
        return String.format("%s:%d:%d: %s: %s", path, lineRange.startLine().line() + 1,
                lineRange.startLine().offset() + 1, severity, message);
    }
}
