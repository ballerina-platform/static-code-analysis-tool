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

package io.ballerina.scan.internal;

import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

/**
 * Represents the implementation of the {@link Issue} interface.
 *
 * @since 0.1.0
 * */
public class IssueImpl implements Issue {
    private final BLangDiagnosticLocation location;
    private final RuleImpl rule;
    private final Source source;
    private final String fileName;
    private final String filePath;

    IssueImpl(Location location,
              Rule rule,
              Source source,
              String fileName,
              String filePath) {
        LineRange lineRange = location.lineRange();
        TextRange textRange = location.textRange();
        this.location = new BLangDiagnosticLocation(lineRange.fileName(), lineRange.startLine().line() + 1,
                lineRange.endLine().line() + 1, lineRange.startLine().offset() + 1, lineRange.endLine().offset() + 1,
                textRange.startOffset() + 1, textRange.length());
        this.rule = (RuleImpl) rule;
        this.source = source;
        this.fileName = fileName;
        this.filePath = filePath;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public Rule rule() {
        return rule;
    }

    @Override
    public Source source() {
        return source;
    }

    public String filePath() {
        return filePath;
    }

    public String fileName() {
        return fileName;
    }
}
