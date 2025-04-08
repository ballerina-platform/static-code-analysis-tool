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

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * Test utility class to hold scan options for testing purposes.
 *
 * @since 0.10.0
 */
public class TestOptions {
    private final Project project;
    private final PrintStream outputStream;
    private final boolean helpFlag;
    private final boolean platformTriggered;
    private final String targetDir;
    private final boolean scanReport;
    private final boolean listRules;
    private final List<Rule> includeRules;
    private final List<Rule> excludeRules;
    private final List<String> platforms;

    private TestOptions(Project project, PrintStream outputStream, boolean helpFlag, boolean platformTriggered,
                        String targetDir, boolean scanReport, boolean listRules, List<Rule> includeRules,
                        List<Rule> excludeRules, List<String> platforms) {
        this.project = project;
        this.outputStream = outputStream;
        this.helpFlag = helpFlag;
        this.platformTriggered = platformTriggered;
        this.targetDir = targetDir;
        this.scanReport = scanReport;
        this.listRules = listRules;
        this.includeRules = includeRules;
        this.excludeRules = excludeRules;
        this.platforms = platforms;
    }

    /**
     * Create a new {@code TestOptionsBuilder} instance.
     *
     * @param project the project to be scanned
     * @return a new {@code TestOptionsBuilder} instance
     */
    public static TestOptionsBuilder builder(Project project) {
        return new TestOptionsBuilder(project);
    }

    /**
     * Get the project to be scanned.
     *
     * @return the project to be scanned
     */
     Project project() {
        return project;
     }

    /**
     * Get the output stream.
     *
     * @return the output stream
     */
    PrintStream outputStream() {
        return outputStream;
    }

    /**
     * Get if the help flag is enabled or not.
     *
     * @return true if the help flag is enabled, false otherwise
     */
    boolean helpFlag() {
        return helpFlag;
    }

    /**
     * Get if the scan is triggered by a platform or not.
     *
     * @return true if the scan is triggered by a platform, false otherwise
     */
    boolean platformTriggered() {
        return platformTriggered;
    }

    /**
     * Get the target directory.
     *
     * @return the target directory
     */
    String targetDir() {
        return targetDir;
    }

    /**
     * Get if the scan report is enabled or not.
     *
     * @return true if the scan report is enabled, false otherwise
     */
    boolean scanReport() {
        return scanReport;
    }

    /**
     * Get if the rules should be listed or not.
     *
     * @return true if the rules should be listed, false otherwise
     */
    boolean listRules() {
        return listRules;
    }

    /**
     * Get the list of rules to be included.
     *
     * @return the list of rules to be included
     */
    List<Rule> includeRules() {
        return includeRules;
    }

    /**
     * Get the list of rules to be excluded.
     *
     * @return the list of rules to be excluded
     */
    List<Rule> excludeRules() {
        return excludeRules;
    }

    /**
     * Get the list of platforms.
     *
     * @return the list of platforms
     */
    List<String> platforms() {
        return platforms;
    }

    public static class TestOptionsBuilder {
        private final Project project;
        private PrintStream outputStream;
        private boolean helpFlag;
        private boolean platformTriggered;
        private String targetDir;
        private boolean scanReport;
        private boolean listRules;
        private List<Rule> includeRules = List.of();
        private List<Rule> excludeRules = List.of();
        private List<String> platforms = List.of();

        private TestOptionsBuilder(Project project) {
            this.project = project;
        }

        /**
         * Set the output stream.
         *
         * @param outputStream the output stream
         * @return this builder
         */
        public TestOptionsBuilder setOutputStream(PrintStream outputStream) {
            this.outputStream = outputStream;
            return this;
        }

        /**
         * Set the help flag.
         *
         * @param helpFlag true if the help flag needs to be enabled, false otherwise
         * @return this builder
         */
        public TestOptionsBuilder setHelpFlag(boolean helpFlag) {
            this.helpFlag = helpFlag;
            return this;
        }

        /**
         * Set if the scan is triggered by a platform.
         *
         * @param platformTriggered true if the scan is triggered by a platform, false otherwise
         * @return this builder
         */
        public TestOptionsBuilder setPlatformTriggered(boolean platformTriggered) {
            this.platformTriggered = platformTriggered;
            return this;
        }

        /**
         * Set the target directory.
         *
         * @param targetDir the target directory
         * @return this builder
         */
        public TestOptionsBuilder setTargetDir(String targetDir) {
            this.targetDir = targetDir;
            return this;
        }

        /**
         * Set if the scan report needs to be enabled.
         *
         * @param scanReport true if the scan report needs to be enabled, false otherwise
         * @return this builder
         */
        public TestOptionsBuilder setScanReport(boolean scanReport) {
            this.scanReport = scanReport;
            return this;
        }

        /**
         * Set if the rules should be listed.
         *
         * @param listRules true if the rules should be listed, false otherwise
         * @return this builder
         */
        public TestOptionsBuilder setListRules(boolean listRules) {
            this.listRules = listRules;
            return this;
        }

        /**
         * Set the list of rules to be included.
         *
         * @param includeRules the list of rules to be included
         * @return this builder
         */
        public TestOptionsBuilder setIncludeRules(List<Rule> includeRules) {
            this.includeRules = Collections.unmodifiableList(includeRules);
            return this;
        }

        /**
         * Set the list of rules to be excluded.
         *
         * @param excludeRules the list of rules to be excluded
         * @return this builder
         */
        public TestOptionsBuilder setExcludeRules(List<Rule> excludeRules) {
            this.excludeRules = Collections.unmodifiableList(excludeRules);
            return this;
        }

        /**
         * Set the list of platforms.
         *
         * @param platforms the list of platforms
         * @return this builder
         */
        public TestOptionsBuilder setPlatforms(List<String> platforms) {
            this.platforms = Collections.unmodifiableList(platforms);
            return this;
        }

        /**
         * Build the {@code TestOptions} instance.
         *
         * @return the built {@code TestOptions} instance
         */
        public TestOptions build() {
            return new TestOptions(project, outputStream, helpFlag, platformTriggered,
                    targetDir, scanReport, listRules, includeRules, excludeRules, platforms);
        }
    }
}
