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

import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Scan utilities tests.
 *
 * @since 0.1.0
 */
public class ScanUtilsTest extends BaseTest {
    private final Path validBalProject = testResources.resolve("test-resources")
            .resolve("valid-bal-project");
    private static final String RESULTS_DIRECTORY = "results";

    @AfterTest
    void cleanup() {
        Path resultsDirectoryPath = validBalProject.resolve(RESULTS_DIRECTORY);
        if (Files.exists(resultsDirectoryPath)) {
            ProjectUtils.deleteDirectory(resultsDirectoryPath);
        }
    }

    @Test(description = "test method for printing results to console")
    void testPrintToConsole() throws IOException {
        List<Issue> issues = new ArrayList<>();
        ScanUtils.printToConsole(issues, printStream);
        String printLog = readOutput(true).trim();
        String expected = "[]";
        Assert.assertEquals(printLog, expected);
    }

    @Test(description = "test method for saving results to file when no directory is provided")
    void testSaveToDirectory() throws IOException {
        List<Issue> issues = new ArrayList<>();
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", validBalProject.toString());
        Project project = ProjectLoader.loadProject(validBalProject);
        System.setProperty("user.dir", userDir);
        Path resultsFile = ScanUtils.saveToDirectory(issues, project, null);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = "[]";
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for saving results to file when directory is provided")
    void testSaveToProvidedDirectory() throws IOException {
        List<Issue> issues = new ArrayList<>();
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", validBalProject.toString());
        Project project = ProjectLoader.loadProject(validBalProject);
        System.setProperty("user.dir", userDir);
        Path resultsFile = ScanUtils.saveToDirectory(issues, project, RESULTS_DIRECTORY);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = "[]";
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for creating html analysis report from analysis results")
    void testGenerateScanReport() throws IOException {
        List<Issue> issues = new ArrayList<>();
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", validBalProject.toString());
        Project project = ProjectLoader.loadProject(validBalProject);
        System.setProperty("user.dir", userDir);
        Path scanReportPath = ScanUtils.generateScanReport(issues, project, null);
        String result = Files.readString(scanReportPath, StandardCharsets.UTF_8).replace("\r\n", "\n");
        Path validationScanReportPath = testResources.resolve("command-outputs")
                .resolve("empty-issues-html-report.txt");
        String expected = Files.readString(validationScanReportPath, StandardCharsets.UTF_8).replace("\r\n",
                "\n");
        Assert.assertEquals(result, expected);
    }

    @Test(description =
            "test method for creating html analysis report from analysis results when directory is provided")
    void testGenerateScanReportToProvidedDirectory() throws IOException {
        List<Issue> issues = new ArrayList<>();
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", validBalProject.toString());
        Project project = ProjectLoader.loadProject(validBalProject);
        System.setProperty("user.dir", userDir);
        Path scanReportPath = ScanUtils.generateScanReport(issues, project, RESULTS_DIRECTORY);
        String result = Files.readString(scanReportPath, StandardCharsets.UTF_8).replace("\r\n", "\n");
        Path validationScanReportPath = testResources.resolve("command-outputs")
                .resolve("empty-issues-html-report.txt");
        String expected = Files.readString(validationScanReportPath, StandardCharsets.UTF_8).replace("\r\n",
                "\n");
        Assert.assertEquals(result, expected);
    }
}
