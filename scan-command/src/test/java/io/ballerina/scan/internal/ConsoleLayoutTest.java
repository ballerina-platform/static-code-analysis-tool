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

import io.ballerina.scan.BaseTest;
import io.ballerina.scan.internal.ConsoleLayout.Section;
import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
                bal scan: warning: generating reports is not supported with single bal files, ignoring the flag and \
                continuing the scans
                bal scan: warning: generating scan reports is not supported with single bal files, ignoring the flag \
                and continuing the scans

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
                Usage: bal scan [OPTIONS] [<package>|<source-file>]
                Try 'bal scan --help' for more information.
                """);
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
}
