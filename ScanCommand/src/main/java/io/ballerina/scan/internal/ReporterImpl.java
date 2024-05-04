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

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.scan.exceptions.RuleNotFoundException;
import io.ballerina.tools.diagnostics.Location;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_ORG;
import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_RULE_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.PATH_SEPARATOR;

/**
 * Represents the implementation of the {@link Reporter} interface.
 *
 * @since 0.1.0
 * */
class ReporterImpl implements Reporter {
    private final List<Issue> issues = new ArrayList<>();
    private final Map<Integer, Rule> rules = new HashMap<>();

    ReporterImpl(List<Rule> rules) {
        rules.forEach(rule -> {
            this.rules.put(rule.numericId(), rule);
        });
    }

    @Override
    public void reportIssue(Document reportedDocument, Location location, int ruleId) {
        Rule rule = rules.get(ruleId);
        if (rule == null) {
            throw new RuleNotFoundException(ruleId);
        }
        issues.add(createIssue(reportedDocument, location, rule));
    }

    @Override
    public void reportIssue(Document reportedDocument, Location location, Rule rule) {
        issues.add(createIssue(reportedDocument, location, rule));
    }

    private Issue createIssue(Document reportedDocument, Location location, Rule rule) {
        String documentName = reportedDocument.name();
        Module module = reportedDocument.module();
        String moduleName = module.moduleName().toString();
        Path issuesFilePath = module.project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        String fullyQualifiedRuleId = rule.id();
        String[] parts = fullyQualifiedRuleId.split(":");
        Source source;
        if (parts[0].equals(BALLERINA_RULE_PREFIX + rule.numericId())) {
            source = Source.BUILT_IN;
        } else {
            String reportedSource = parts[0];
            String pluginOrg = reportedSource.split(PATH_SEPARATOR)[0];
            source = pluginOrg.equals(BALLERINA_ORG) ? Source.BUILT_IN : Source.EXTERNAL;
        }

        return new IssueImpl(location, rule, source, moduleName + System.lineSeparator() + documentName,
                issuesFilePath.toString());
    }

    List<Issue> getIssues() {
        return issues;
    }
}
