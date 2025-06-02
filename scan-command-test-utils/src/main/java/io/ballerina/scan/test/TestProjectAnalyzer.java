/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scan.test;

import io.ballerina.projects.Project;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.internal.ProjectAnalyzer;
import io.ballerina.scan.utils.ScanTomlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Test utility class to analyze a project and return the reporters.
 *
 * @since 0.10.0
 */
class TestProjectAnalyzer extends ProjectAnalyzer {
    private final List<TestReporter> reporters;

    TestProjectAnalyzer(Project project, ScanTomlFile scanTomlFile) {
        super(project, scanTomlFile);
        this.reporters = new ArrayList<>();
    }

    @Override
    protected ScannerContext getScannerContext(List<Rule> rules) {
        TestScannerContext scannerContext = new TestScannerContext(rules);
        reporters.add(scannerContext.getReporter());
        return scannerContext;
    }

    List<TestReporter> getReporters() {
        return reporters;
    }
}
