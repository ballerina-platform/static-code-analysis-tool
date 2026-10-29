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

import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LineRange;

/**
 * A diagnostic about a {@code @scan:Suppress} annotation, such as one that matched no issue.
 *
 * @param severity    severity of the diagnostic
 * @param message     description of the problem
 * @param fileName    name of the document containing the annotation
 * @param location    location of the annotation, or of the rule the diagnostic is about
 * @param suppression the annotation the diagnostic is about, or {@code null} when it could not be read
 * @since 0.13.0
 */
record SuppressionDiagnostic(DiagnosticSeverity severity, String message, String fileName, LineRange location,
                             SuppressionAnnotation suppression) {

    boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }

    @Override
    public String toString() {
        return String.format("%s [%s:(%d:%d,%d:%d)] %s", severity, fileName,
                location.startLine().line() + 1, location.startLine().offset() + 1,
                location.endLine().line() + 1, location.endLine().offset() + 1, message);
    }
}
