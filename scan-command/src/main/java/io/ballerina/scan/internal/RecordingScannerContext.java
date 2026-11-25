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

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.tools.diagnostics.Location;

import java.util.List;
import java.util.Map;

/**
 * The {@link ScannerContext} given to an analyzer: it records every issue the analyzer reports for the scan, and
 * passes each report on to the {@link Reporter} of the context it wraps, which can be any implementation.
 *
 * @since 0.14.1
 */
class RecordingScannerContext implements ScannerContext {
    private final ScannerContext context;
    private final RecordingReporter reporter;

    RecordingScannerContext(ScannerContext context, List<Rule> rules) {
        this.context = context;
        // Nothing reads the issues of the scan tool's own context, so its reporter is not given them a second time
        Reporter delegate = context.getClass() == ScannerContextImpl.class ? null : context.getReporter();
        this.reporter = new RecordingReporter(rules, delegate);
    }

    @Override
    public Reporter getReporter() {
        return reporter;
    }

    @Override
    public Map<String, Object> userData() {
        return context.userData();
    }

    @Override
    public boolean isExcludeTests() {
        return context.isExcludeTests();
    }

    /**
     * Returns the issues the analyzer reported.
     */
    List<Issue> issues() {
        return reporter.getIssues();
    }

    /**
     * Checks a report as the scan tool's reporter does before passing it on, so that the wrapped reporter sees only
     * reports the scan accepts, and records the issue only once the wrapped reporter has accepted it too.
     */
    private static final class RecordingReporter extends ReporterImpl {
        private final Reporter delegate;

        RecordingReporter(List<Rule> rules, Reporter delegate) {
            super(rules);
            this.delegate = delegate;
        }

        @Override
        public void reportIssue(Document reportedDocument, Location location, int ruleId) {
            Issue issue = createIssue(reportedDocument, location, ruleId);
            if (delegate != null) {
                delegate.reportIssue(reportedDocument, location, ruleId);
            }
            getIssues().add(issue);
        }

        @Override
        public void reportIssue(Document reportedDocument, Location location, Rule rule) {
            Issue issue = createIssue(reportedDocument, location, rule);
            if (delegate != null) {
                delegate.reportIssue(reportedDocument, location, rule);
            }
            getIssues().add(issue);
        }
    }
}
