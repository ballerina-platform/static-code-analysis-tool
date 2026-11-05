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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.SingleFileProject;
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
import java.util.Set;

import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.scan.ScanReportTestUtils.readScanReportData;
import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;

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
        String expected = "[]";
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test method for saving results to file when no directory is provided")
    void testSaveToDirectory() throws IOException {
        List<Issue> issues = new ArrayList<>();
        Project project = ProjectLoader.loadProject(validBalProject);
        Path resultsFile = ScanUtils.saveToDirectory(issues, project, null);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = "[]";
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for saving results to file when directory is provided")
    void testSaveToProvidedDirectory() throws IOException {
        List<Issue> issues = new ArrayList<>();
        Project project = ProjectLoader.loadProject(validBalProject);
        Path resultsFile = ScanUtils.saveToDirectory(issues, project, RESULTS_DIRECTORY);
        String result = Files.readString(resultsFile, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String expected = "[]";
        Assert.assertEquals(result, expected);
    }

    @Test(description = "test method for creating html analysis report from analysis results")
    void testGenerateScanReport() throws IOException {
        List<Issue> issues = new ArrayList<>();
        Project project = ProjectLoader.loadProject(validBalProject);
        Path scanReportPath = ScanUtils.generateScanReport(issues, project, null);
        assertEmptyScanReport(scanReportPath);
    }

    @Test(description =
            "test method for creating html analysis report from analysis results when directory is provided")
    void testGenerateScanReportToProvidedDirectory() throws IOException {
        List<Issue> issues = new ArrayList<>();
        Project project = ProjectLoader.loadProject(validBalProject);
        Path scanReportPath = ScanUtils.generateScanReport(issues, project, RESULTS_DIRECTORY);
        Assert.assertTrue(scanReportPath.toAbsolutePath().normalize()
                        .startsWith(validBalProject.resolve(RESULTS_DIRECTORY).toAbsolutePath().normalize()),
                "Report was not written to the provided directory: " + scanReportPath);
        assertEmptyScanReport(scanReportPath);
    }

    // Files are listed even when they have no issues, so a clean project still shows every analyzed file
    private void assertEmptyScanReport(Path scanReportPath) throws IOException {
        JsonObject scanData = readScanReportData(scanReportPath);
        Assert.assertEquals(scanData.get("projectName").getAsString(), "valid_bal_project");
        Assert.assertEquals(scanData.get("projectVersion").getAsString(), "0.1.0");
        Assert.assertEquals(scanData.get("projectKind").getAsString(), "BUILD_PROJECT");
        JsonArray packages = scanData.getAsJsonArray("packages");
        Assert.assertEquals(packages.size(), 1);
        JsonObject scannedPackage = packages.get(0).getAsJsonObject();
        Assert.assertEquals(scannedPackage.get("org").getAsString(), "ballerina");
        Assert.assertEquals(scannedPackage.get("name").getAsString(), "valid_bal_project");
        Assert.assertEquals(scannedPackage.get("version").getAsString(), "0.1.0");
        Assert.assertEquals(scannedPackage.get("path").getAsString(), "");
        JsonArray scannedFiles = scanData.getAsJsonArray("scannedFiles");
        Assert.assertEquals(scannedFiles.size(), 1);
        JsonObject scannedFile = scannedFiles.get(0).getAsJsonObject();
        Assert.assertEquals(scannedFile.get("fileName").getAsString(), "main.bal");
        Assert.assertEquals(scannedFile.get("relativePath").getAsString(), "main.bal");
        Assert.assertEquals(scannedFile.get("packageName").getAsString(), "valid_bal_project");
        Assert.assertEquals(scannedFile.get("fileContent").getAsString(),
                Files.readString(validBalProject.resolve("main.bal"), StandardCharsets.UTF_8));
        Assert.assertTrue(scannedFile.getAsJsonArray("issues").isEmpty());
    }

    @Test(description =
            "test method for loading configurations from a Scan.toml file in a Ballerina single file project")
    void testloadScanTomlConfigurationsForSingleFileProject() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("single-file-project-with-config-file").resolve("main.bal");
        Project project = SingleFileProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNotNull(scanTomlFile);
    }

    @Test(description = "test method for loading configurations from a Scan.toml file")
    void testloadScanTomlConfigurations() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNotNull(scanTomlFile);;
        Set<ScanTomlFile.Analyzer> analyzers = scanTomlFile.getAnalyzers();
        List<ScanTomlFile.Analyzer> analyzerList = new ArrayList<>(analyzers);
        Assert.assertEquals(analyzerList.size(), 4);
        ScanTomlFile.Analyzer analyzer = analyzerList.get(0);
        assertAnalyzer(analyzer, "exampleOrg", "example_module_static_code_analyzer", null, null);
        analyzer = analyzerList.get(1);
        assertAnalyzer(analyzer, "ballerina", "example_module_static_code_analyzer", "0.1.0", null);
        analyzer = analyzerList.get(2);
        assertAnalyzer(analyzer, "ballerinax", "example_module_static_code_analyzer", "0.1.0", LOCAL_REPOSITORY_NAME);
        Set<ScanTomlFile.RuleToFilter> rulesToInclude = scanTomlFile.getRulesToInclude();
        List<ScanTomlFile.RuleToFilter> ruleToIncludeList = new ArrayList<>(rulesToInclude);
        Assert.assertEquals(ruleToIncludeList.size(), 4);
        ScanTomlFile.RuleToFilter ruleToInclude = ruleToIncludeList.get(0);
        Assert.assertEquals(ruleToInclude.id(), "ballerina:1");
        ruleToInclude = ruleToIncludeList.get(1);
        Assert.assertEquals(ruleToInclude.id(), "exampleOrg/example_module_static_code_analyzer:1");
        ruleToInclude = ruleToIncludeList.get(2);
        Assert.assertEquals(ruleToInclude.id(), "ballerina/example_module_static_code_analyzer:1");
        ruleToInclude = ruleToIncludeList.get(3);
        Assert.assertEquals(ruleToInclude.id(), "ballerinax/example_module_static_code_analyzer:1");
        Set<ScanTomlFile.RuleToFilter> rulesToExclude = scanTomlFile.getRulesToExclude();
        List<ScanTomlFile.RuleToFilter> ruleToExcludeList = new ArrayList<>(rulesToExclude);
        Assert.assertEquals(ruleToExcludeList.size(), 1);
        ScanTomlFile.RuleToFilter ruleToExclude = ruleToExcludeList.get(0);
        Assert.assertEquals(ruleToExclude.id(), "ballerina:1");
        Set<ScanTomlFile.Platform> platforms = scanTomlFile.getPlatforms();
        Assert.assertEquals(platforms.size(), 0);
    }

    @Test(description =
            "test method for loading configurations from a Scan.toml file with invalid platform JAR")
    void testloadInvalidPlatformJAR() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-platform-jar");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        // The JAR is downloaded only when the platform is selected, so loading Scan.toml succeeds
        Assert.assertNotNull(scanTomlFile);
        ScanTomlFile.Platform platform = scanTomlFile.getPlatforms().iterator().next();
        Assert.assertEquals(platform.path(), "https://example.com/example-platform-analyzer-1.0.jar");
        ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                () -> ScanUtils.resolvePlatformJar(platform, project.targetDir(), printStream));
        Assert.assertEquals(exception.getMessage(), "failed to download remote jar file: 'examplePlatform.jar'");
    }

    @Test(description = "test a platform path that is neither a file nor a URL fails only when the platform is used")
    void testloadInvalidPlatformScanTomlConfigurations() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-platform-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNotNull(scanTomlFile);
        ScanTomlFile.Platform platform = scanTomlFile.getPlatforms().iterator().next();
        String jarPath = ballerinaProject.toAbsolutePath().resolve("www.example.com/example-platform-analyzer-1.0.jar")
                .normalize().toString();
        Assert.assertEquals(platform.path(), jarPath);
        ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                () -> ScanUtils.resolvePlatformJar(platform, project.targetDir(), printStream));
        Assert.assertEquals(exception.getMessage(), DiagnosticLog.error(DiagnosticCode.PLATFORM_FILE_NOT_FOUND,
                "examplePlatform", "path", jarPath));
    }

    @Test(description =
            "test method for loading configurations from a Scan.toml file with invalid Ballerina.toml configuration")
    void testloadScanTomlConfigurationsWithInvalidBallerinaTomlConfiguration() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-file-configuration");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNull(scanTomlFile);
        String expected = DiagnosticLog.error(DiagnosticCode.MISSING_CONFIG_FIELD);
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(description = "test method for loading a Scan.toml file with a suppression setting that is not a boolean")
    void testLoadScanTomlConfigurationsWithNonBooleanSuppressionSetting() {
        assertSuppressionError("bal-project-with-invalid-suppression-settings", "Scan.toml:2:17: error: "
                + DiagnosticLog.error(DiagnosticCode.INVALID_SUPPRESSION_SETTING, "requireReason"));
    }

    @Test(description = "test method for loading a Scan.toml file whose suppression settings are not a table")
    void testLoadScanTomlConfigurationsWithSuppressionTableArray() {
        assertSuppressionError("bal-project-with-suppression-table-array", "Scan.toml:1:1: error: "
                + DiagnosticLog.error(DiagnosticCode.INVALID_SUPPRESSION_TABLE));
    }

    @Test(description = "test method for loading a Scan.toml file with an unknown suppression setting")
    void testLoadScanTomlConfigurationsWithUnknownSuppressionSetting() {
        assertSuppressionError("bal-project-with-unknown-suppression-setting", "Scan.toml:2:1: error: "
                + DiagnosticLog.error(DiagnosticCode.UNKNOWN_SUPPRESSION_SETTING, "requireReasons"));
    }

    // bal scan reports the error at its position with the other errors of the file, and the editor ignores it
    private void assertSuppressionError(String projectName, String expectedError) {
        Path ballerinaProject = testResources.resolve("test-resources").resolve(projectName);
        Project project = ProjectLoader.loadProject(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        try {
            ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                    () -> ScanUtils.readScanTomlConfigurations(project, printStream));
            Assert.assertEquals(exception.getMessage(),
                    DiagnosticLog.error(DiagnosticCode.CONFIG_FILE_HAS_ERRORS, "Scan.toml", 1));
            Assert.assertEquals(exception.diagnostics(), List.of(expectedError));
            Assert.assertTrue(ScanUtils.loadScanTomlConfigurations(project, printStream).isPresent());
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    @Test(description = "test method for loading configurations from an external configuration file")
    void testloadExternalScanTomlConfigurations() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-external-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNotNull(scanTomlFile);
        Set<ScanTomlFile.Analyzer> analyzers = scanTomlFile.getAnalyzers();
        List<ScanTomlFile.Analyzer> analyzerList = new ArrayList<>(analyzers);
        Assert.assertEquals(analyzerList.size(), 4);
        ScanTomlFile.Analyzer analyzer = analyzerList.get(0);
        assertAnalyzer(analyzer, "exampleOrg", "example_module_static_code_analyzer", null, null);
        analyzer = analyzerList.get(1);
        assertAnalyzer(analyzer, "ballerina", "example_module_static_code_analyzer", "0.1.0", null);
        analyzer = analyzerList.get(2);
        assertAnalyzer(analyzer, "ballerinax", "example_module_static_code_analyzer", "0.1.0", LOCAL_REPOSITORY_NAME);
        Set<ScanTomlFile.RuleToFilter> rulesToInclude = scanTomlFile.getRulesToInclude();
        List<ScanTomlFile.RuleToFilter> ruleToIncludeList = new ArrayList<>(rulesToInclude);
        Assert.assertEquals(ruleToIncludeList.size(), 4);
        ScanTomlFile.RuleToFilter ruleToInclude = ruleToIncludeList.get(0);
        Assert.assertEquals(ruleToInclude.id(), "ballerina:1");
        ruleToInclude = ruleToIncludeList.get(1);
        Assert.assertEquals(ruleToInclude.id(), "exampleOrg/example_module_static_code_analyzer:1");
        ruleToInclude = ruleToIncludeList.get(2);
        Assert.assertEquals(ruleToInclude.id(), "ballerina/example_module_static_code_analyzer:1");
        ruleToInclude = ruleToIncludeList.get(3);
        Assert.assertEquals(ruleToInclude.id(), "ballerinax/example_module_static_code_analyzer:1");
        Set<ScanTomlFile.RuleToFilter> rulesToExclude = scanTomlFile.getRulesToExclude();
        List<ScanTomlFile.RuleToFilter> ruleToExcludeList = new ArrayList<>(rulesToExclude);
        Assert.assertEquals(ruleToExcludeList.size(), 1);
        ScanTomlFile.RuleToFilter ruleToExclude = ruleToExcludeList.get(0);
        Assert.assertEquals(ruleToExclude.id(), "ballerina:1");
        Set<ScanTomlFile.Platform> platforms = scanTomlFile.getPlatforms();
        Assert.assertEquals(platforms.size(), 0);
    }

    @Test(description = "test method for loading configurations from an invalid external configuration file")
    void testloadInvalidExternalScanTomlConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-external-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        String expected = DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_CONFIG_FILE, "no protocol: ./Config.toml");
        Assert.assertNull(scanTomlFile);
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    @Test(enabled = false, // the remote toml location is not available due to the missing bot account
            description = "test method for loading configurations from a remote configuration file")
    void testloadRemoteScanTomlConfigurations() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-remote-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        Assert.assertNotNull(scanTomlFile);
        Set<ScanTomlFile.Analyzer> analyzers = scanTomlFile.getAnalyzers();
        List<ScanTomlFile.Analyzer> analyzerList = new ArrayList<>(analyzers);
        Assert.assertEquals(analyzerList.size(), 4);
        ScanTomlFile.Analyzer analyzer = analyzerList.get(0);
        assertAnalyzer(analyzer, "exampleOrg", "example_module_static_code_analyzer", null, null);
        analyzer = analyzerList.get(1);
        assertAnalyzer(analyzer, "ballerina", "example_module_static_code_analyzer", "0.1.0", null);
        analyzer = analyzerList.get(2);
        assertAnalyzer(analyzer, "ballerinax", "example_module_static_code_analyzer", "0.1.0", LOCAL_REPOSITORY_NAME);
        Set<ScanTomlFile.RuleToFilter> rulesToInclude = scanTomlFile.getRulesToInclude();
        List<ScanTomlFile.RuleToFilter> ruleToIncludeList = new ArrayList<>(rulesToInclude);
        Assert.assertEquals(ruleToIncludeList.size(), 4);
        ScanTomlFile.RuleToFilter ruleToInclude = ruleToIncludeList.get(0);
        Assert.assertEquals(ruleToInclude.id(), "ballerina:1");
        ruleToInclude = ruleToIncludeList.get(1);
        Assert.assertEquals(ruleToInclude.id(), "exampleOrg/example_module_static_code_analyzer:1");
        ruleToInclude = ruleToIncludeList.get(2);
        Assert.assertEquals(ruleToInclude.id(), "ballerina/example_module_static_code_analyzer:1");
        ruleToInclude = ruleToIncludeList.get(3);
        Assert.assertEquals(ruleToInclude.id(), "ballerinax/example_module_static_code_analyzer:1");
        Set<ScanTomlFile.RuleToFilter> rulesToExclude = scanTomlFile.getRulesToExclude();
        List<ScanTomlFile.RuleToFilter> ruleToExcludeList = new ArrayList<>(rulesToExclude);
        Assert.assertEquals(ruleToExcludeList.size(), 1);
        ScanTomlFile.RuleToFilter ruleToExclude = ruleToExcludeList.get(0);
        Assert.assertEquals(ruleToExclude.id(), "ballerina:1");
        Set<ScanTomlFile.Platform> platforms = scanTomlFile.getPlatforms();
        Assert.assertEquals(platforms.size(), 0);
    }

    @Test(description = "test method for loading configurations from an invalid remote configuration file")
    void testloadInvalidRemoteScanTomlConfigurations() throws IOException {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-invalid-remote-config-file");
        Project project = BuildProject.load(ballerinaProject);
        System.setProperty("user.dir", ballerinaProject.toString());
        ScanTomlFile scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream).orElse(null);
        System.setProperty("user.dir", userDir);
        String expected = DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_CONFIG_FILE,
                "no protocol: www.example.com/Config.toml");
        Assert.assertNull(scanTomlFile);
        Assert.assertEquals(readOutput(true).trim(), expected);
    }

    private void assertAnalyzer(ScanTomlFile.Analyzer analyzer, String org, String name, String version,
                                String repository) {
        Assert.assertEquals(analyzer.org(), org);
        Assert.assertEquals(analyzer.name(), name);
        Assert.assertEquals(analyzer.version(), version);
        Assert.assertEquals(analyzer.repository(), repository);
    }
}
