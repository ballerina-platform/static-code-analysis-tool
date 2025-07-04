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

/**
 * Represents a diagnostic code.
 *
 * @since 0.1.0
 */
public enum DiagnosticCode {
    INVALID_NUMBER_OF_ARGUMENTS("STATIC_ANALYSIS_ERROR_001", "invalid.number.of.arguments"),
    EMPTY_PACKAGE("STATIC_ANALYSIS_ERROR_002", "empty.package"),
    MISSING_CONFIG_FIELD("STATIC_ANALYSIS_ERROR_003", "missing.config.field"),
    LOADING_REMOTE_CONFIG_FILE("STATIC_ANALYSIS_ERROR_004", "loading.remote.config.file"),
    READING_CONFIG_FILE("STATIC_ANALYSIS_ERROR_005", "reading.config.file"),
    LOADING_REMOTE_PLATFORM_FILE("STATIC_ANALYSIS_ERROR_006", "loading.remote.platform.file"),
    DOWNLOADING_REMOTE_JAR_FILE("STATIC_ANALYSIS_ERROR_007", "downloading.remote.jar.file"),
    FAILED_TO_LOAD_COMPILER_PLUGIN("STATIC_ANALYSIS_ERROR_008", "loading.compiler.plugin"),
    READING_RULES_FILE("STATIC_ANALYSIS_ERROR_009", "reading.rules.file"),
    INVALID_JSON_FORMAT("STATIC_ANALYSIS_ERROR_010", "invalid.json.format"),
    INVALID_JSON_FORMAT_RULE("STATIC_ANALYSIS_ERROR_011", "invalid.json.format.rule"),
    INVALID_JSON_FORMAT_RULE_KIND("STATIC_ANALYSIS_ERROR_012", "invalid.json.format.rule.kind"),
    FAILED_TO_READ_BALLERINA_FILE("STATIC_ANALYSIS_ERROR_013", "failed.to.read.ballerina.file"),
    FAILED_TO_COPY_SCAN_REPORT("STATIC_ANALYSIS_ERROR_014", "failed.to.copy.scan.report"),
    RULE_NOT_FOUND("STATIC_ANALYSIS_ERROR_015", "rule.not.found"),
    ATTEMPT_TO_INCLUDE_AND_EXCLUDE("STATIC_ANALYSIS_ERROR_016", "attempt.to.include.and.exclude"),
    INVALID_FORMAT("STATIC_ANALYSIS_ERROR_017", "invalid.format"),
    REPORT_NOT_SUPPORTED("STATIC_ANALYSIS_WARNING_001", "report.not.supported"),
    SCAN_REPORT_NOT_SUPPORTED("STATIC_ANALYSIS_WARNING_002", "scan.report.not.supported");

    private final String diagnosticId;
    private final String messageKey;

    DiagnosticCode(String diagnosticId, String messageKey) {
        this.diagnosticId = diagnosticId;
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
