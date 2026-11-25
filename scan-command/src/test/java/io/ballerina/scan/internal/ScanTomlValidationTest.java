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

import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.utils.GateSeverity;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Tests for the validation of the tables and keys of a Scan.toml file.
 *
 * @since 0.11.2
 */
public class ScanTomlValidationTest extends BaseTest {
    private static final String TABLES = "[[analyzer]], [[platform]], [rule], [suppression], [gate] or [analysis]";
    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path project = testResourcesRoot.resolve("bal-project-with-scan-toml-errors");
    private final Path scanToml = project.resolve("Scan.toml");

    @AfterMethod(alwaysRun = true)
    void restoreFiles() throws IOException {
        Files.deleteIfExists(scanToml);
    }

    @DataProvider(name = "invalid-scan-tomls")
    public Object[][] invalidScanTomls() {
        return new Object[][]{
                // A misspelled table turns its settings off
                {"""
                        [gates]
                        failOnCount = 1
                        """, List.of("Scan.toml:1:1: error: unknown table 'gates'; expected " + TABLES)},
                {"""
                        [rules]
                        include = ["ballerina:1"]
                        """, List.of("Scan.toml:1:1: error: unknown table 'rules'; expected " + TABLES)},
                {"""
                        [[gates]]
                        failOnCount = 1
                        """, List.of("Scan.toml:1:1: error: unknown table 'gates'; expected " + TABLES)},
                {"""
                        foo = 1
                        """, List.of("Scan.toml:1:1: error: unknown key 'foo'; expected " + TABLES)},
                {"""
                        foo = { a = 1 }
                        """, List.of("Scan.toml:1:1: error: unknown key 'foo'; expected " + TABLES)},
                {"""
                        analyzer = [1]
                        """, List.of("Scan.toml:1:1: error: 'analyzer' must be written as [[analyzer]] tables, or as "
                        + "analyzer = []")},
                {"""
                        [platform]
                        name = "examplePlatform"
                        path = "plugin.jar"
                        """, List.of("Scan.toml:1:1: error: 'platform' must be written as [[platform]] tables, or as "
                        + "platform = []")},
                // A name or path of another type would otherwise read as missing
                {"""
                        [[platform]]
                        name = "examplePlatform"
                        path = 10
                        """, List.of("Scan.toml:3:8: error: invalid value '10' for 'path'; expected the path or URL of "
                        + "the plugin JAR")},
                {"""
                        [[platform]]
                        name = 10
                        path = "plugin.jar"
                        """, List.of("Scan.toml:2:8: error: invalid value '10' for 'name'; expected a platform name, "
                        + "such as \"sonarqube\"")},
                {"""
                        [[platform]]
                        name = "examplePlatform"
                        path = { jar = "plugin.jar" }
                        """, List.of("Scan.toml:3:8: error: invalid value '{jar=plugin.jar}' for 'path'; expected the "
                        + "path or URL of the plugin JAR")},
                {"""
                        [[platform]]
                        name = { id = "examplePlatform" }
                        path = "plugin.jar"
                        """, List.of("Scan.toml:2:8: error: invalid value '{id=examplePlatform}' for 'name'; expected "
                        + "a platform name, such as \"sonarqube\"")},
                {"""
                        platform = "plugin.jar"
                        """, List.of("Scan.toml:1:1: error: 'platform' must be written as [[platform]] tables, or as "
                        + "platform = []")},
                {"""
                        platform = ["plugin.jar"]
                        """, List.of("Scan.toml:1:1: error: 'platform' must be written as [[platform]] tables, or as "
                        + "platform = []")},
        };
    }

    @Test(description = "test an unknown table or key and a table written in the wrong form are Scan.toml errors",
            dataProvider = "invalid-scan-tomls")
    void testInvalidScanToml(String content, List<String> errors) throws IOException {
        Files.writeString(scanToml, content);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: Scan.toml has " + errors.size()
                + (errors.size() == 1 ? " error\n" : " errors\n") + String.join("\n", errors));
        Assert.assertFalse(run.output().contains("Running Scans"), run.output());
    }

    @Test(description = "test every error of a Scan.toml file is reported at once, in the order of the file")
    void testAllErrorsReportedAtOnce() throws IOException {
        Files.writeString(scanToml, """
                foo = 1

                [gates]
                failOnCount = 1

                [gate]
                failOnCount = 0
                """);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), """
                bal scan: error: Scan.toml has 3 errors
                Scan.toml:1:1: error: unknown key 'foo'; expected\s""" + TABLES + "\n"
                + "Scan.toml:3:1: error: unknown table 'gates'; expected " + TABLES + "\n"
                + "Scan.toml:7:15: error: invalid value '0' for 'failOnCount'; expected a whole number of 1 or more");
    }

    @Test(description = "test every documented table and key is accepted, with the extra keys of [[platform]]")
    void testValidScanToml() throws IOException {
        Files.writeString(scanToml, """
                [rule]
                include = ["ballerina:1", "exampleOrg/example_module_static_code_analyzer:1"]

                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"

                [[analyzer]]
                org = "ballerinax"
                name = "example_module_static_code_analyzer"
                version = "0.1.0"
                repository = "local"

                [[platform]]
                name = "examplePlatform"
                path = "https://example.com/example-platform-plugin.jar"
                sonarProjectPropertiesPath = "sonar-project.properties"
                token = 10

                [[platform]]
                name = "noPathPlatform"

                [suppression]
                enabled = true
                requireReason = false
                requireRuleId = false

                [gate]
                failOnSeverity = "NONE"
                failOnCount = 5

                [analysis]
                excludeTests = false
                """);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertFalse(run.output().contains("error:"), run.output());
        Assert.assertFalse(run.output().contains("warning:"), run.output());

        Files.writeString(scanToml, """
                analyzer = []
                platform = []
                """);
        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, run.output());
    }

    @Test(description = "test --list-rules does not fail on an unknown table, which does not change the rules")
    void testListRulesWithUnknownTable() throws IOException {
        Files.writeString(scanToml, """
                [gates]
                failOnCount = 1
                """);
        ScanRun run = scan(project, "--list-rules");
        Assert.assertEquals(run.exitCode(), 0, run.output());
        Assert.assertTrue(run.output().contains("ballerina:1 "), run.output());
    }

    @Test(description = "test the editor loads a Scan.toml with an unknown table leniently")
    void testLenientLoadingWithUnknownTable() throws IOException {
        Files.writeString(scanToml, """
                [gates]
                failOnCount = 1

                [gate]
                failOnSeverity = "HIGH"
                """);
        Project loaded = ProjectLoader.loadProject(project);
        // The Scan.toml file of a package is found in the working directory
        System.setProperty("user.dir", project.toString());
        try {
            Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(loaded, printStream);
            Assert.assertTrue(scanTomlFile.isPresent());
            Assert.assertEquals(scanTomlFile.get().getFailOnSeverity(), Optional.of(GateSeverity.HIGH));
            ScanToolException exception = Assert.expectThrows(ScanToolException.class,
                    () -> ScanUtils.readScanTomlConfigurations(loaded, printStream));
            Assert.assertEquals(exception.diagnostics(), List.of("Scan.toml:1:1: error: unknown table 'gates'; "
                    + "expected " + TABLES));
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    @DataProvider(name = "invalid-analyzers")
    public Object[][] invalidAnalyzers() {
        return new Object[][]{
                // An entry that cannot be used would otherwise be left out, and its rules with it
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer"
                        version = "latest"
                        bogus = 1

                        [[analyzer]]
                        org = 10
                        name = ""
                        repository = "central"
                        """, List.of(
                        "Scan.toml:4:11: error: invalid value 'latest' for 'version'; expected a version, such as "
                                + "\"1.0.0\"",
                        "Scan.toml:5:1: error: unknown key 'bogus' in [[analyzer]]; expected org, name, version or "
                                + "repository",
                        "Scan.toml:8:7: error: invalid value '10' for 'org'; expected an organization name, such as "
                                + "\"ballerina\"",
                        "Scan.toml:9:8: error: invalid value '' for 'name'; expected a package name, such as "
                                + "\"example_analyzer\"",
                        "Scan.toml:10:14: error: invalid value 'central' for 'repository'; expected \"local\"")},
                {"""
                        [[analyzer]]
                        nmae = "example_module_static_code_analyzer"
                        org = "exampleOrg"

                        [[analyzer]]
                        org = "ballerinax"
                        name = "example_module_static_code_analyzer"
                        repository = "local"
                        """, List.of(
                        "Scan.toml:1:1: error: 'name' is required in [[analyzer]]",
                        "Scan.toml:2:1: error: unknown key 'nmae' in [[analyzer]]; expected org, name, version or "
                                + "repository",
                        "Scan.toml:5:1: error: 'version' is required in [[analyzer]] with repository = \"local\"")},
        };
    }

    @Test(description = "test an [[analyzer]] entry that cannot be used is a Scan.toml error, also for --list-rules",
            dataProvider = "invalid-analyzers")
    void testInvalidAnalyzer(String content, List<String> errors) throws IOException {
        Files.writeString(scanToml, content);
        String expected = "bal scan: error: Scan.toml has " + errors.size() + " errors\n" + String.join("\n", errors);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), expected);

        // The rules it would list depend on the analyzers
        run = scan(project, "--list-rules");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), expected);
    }

    @DataProvider(name = "unresolved-analyzers")
    public Object[][] unresolvedAnalyzers() {
        return new Object[][]{
                {"""
                        [[analyzer]]
                        org = "nosuchorgxyz"
                        name = "nosuchpkg"
                        """, "Scan.toml:1:1: error: analyzer 'nosuchorgxyz/nosuchpkg' cannot be resolved"},
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer"
                        version = "9.9.9"
                        """, "Scan.toml:1:1: error: analyzer 'exampleOrg/example_module_static_code_analyzer:9.9.9' "
                        + "cannot be resolved"},
                {"""
                        [[analyzer]]
                        org = "ballerinax"
                        name = "example_module_static_code_analyzer"
                        version = "9.9.9"
                        repository = "local"
                        """, "Scan.toml:1:1: error: analyzer 'ballerinax/example_module_static_code_analyzer:9.9.9 "
                        + "from the local repository' cannot be resolved: Dependency version (9.9.9) cannot be found "
                        + "in the local repository. org: `ballerinax` name: example_module_static_code_analyzer"},
                // A package whose name starts with that of a resolved package is not one of its modules
                {"""
                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer"

                        [[analyzer]]
                        org = "exampleOrg"
                        name = "example_module_static_code_analyzer.typo"
                        """, "Scan.toml:5:1: error: analyzer 'exampleOrg/example_module_static_code_analyzer.typo' "
                        + "cannot be resolved"},
                {"""
                        [[analyzer]]
                        org = "ballerina"
                        name = "bal_project_with_scan_toml_errors"
                        """, "Scan.toml:1:1: error: analyzer 'ballerina/bal_project_with_scan_toml_errors' cannot be "
                        + "resolved: it is the package being scanned"},
        };
    }

    @Test(description = "test an [[analyzer]] that cannot be resolved fails the scan instead of being skipped",
            dataProvider = "unresolved-analyzers")
    void testUnresolvedAnalyzer(String content, String error) throws IOException {
        Files.writeString(scanToml, content);
        ScanRun run = scan(project);
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: Scan.toml has 1 error\n" + error);
        Assert.assertFalse(run.output().contains("Running Scans"), run.output());

        run = scan(project, "--list-rules");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: Scan.toml has 1 error\n" + error);
    }

    @Test(description = "test an analyzer version that does not exist fails although an older version is locked")
    void testMissingAnalyzerVersionWithLockedVersion() throws IOException {
        Path lockedProject = Files.createTempDirectory("project-with-locked-analyzer").resolve("locked_analyzer");
        try {
            writePackage(lockedProject, "locked_analyzer", """
                    import exampleOrg/example_module_static_code_analyzer as _;

                    public function main() {
                    }
                    """, lockedPackage("locked_analyzer", """
                    dependencies = [
                    \t{org = "exampleOrg", name = "example_module_static_code_analyzer"}
                    ]
                    """) + """

                    [[package]]
                    org = "exampleOrg"
                    name = "example_module_static_code_analyzer"
                    version = "0.1.0"
                    modules = [
                    \t{org = "exampleOrg", packageName = "example_module_static_code_analyzer", \
                    moduleName = "example_module_static_code_analyzer"}
                    ]
                    """);
            Files.writeString(lockedProject.resolve("Scan.toml"), """
                    [[analyzer]]
                    org = "exampleOrg"
                    name = "example_module_static_code_analyzer"
                    version = "9.9.9"
                    repository = "local"
                    """);
            ScanRun run = scan(lockedProject);
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertEquals(run.errorReport(), """
                    bal scan: error: Scan.toml has 1 error
                    Scan.toml:1:1: error: analyzer 'exampleOrg/example_module_static_code_analyzer:9.9.9 from the \
                    local repository' cannot be resolved: Dependency version (9.9.9) cannot be found in the local \
                    repository. org: `exampleOrg` name: example_module_static_code_analyzer""");
        } finally {
            ProjectUtils.deleteDirectory(lockedProject.getParent());
        }
    }

    private static void writePackage(Path root, String name, String source, String lockedPackages)
            throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("Ballerina.toml"), """
                [package]
                org = "ballerina"
                name = "<name>"
                version = "0.1.0"
                """.replace("<name>", name));
        Files.writeString(root.resolve("Dependencies.toml"), """
                [ballerina]
                dependencies-toml-version = "2"
                distribution-version = "2201.12.0"
                """ + lockedPackages);
        Files.writeString(root.resolve("main.bal"), source);
    }

    private static String lockedPackage(String name, String dependencies) {
        return """

                [[package]]
                org = "ballerina"
                name = "<name>"
                version = "0.1.0"
                <dependencies>modules = [
                \t{org = "ballerina", packageName = "<name>", moduleName = "<name>"}
                ]
                """.replace("<name>", name).replace("<dependencies>", dependencies);
    }

    @Test(description = "test a platform named in --platforms whose [[platform]] has no path is reported as such")
    void testPlatformWithoutPath() throws IOException {
        Files.writeString(scanToml, """
                [[platform]]
                name = "examplePlatform"
                """);
        ScanRun run = scan(project, "--platforms=examplePlatform");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: platform 'examplePlatform' has no 'path' in the "
                + "[[platform]] table of Scan.toml; set it to the plugin JAR");

        run = scan(project, "--platforms=otherPlatform");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertEquals(run.errorReport(), "bal scan: error: platform 'otherPlatform' is not available; add its "
                + "plugin to the [[platform]] table of Scan.toml");

        // A platform without a path and one that is not declared are reported in the same run
        run = scan(project, "--platforms=examplePlatform,otherPlatform");
        Assert.assertEquals(run.exitCode(), 1, run.output());
        Assert.assertTrue(run.output().endsWith("""
                bal scan: error: platform 'otherPlatform' is not available; add its plugin to the [[platform]] table \
                of Scan.toml
                bal scan: error: platform 'examplePlatform' has no 'path' in the [[platform]] table of Scan.toml; set \
                it to the plugin JAR
                """), run.output());

        run = scan(project);
        Assert.assertEquals(run.exitCode(), 0, "a platform without a path is ignored until it is named: "
                + run.output());
    }

    @Test(description = "test a missing platform is reported against the configuration file set by configPath")
    void testMissingPlatformInCustomConfigPath() throws IOException {
        Path ballerinaToml = project.resolve("Ballerina.toml");
        String originalBallerinaToml = Files.readString(ballerinaToml);
        Path configDir = project.resolve("config");
        Path customScanToml = configDir.resolve("scan-settings.toml");
        try {
            Files.writeString(ballerinaToml, originalBallerinaToml + """

                    [scan]
                    configPath = "config/scan-settings.toml"
                    """);
            Files.createDirectories(configDir);
            Files.writeString(customScanToml, """
                    [[platform]]
                    name = "examplePlatform"
                    """);
            ScanRun run = scan(project, "--platforms=examplePlatform,otherPlatform");
            Assert.assertEquals(run.exitCode(), 1, run.output());
            Assert.assertTrue(run.output().endsWith("""
                    bal scan: error: platform 'otherPlatform' is not available; add its plugin to the [[platform]] \
                    table of config/scan-settings.toml
                    bal scan: error: platform 'examplePlatform' has no 'path' in the [[platform]] table of \
                    config/scan-settings.toml; set it to the plugin JAR
                    """), run.output());
        } finally {
            Files.writeString(ballerinaToml, originalBallerinaToml);
            Files.deleteIfExists(customScanToml);
            Files.deleteIfExists(configDir);
        }
    }

    private ScanRun scan(Path target, String... args) throws IOException {
        System.setProperty("user.dir", target.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream);
            new CommandLine(scanCmd).parseArgs(args);
            scanCmd.execute();
            return new ScanRun(scanCmd.exitCode(), readOutput(true));
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private record ScanRun(int exitCode, String output) {

        // The error report is the last thing written, so it runs from the last error line to the end
        String errorReport() {
            int start = output.lastIndexOf("bal scan: error: ");
            return start < 0 ? "" : output.substring(start).trim();
        }
    }
}
