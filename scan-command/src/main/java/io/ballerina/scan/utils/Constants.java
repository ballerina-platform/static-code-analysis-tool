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
 * {@code Constants} contains the constants used by the scan tool utilities.
 *
 * @since 0.1.0
 */
public class Constants {
    static final String RESULTS_JSON_FILE = "scan_results.json";
    static final String RESULTS_HTML_FILE = "index.html";
    static final String REPORT_DATA_PLACEHOLDER = "__data__";
    static final String SCAN_REPORT_PROJECT_NAME = "projectName";
    static final String SCAN_REPORT_FILE_NAME = "fileName";
    static final String SCAN_REPORT_FILE_PATH = "filePath";
    static final String SCAN_REPORT_FILE_CONTENT = "fileContent";
    static final String SCAN_REPORT_SCANNED_FILES = "scannedFiles";
    static final String SCAN_REPORT_ZIP_FILE = "report.zip";
    static final String SCAN_REPORT_ISSUES = "issues";
    static final String SCAN_REPORT_ISSUE_RULE_ID = "ruleID";
    static final String SCAN_REPORT_ISSUE_SEVERITY = "issueSeverity";
    static final String SCAN_REPORT_ISSUE_TYPE = "issueType";
    static final String SCAN_REPORT_ISSUE_MESSAGE = "message";
    static final String SCAN_REPORT_ISSUE_TEXT_RANGE = "textRange";
    static final String SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE = "startLine";
    static final String SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE_OFFSET = "startLineOffset";
    static final String SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE = "endLine";
    static final String SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE_OFFSET = "endLineOffset";
    static final String SCAN_TABLE = "scan";
    static final String SCAN_FILE_FIELD = "configPath";
    static final String SCAN_FILE = "Scan.toml";
    static final String PLATFORM_TABLE = "platform";
    static final String PLATFORM_NAME = "name";
    static final String PLATFORM_PATH = "path";
    static final String ANALYZER_TABLE = "analyzer";
    static final String ANALYZER_ORG = "org";
    static final String ANALYZER_NAME = "name";
    static final String ANALYZER_VERSION = "version";
    static final String ANALYZER_REPOSITORY = "repository";
    static final String RULES_TABLE = "rule";
    static final String JAR_PREDICATE = ".jar";
    static final String CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+$";
    static final String RULE_ID_COLUMN = "RuleID";
    static final String RULE_KIND_COLUMN = "Rule Kind";
    static final String RULE_DESCRIPTION_COLUMN = "Rule Description";
    static final String[] RULE_PRIORITY_LIST = {"ballerina", "ballerinax", "wso2"};

    public static class Token {
        public static final String FLOAT = "float";
        public static final String INT = "int";
        public static final String INFINITY = "Infinity";
        public static final String MAX_VALUE = "MAX_VALUE";
        public static final String MIN_VALUE = "MIN_VALUE";
        public static final String TRUE = "true";
        public static final String FALSE = "false";
        public static final String ZERO = "0";
        public static final String ONE = "1";
        public static final String MINUS_ONE = "-1";
        private Token() {
        }
    }

    private Constants() {
    }
}
