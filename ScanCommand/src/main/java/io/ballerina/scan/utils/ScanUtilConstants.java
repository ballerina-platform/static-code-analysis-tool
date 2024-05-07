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

public class ScanUtilConstants {
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

    private ScanUtilConstants() {
    }
}
