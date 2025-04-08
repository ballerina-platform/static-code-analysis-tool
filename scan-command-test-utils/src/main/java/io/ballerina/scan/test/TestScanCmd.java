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
import io.ballerina.scan.internal.ProjectAnalyzer;
import io.ballerina.scan.internal.ScanCmd;
import io.ballerina.scan.utils.ScanTomlFile;

import java.util.Optional;

/**
 * TestScanCmd extends ScanCmd to extend it for testing purposes.
 *
 * @since 0.10.0
 */
public class TestScanCmd extends ScanCmd {
    private final Project project;
    private TestProjectAnalyzer projectAnalyzer;

    TestScanCmd(TestScanOptions options) {
        super(
                options.project().sourceRoot(),
                options.outputStream(),
                options.helpFlag(),
                options.platformTriggered(),
                options.targetDir(),
                options.scanReport(),
                options.listRules(),
                options.includeRules(),
                options.excludeRules(),
                options.platforms()
        );
        this.project = options.project();
    }

    @Override
    protected Optional<Project> getProject() {
        return Optional.of(this.project);
    }

    @Override
    protected ProjectAnalyzer getProjectAnalyzer(Project project, ScanTomlFile scanTomlFile) {
        this.projectAnalyzer = new TestProjectAnalyzer(project, scanTomlFile);
        return this.projectAnalyzer;
    }

    TestProjectAnalyzer getProjectAnalyzer() {
        return this.projectAnalyzer;
    }
}
