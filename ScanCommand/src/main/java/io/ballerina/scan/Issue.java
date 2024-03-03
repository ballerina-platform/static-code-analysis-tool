/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

public class Issue {

    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;
    private final String ruleID;
    private final String message;
    private final String issueType; // core issue or external issue
    private final String type; // CODE_SMELL, BUG, VULNERABILITY etc
    // There can be more than one ballerina file which has the same name, so we store it in the following format:
    // i.e: fileName = "moduleName/main.bal"
    private final String fileName;
    private final String reportedFilePath;

    public Issue(int startLine,
                 int startLineOffset,
                 int endLine,
                 int endLineOffset,
                 String ruleID,
                 String message,
                 String issueType,
                 String type,
                 String fileName,
                 String reportedFilePath) {

        this.startLine = startLine;
        this.startLineOffset = startLineOffset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
        this.ruleID = ruleID;
        this.message = message;
        this.issueType = issueType;
        this.type = type;
        this.fileName = fileName;
        this.reportedFilePath = reportedFilePath;
    }

    public int getStartLine() {

        return startLine;
    }

    public int getStartLineOffset() {

        return startLineOffset;
    }

    public int getEndLine() {

        return endLine;
    }

    public int getEndLineOffset() {

        return endLineOffset;
    }

    public String getRuleID() {

        return ruleID;
    }

    public String getMessage() {

        return message;
    }

    public String getIssueType() {

        return issueType;
    }

    public String getReportedFilePath() {

        return reportedFilePath;
    }

    public String getFileName() {

        return fileName;
    }

    public String getType() {

        return type;
    }
}
