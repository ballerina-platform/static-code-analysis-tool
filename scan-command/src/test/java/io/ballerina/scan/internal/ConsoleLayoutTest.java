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

import com.google.gson.JsonParser;
import io.ballerina.projects.Project;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.internal.ConsoleLayout.Section;
import io.ballerina.scan.utils.AnalyzedSources;
import io.ballerina.scan.utils.ScanTomlFile;
import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiFunction;

import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;

/**
 * Tests the layout of the console output: every scan prints its sections in the same order, separated by exactly
 * one blank line, and skips the sections that do not apply.
 *
 * @since 0.13.0
 */
public class ConsoleLayoutTest extends BaseTest {
    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path gateIssuesProject = testResourcesRoot.resolve("bal-project-with-gate-issues");

    @Test(description = "test sections are separated by one blank line, and empty sections leave none")
    void testSections() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream console = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        ConsoleLayout layout = new ConsoleLayout();
        PrintStream out = layout.track(console);
        PrintStream err = layout.track(console);

        layout.enter(Section.LOADING);
        err.println("loading a");
        layout.enter(Section.LOADING);
        err.println("loading b");
        layout.enter(Section.WARNINGS);
        layout.enter(Section.RUNNING);
        err.println("running");
        layout.enter(Section.RESULTS);
        out.println("[]");
        layout.enter(Section.SUMMARY);
        err.println("0 issue(s)");

        Assert.assertEquals(normalize(bytes.toString(StandardCharsets.UTF_8)),
                "loading a\nloading b\n\nrunning\n\n[]\n\n0 issue(s)\n");
    }

    @Test(description = "test the output never starts with a blank line")
    void testNoLeadingBlankLine() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleLayout layout = new ConsoleLayout();
        PrintStream err = layout.track(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        layout.enter(Section.RESOLVING);
        layout.enter(Section.FAILURE);
        err.println("bal scan: error: failed");
        Assert.assertEquals(normalize(bytes.toString(StandardCharsets.UTF_8)), "bal scan: error: failed\n");
    }

    @Test(description = "test output that already ends with a blank line gets no second one")
    void testNoDoubleBlankLine() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleLayout layout = new ConsoleLayout();
        PrintStream out = layout.track(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        layout.enter(Section.PLATFORMS);
        out.println("Reporting issues to: examplePlatform");
        layout.enter(Section.PLATFORM_OUTPUT);
        out.println("uploaded");
        out.println();
        layout.enter(Section.FAILURE);
        out.println("bal scan: error: failed");
        Assert.assertEquals(normalize(bytes.toString(StandardCharsets.UTF_8)),
                "Reporting issues to: examplePlatform\n\nuploaded\n\nbal scan: error: failed\n");
    }

    @Test(description = "test the blank lines between sections are written to the separator stream only")
    void testSeparatorStream() {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        PrintStream outConsole = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream errConsole = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        ConsoleLayout layout = new ConsoleLayout();
        PrintStream out = layout.track(outConsole);
        PrintStream err = layout.track(errConsole);
        layout.separateWith(errConsole);

        layout.enter(Section.LOADING);
        err.println("loading");
        layout.enter(Section.RESULTS);
        out.println("[]");
        layout.enter(Section.SUMMARY);
        err.println("0 issue(s)");

        Assert.assertEquals(normalize(outBytes.toString(StandardCharsets.UTF_8)), "[]\n");
        Assert.assertEquals(normalize(errBytes.toString(StandardCharsets.UTF_8)), "loading\n\n\n0 issue(s)\n");
    }

    @Test(description = "test standard output holds only the results when it is separate from standard error")
    void testSplitStreams() {
        ScanStreams run = scanSplit(gateIssuesProject);
        Assert.assertTrue(run.out().startsWith("["), "no blank line before the results: " + run.out());
        Assert.assertFalse(JsonParser.parseString(run.out()).getAsJsonArray().isEmpty(), run.out());
        // The two blank lines around the results, which went to standard output
        Assert.assertTrue(run.err().startsWith("Running Scans\n\n\n6 issue(s)\n"), run.err());

        run = scanSplit(gateIssuesProject, "--list-rules");
        Assert.assertFalse(run.out().startsWith("\n") || run.out().startsWith("\r"),
                "bal scan --list-rules > rules.txt starts with the table: " + run.out());
        Assert.assertTrue(run.out().contains("ballerina:1"), run.out());
        Assert.assertTrue(run.err().endsWith("\n\n"), "the separator before the table: " + run.err());
    }

    @Test(description = "test held warnings are printed, separated, before a failure")
    void testFailureAfterHeldWarnings() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        Files.writeString(scanToml, """
                [gate]
                failOnSeverity = "LOW"
                """);
        try {
            String output = scan(gateIssuesProject, "--fail-on-severity=HIGH", "--include-rules=ballerina:99");
            Assert.assertEquals(skeleton(output), """
                    Loading scan tool configurations from <PATH>

                    bal scan: warning: --fail-on-severity=HIGH overrides [gate] failOnSeverity = "LOW" in Scan.toml

                    bal scan: error: unknown rule 'ballerina:99'
                      Run 'bal scan --list-rules' to see the available rules.
                    Usage: bal scan [OPTIONS] [<workspace>|<package>|<source-file>]
                    Try 'bal scan --help' for more information.
                    """, output);
        } finally {
            Files.deleteIfExists(scanToml);
        }
    }

    @Test(description = "test workspace warnings are held through the loading and running blocks")
    void testWorkspaceWarningsLayout() throws IOException {
        Path workspace = testResourcesRoot.resolve("workspace-with-gates");
        Path packageScanToml = workspace.resolve("pkg-a").resolve("Scan.toml");
        Path workspaceScanToml = workspace.resolve("Scan.toml");
        String original = Files.readString(packageScanToml, StandardCharsets.UTF_8);
        try {
            // pkg-a uses the workspace file, which names a rule pkg-a lacks; pkg-b's own file leaves it out
            Files.delete(packageScanToml);
            Files.writeString(workspaceScanToml, """
                    [rule]
                    exclude = ["exampleOrg/example_module_static_code_analyzer:1"]
                    """);
            String output = scan(workspace);
            Assert.assertEquals(withoutWarnings(skeleton(output)), """
                    Resolving workspace dependencies

                    Loading scan tool configurations for the workspace from Scan.toml
                    Using the workspace scan tool configurations for pkg-a
                    Loading scan tool configurations for pkg-b from pkg-b/Scan.toml

                    <WARNING>

                    Running Scans
                    Running scans on pkg-a
                    Running scans on pkg-b

                    <WARNING>

                    Combining the results of 2 packages

                    <ISSUES>

                    <SUMMARY>

                    View scan results at:
                    \t<URI>
                    """, output);
        } finally {
            Files.deleteIfExists(workspaceScanToml);
            Files.writeString(packageScanToml, original);
        }
    }

    @Test(description = "test the platform output is a section of its own, followed by the gate result")
    void testPlatformsLayout() throws IOException {
        Path project = testResourcesRoot.resolve("bal-project-with-platform-configurations");
        withPlatformScanToml(project, "", () -> {
            String output = scan(project, "--platforms=examplePlatform", "--fail-on-severity=INFO");
            Assert.assertEquals(skeleton(output).replaceAll("(?m)^bal scan: error: .*$", "<GATE>"), """
                    Loading scan tool configurations from <PATH>

                    Running Scans

                    <ISSUES>

                    <SUMMARY>

                    View scan results at:
                    \t<URI>

                    Reporting issues to: examplePlatform

                    uploaded by examplePlatform

                    <GATE>
                    """, output);
        });
    }

    @Test(description = "test the output of a platform is printed to standard error, so the results stay valid JSON")
    void testPlatformOutputIsPrintedToStandardError() throws IOException {
        Path project = testResourcesRoot.resolve("bal-project-with-platform-configurations");
        withPlatformScanToml(project, "", () -> {
            ScanStreams streams = scanSplit(project, "--platforms=examplePlatform");
            Assert.assertFalse(streams.out().contains("uploaded by examplePlatform"), streams.out());
            Assert.assertTrue(JsonParser.parseString(streams.out()).isJsonArray(), streams.out());
            Assert.assertTrue(streams.err().contains("uploaded by examplePlatform\n"), streams.err());
        });
    }

    @Test(description = "test anything printed to System.out while the scan runs, such as by a compiler plugin, is "
            + "printed to standard error, and System.out is restored after the scan")
    void testSystemOutIsPrintedToStandardError() {
        PrintStream systemOut = System.out;
        BiFunction<PrintStream, PrintStream, ScanCmd> printingCommand = (out, err) -> new ScanCmd(out, err) {
            @Override
            protected ProjectAnalyzer getProjectAnalyzer(Project project, ScanTomlFile scanTomlFile,
                                                         AnalyzedSources analyzedSources) {
                PrintStream pluginOut = System.out;
                pluginOut.println("printed to System.out by a plugin");
                return super.getProjectAnalyzer(project, scanTomlFile, analyzedSources);
            }
        };
        ScanStreams streams = scanSplit(testResourcesRoot.resolve("bal-project-with-tests"), printingCommand);
        Assert.assertTrue(JsonParser.parseString(streams.out()).isJsonArray(), streams.out());
        Assert.assertTrue(streams.err().contains("printed to System.out by a plugin\n"), streams.err());
        Assert.assertSame(System.out, systemOut);

        ScanStreams failed = scanSplit(testResourcesRoot.resolve("bal-project-with-tests"), printingCommand,
                "--include-rules=ballerina:9999");
        Assert.assertTrue(failed.err().contains("bal scan: error:"), failed.err());
        Assert.assertSame(System.out, systemOut);
    }

    @Test(description = "test a scan that writes to one stream, as the test runner does, leaves System.out alone")
    void testSingleStreamScanLeavesSystemOut() {
        PrintStream systemOut = System.out;
        PrintStream[] outDuringScan = new PrintStream[1];
        System.setProperty("user.dir", testResourcesRoot.resolve("bal-project-with-tests").toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream) {
                @Override
                protected ProjectAnalyzer getProjectAnalyzer(Project project, ScanTomlFile scanTomlFile,
                                                             AnalyzedSources analyzedSources) {
                    outDuringScan[0] = System.out;
                    return super.getProjectAnalyzer(project, scanTomlFile, analyzedSources);
                }
            };
            scanCmd.execute();
        } finally {
            System.setProperty("user.dir", userDir);
        }
        Assert.assertSame(outDuringScan[0], systemOut);
        Assert.assertSame(System.out, systemOut);
    }

    @Test(description = "test a platform-triggered scan prints its held warnings before the summary")
    void testPlatformTriggeredLayout() throws IOException {
        Path project = testResourcesRoot.resolve("bal-project-with-tests");
        withPlatformScanToml(project, """
                [[analyzer]]
                org = "exampleOrg"
                name = "example_module_static_code_analyzer"

                """, () -> {
            String output = scan(project, "--platform-triggered");
            Assert.assertEquals(withoutWarnings(skeleton(output)), """
                    Loading scan tool configurations from <PATH>

                    Running Scans

                    <WARNING>

                    <SUMMARY>

                    Reporting issues to: examplePlatform

                    uploaded by examplePlatform

                    """, output);
        });
    }

    @Test(description = "test the layout of a package scan with warnings, a report and a failing gate")
    void testPackageLayout() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        Files.writeString(scanToml, """
                [rule]
                exclude = ["ballerina:7"]

                [gate]
                failOnSeverity = "LOW"
                """);
        try {
            String output = scan(gateIssuesProject, "--exclude-rules=ballerina:1", "--fail-on-severity=HIGH",
                    "--scan-report");
            Assert.assertEquals(skeleton(output), """
                    Loading scan tool configurations from <PATH>

                    bal scan: warning: --fail-on-severity=HIGH overrides [gate] failOnSeverity = "LOW" in Scan.toml

                    Running Scans

                    <ISSUES>

                    <SUMMARY>

                    View scan results at:
                    \t<URI>

                    View scan report at:
                    \t<URI>

                    bal scan: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)
                    """, output);
        } finally {
            Files.deleteIfExists(scanToml);
        }
    }

    @Test(description = "test the layout of a workspace scan")
    void testWorkspaceLayout() throws IOException {
        String output = scan(testResourcesRoot.resolve("workspace-with-gates"), "--scan-report");
        Assert.assertEquals(skeleton(output), """
                Resolving workspace dependencies

                Loading scan tool configurations for pkg-a from pkg-a/Scan.toml
                Loading scan tool configurations for pkg-b from pkg-b/Scan.toml

                Running Scans
                Running scans on pkg-a
                Running scans on pkg-b

                Combining the results of 2 packages

                <ISSUES>

                <SUMMARY>

                View scan results at:
                \t<URI>

                View scan report at:
                \t<URI>

                bal scan: error: 1 package failed the gate
                pkg-a: error: 1 issue at or above HIGH, limit 1 (HIGH: 1)
                """, output);
    }

    @Test(description = "test the layout of a single file scan, whose warnings come before the scan")
    void testSingleFileLayout() throws IOException {
        Path singleFile = testResourcesRoot.resolve("valid-single-file-project").resolve("main.bal");
        String output = scan(testResourcesRoot, singleFile.toAbsolutePath().toString(), "--target-dir=out",
                "--scan-report");
        Assert.assertEquals(skeleton(output), """
                bal scan: warning: reports are not saved for a single bal file, ignoring --target-dir
                bal scan: warning: the HTML report is not generated for a single bal file, ignoring --scan-report

                Running Scans

                <ISSUES>

                <SUMMARY>
                """, output);
    }

    @Test(description = "test --platforms on a single file is rejected as not supported")
    void testPlatformsOnSingleFile() throws IOException {
        Path singleFile = testResourcesRoot.resolve("valid-single-file-project").resolve("main.bal");
        System.setProperty("user.dir", testResourcesRoot.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        try {
            new CommandLine(scanCmd).parseArgs(singleFile.toAbsolutePath().toString(), "--platforms=sonarqube");
            scanCmd.execute();
        } finally {
            System.setProperty("user.dir", userDir);
        }
        String output = readOutput(true);
        Assert.assertEquals(scanCmd.exitCode(), 2, output);
        Assert.assertEquals(output, """
                bal scan: error: reporting to platforms is not supported with single bal files; scan a package to \
                use --platforms
                Usage: bal scan [OPTIONS] [<workspace>|<package>|<source-file>]
                Try 'bal scan --help' for more information.
                """);
    }

    @Test(description = "test --platforms on a single file is a usage error before the file is loaded")
    void testPlatformsOnSingleFileBeforeLoading() throws IOException {
        Path directory = Files.createTempDirectory("scan-platforms");
        try {
            Path emptyFile = Files.writeString(directory.resolve("empty.bal"), "");
            Path brokenFile = Files.writeString(directory.resolve("broken.bal"), "public function main( {");
            for (Path file : new Path[]{emptyFile, brokenFile}) {
                System.setProperty("user.dir", directory.toString());
                ScanCmd scanCmd = new ScanCmd(printStream, printStream);
                try {
                    new CommandLine(scanCmd).parseArgs(file.toString(), "--platforms=sonarqube");
                    scanCmd.execute();
                } finally {
                    System.setProperty("user.dir", userDir);
                }
                String output = readOutput(true);
                Assert.assertEquals(scanCmd.exitCode(), 2, output);
                Assert.assertTrue(output.startsWith("bal scan: error: reporting to platforms is not supported with "
                        + "single bal files"), output);
            }
        } finally {
            ProjectUtils.deleteDirectory(directory);
        }
    }

    @Test(description = "test --list-rules with --platforms on a single file lists the rules")
    void testListRulesWithPlatformsOnSingleFile() throws IOException {
        Path singleFile = testResourcesRoot.resolve("valid-single-file-project").resolve("main.bal");
        System.setProperty("user.dir", testResourcesRoot.toString());
        ScanCmd scanCmd = new ScanCmd(printStream, printStream);
        try {
            new CommandLine(scanCmd).parseArgs("--list-rules", "--platforms=x", singleFile.toAbsolutePath().toString());
            scanCmd.execute();
        } finally {
            System.setProperty("user.dir", userDir);
        }
        String output = readOutput(true);
        Assert.assertEquals(scanCmd.exitCode(), 0, output);
        Assert.assertTrue(output.contains("ballerina:1"), output);
    }

    @Test(description = "test held warnings are printed when the scan ends with an Error")
    void testWarningsPrintedOnError() throws IOException {
        Path scanToml = gateIssuesProject.resolve("Scan.toml");
        Files.writeString(scanToml, """
                [gate]
                failOnSeverity = "LOW"
                """);
        System.setProperty("user.dir", gateIssuesProject.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream) {
                @Override
                protected ProjectAnalyzer getProjectAnalyzer(Project project, ScanTomlFile scanTomlFile,
                                                             AnalyzedSources analyzedSources) {
                    throw new NoClassDefFoundError("io/example/MissingClass");
                }
            };
            new CommandLine(scanCmd).parseArgs("--fail-on-severity=HIGH");
            Assert.assertThrows(NoClassDefFoundError.class, scanCmd::execute);
        } finally {
            System.setProperty("user.dir", userDir);
            Files.deleteIfExists(scanToml);
        }
        String output = readOutput(true);
        Assert.assertTrue(output.contains("bal scan: warning: --fail-on-severity=HIGH overrides [gate] failOnSeverity"),
                output);
    }

    /**
     * Runs a scan from the given directory, and returns everything it printed.
     */
    private String scan(Path directory, String... args) throws IOException {
        System.setProperty("user.dir", directory.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream, printStream);
            new CommandLine(scanCmd).parseArgs(args);
            scanCmd.execute();
            return readOutput(true);
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    /**
     * Runs a scan from the given directory with separate standard output and standard error streams.
     */
    private ScanStreams scanSplit(Path directory, String... args) {
        return scanSplit(directory, ScanCmd::new, args);
    }

    private ScanStreams scanSplit(Path directory, BiFunction<PrintStream, PrintStream, ScanCmd> command,
                                  String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setProperty("user.dir", directory.toString());
        try (PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
             PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            ScanCmd scanCmd = command.apply(outStream, errStream);
            new CommandLine(scanCmd).parseArgs(args);
            scanCmd.execute();
        } finally {
            System.setProperty("user.dir", userDir);
        }
        return new ScanStreams(normalize(out.toString(StandardCharsets.UTF_8)),
                normalize(err.toString(StandardCharsets.UTF_8)));
    }

    /**
     * Runs a test with a Scan.toml that declares the example platform, which prints a message and a blank line.
     */
    private void withPlatformScanToml(Path project, String otherTables, ThrowingRunnable test) throws IOException {
        Path pluginJar = Objects.requireNonNull(Path.of(userDir).toAbsolutePath().getParent())
                .resolve("test-static-code-analysis-platform-plugins")
                .resolve("exampleOrg-static-code-analysis-platform-plugin").resolve("build").resolve("libs")
                .resolve("exampleOrg-static-code-analysis-platform-plugin.jar");
        Path scanToml = project.resolve("Scan.toml");
        String original = Files.exists(scanToml) ? Files.readString(scanToml, StandardCharsets.UTF_8) : null;
        Files.writeString(scanToml, otherTables + """
                [[platform]]
                name = "examplePlatform"
                path = "<PLUGIN_JAR>"
                consoleMessage = "uploaded by examplePlatform"
                """.replace("<PLUGIN_JAR>", pluginJar.toString().replace("\\", "\\\\")));
        try {
            test.run();
        } finally {
            if (original == null) {
                Files.deleteIfExists(scanToml);
            } else {
                Files.writeString(scanToml, original);
            }
            // The example platform saves what it receives in the working directory
            Files.deleteIfExists(project.resolve("analysis-issues.json"));
            Files.deleteIfExists(project.resolve("platform-arguments.json"));
        }
    }

    private static String withoutWarnings(String output) {
        return output.replaceAll("(?m)^.*warning: .*$", "<WARNING>");
    }

    /**
     * Replaces the parts of the output that vary between runs and machines with placeholders.
     */
    private static String skeleton(String output) {
        return normalize(output)
                .replaceAll("(?ms)^\\[$.*?^]$|^\\[]$", "<ISSUES>")
                .replaceAll("(?m)^\\d+ issue\\(s\\).*$", "<SUMMARY>")
                .replaceAll("(?m)^(Loading scan tool configurations from ).*$", "$1<PATH>")
                .replaceAll("(?m)^\\tfile:.*$", "\t<URI>");
    }

    private static String normalize(String output) {
        return output.replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
    }

    private record ScanStreams(String out, String err) {
    }

    private interface ThrowingRunnable {
        void run() throws IOException;
    }
}
