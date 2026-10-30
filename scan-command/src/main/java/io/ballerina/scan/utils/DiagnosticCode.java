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
    PLATFORM_FILE_NOT_FOUND("STATIC_ANALYSIS_ERROR_006", "platform.file.not.found"),
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
    FAILED_TO_LOAD_VERSION_PROPERTIES("STATIC_ANALYSIS_ERROR_018", "failed.to.load.version.properties"),
    COMPILATION_CONTAINS_ERRORS("STATIC_ANALYSIS_ERROR_019", "compilation.contains.errors"),
    INVALID_JSON_FORMAT_RULE_SEVERITY("STATIC_ANALYSIS_ERROR_020", "invalid.json.format.rule.severity"),
    SUPPRESSION_MISSING_REASON("STATIC_ANALYSIS_ERROR_021", "suppression.missing.reason"),
    SUPPRESSION_MISSING_RULE_ID("STATIC_ANALYSIS_ERROR_022", "suppression.missing.rule.id"),
    SUPPRESSION_REQUIREMENTS_NOT_MET("STATIC_ANALYSIS_ERROR_023", "suppression.requirements.not.met"),
    UNKNOWN_RULE("STATIC_ANALYSIS_ERROR_024", "unknown.rule"),
    INVALID_SUPPRESSION_SETTING("STATIC_ANALYSIS_ERROR_025", "invalid.suppression.setting"),
    UNKNOWN_SUPPRESSION_SETTING("STATIC_ANALYSIS_ERROR_026", "unknown.suppression.setting"),
    MISSING_DEPENDENCIES_TOML("STATIC_ANALYSIS_ERROR_027", "missing.dependencies.toml"),
    INVALID_SUPPRESSION_TABLE("STATIC_ANALYSIS_ERROR_028", "invalid.suppression.table"),
    INVALID_RULE_ID("STATIC_ANALYSIS_ERROR_029", "invalid.rule.id"),
    EMPTY_RULE_LIST("STATIC_ANALYSIS_ERROR_030", "empty.rule.list"),
    INVALID_OPTION_VALUE("STATIC_ANALYSIS_ERROR_031", "invalid.option.value"),
    UNKNOWN_RULE_OPTION("STATIC_ANALYSIS_ERROR_032", "unknown.rule.option"),
    INVALID_PROJECT_PATH("STATIC_ANALYSIS_ERROR_033", "invalid.project.path"),
    FILE_IN_PACKAGE("STATIC_ANALYSIS_ERROR_034", "file.in.package"),
    PROJECT_LOAD_FAILED("STATIC_ANALYSIS_ERROR_035", "project.load.failed"),
    CONFIG_FILE_HAS_ERRORS("STATIC_ANALYSIS_ERROR_036", "config.file.has.errors"),
    INVALID_CONFIG_VALUE("STATIC_ANALYSIS_ERROR_037", "invalid.config.value"),
    UNKNOWN_CONFIG_KEY("STATIC_ANALYSIS_ERROR_038", "unknown.config.key"),
    PLATFORM_NOT_AVAILABLE("STATIC_ANALYSIS_ERROR_039", "platform.not.available"),
    FAILED_TO_WRITE_REPORT("STATIC_ANALYSIS_ERROR_040", "failed.to.write.report"),
    GATE_FAILED("STATIC_ANALYSIS_ERROR_041", "gate.failed"),
    PACKAGES_FAILED_GATE("STATIC_ANALYSIS_ERROR_042", "packages.failed.gate"),
    PACKAGES_FAILED("STATIC_ANALYSIS_ERROR_043", "packages.failed"),
    WORKSPACE_PLATFORM_NOT_ALLOWED("STATIC_ANALYSIS_ERROR_044", "workspace.platform.not.allowed"),
    INVALID_CONFIG_TABLE("STATIC_ANALYSIS_ERROR_045", "invalid.config.table"),
    INVALID_ANALYZER_LIST("STATIC_ANALYSIS_ERROR_046", "invalid.analyzer.list"),
    REPORT_NOT_SUPPORTED("STATIC_ANALYSIS_WARNING_001", "report.not.supported"),
    SCAN_REPORT_NOT_SUPPORTED("STATIC_ANALYSIS_WARNING_002", "scan.report.not.supported"),
    UNMATCHED_SUPPRESSION("STATIC_ANALYSIS_WARNING_003", "unmatched.suppression"),
    UNMATCHED_BLANKET_SUPPRESSION("STATIC_ANALYSIS_WARNING_004", "unmatched.blanket.suppression"),
    UNKNOWN_RULE_PROVIDER("STATIC_ANALYSIS_WARNING_006", "unknown.rule.provider"),
    DUPLICATE_SUPPRESSION_RULE("STATIC_ANALYSIS_WARNING_007", "duplicate.suppression.rule"),
    DUPLICATE_BLANKET_SUPPRESSION("STATIC_ANALYSIS_WARNING_008", "duplicate.blanket.suppression"),
    IGNORED_SUPPRESSION("STATIC_ANALYSIS_WARNING_009", "ignored.suppression"),
    SETTING_OVERRIDDEN("STATIC_ANALYSIS_WARNING_010", "setting.overridden"),
    EMPTY_PACKAGE_SKIPPED("STATIC_ANALYSIS_WARNING_011", "empty.package.skipped"),
    PLATFORM_FAILED("STATIC_ANALYSIS_WARNING_012", "platform.failed"),
    EXCLUDE_TESTS_NOT_SUPPORTED("STATIC_ANALYSIS_WARNING_013", "exclude.tests.not.supported"),
    WORKSPACE_RULE_NOT_AVAILABLE("STATIC_ANALYSIS_WARNING_014", "workspace.rule.not.available");

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
