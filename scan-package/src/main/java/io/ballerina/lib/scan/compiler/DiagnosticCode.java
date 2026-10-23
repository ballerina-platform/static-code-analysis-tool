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

package io.ballerina.lib.scan.compiler;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * Diagnostics reported by the {@code ballerina/scan} compiler plugin.
 *
 * @since 1.0.0
 */
enum DiagnosticCode {
    INVALID_RULE_FORMAT("SCAN_SUPPRESS_101", "invalid rule ID ''{0}'': expected ''ballerina:<number>'' or "
            + "''<org>/<package>:<number>''", DiagnosticSeverity.ERROR),
    BLANK_REASON("SCAN_SUPPRESS_104", "blank suppression reason; it is treated as absent",
            DiagnosticSeverity.WARNING),
    DUPLICATE_RULE("SCAN_SUPPRESS_105", "rule ''{0}'' is listed more than once", DiagnosticSeverity.WARNING),
    UNVERIFIABLE_RULE("SCAN_SUPPRESS_106", "cannot verify rule ''{0}'': use a string literal or a constant",
            DiagnosticSeverity.WARNING),
    EMPTY_RULE_LIST("SCAN_SUPPRESS_107", "empty rule list; omit ''rules'' to suppress every rule",
            DiagnosticSeverity.ERROR);

    private final String code;
    private final String messageFormat;
    private final DiagnosticSeverity severity;

    DiagnosticCode(String code, String messageFormat, DiagnosticSeverity severity) {
        this.code = code;
        this.messageFormat = messageFormat;
        this.severity = severity;
    }

    String code() {
        return code;
    }

    String messageFormat() {
        return messageFormat;
    }

    DiagnosticSeverity severity() {
        return severity;
    }
}
