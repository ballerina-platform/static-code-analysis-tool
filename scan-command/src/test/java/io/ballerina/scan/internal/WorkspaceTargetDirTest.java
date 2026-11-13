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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tests where a workspace scan saves its reports with {@code --target-dir}: one directory for the whole workspace, as
 * {@code bal build} uses, with the results of each package under its path from the workspace root and the combined
 * results at the top.
 *
 * @since 0.14.0
 */
public class WorkspaceTargetDirTest extends BaseTest {
    private static final String SOURCE = """
            function getResult() returns int|error => 1;

            public function main() {
                int result = checkpanic getResult();
            }
            """;

    private Path tempRoot;
    private Path workspace;
    private Path outside;

    @BeforeClass
    void createWorkspace() throws IOException {
        tempRoot = Files.createTempDirectory("workspace-target-dir").toRealPath();
        workspace = tempRoot.resolve("ws");
        outside = tempRoot.resolve("ext");
        createPackage(workspace.resolve("pa"), "pa");
        createPackage(workspace.resolve("apps").resolve("pb"), "pb");
        createPackage(outside, "ext");
        createWorkspace(workspace, "pa", "apps/pb", "../ext");
    }

    @AfterClass
    void deleteWorkspace() {
        ProjectUtils.deleteDirectory(tempRoot);
    }

    @AfterMethod
    void deleteReports() throws IOException {
        List<Path> outputs = new ArrayList<>();
        try (Stream<Path> files = Files.walk(tempRoot)) {
            files.filter(file -> file.endsWith("target") || file.endsWith("out") || file.endsWith("abs-out"))
                    .forEach(outputs::add);
        }
        outputs.stream().filter(Files::exists).forEach(ProjectUtils::deleteDirectory);
    }

    @Test(description = "test an absolute --target-dir keeps the results of every package and the combined results")
    void testAbsoluteTargetDir() throws IOException {
        Path out = tempRoot.resolve("abs-out");
        scan("--target-dir=" + out);

        assertResults(out.resolve("report").resolve("scan_results.json"),
                List.of("apps/pb/main.bal", "pa/main.bal"), true);
        assertResults(out.resolve("pa").resolve("report").resolve("scan_results.json"), List.of("main.bal"), false);
        assertResults(out.resolve("apps").resolve("pb").resolve("report").resolve("scan_results.json"),
                List.of("main.bal"), false);
        assertResults(out.resolve("_external").resolve("test").resolve("ext").resolve("report")
                .resolve("scan_results.json"), List.of("main.bal"), false);
        Assert.assertFalse(Files.exists(workspace.resolve("target")));
    }

    @Test(description = "test a relative --target-dir is one directory under the workspace root")
    void testRelativeTargetDir() throws IOException {
        scan("--target-dir=out");

        Path out = workspace.resolve("out");
        assertResults(out.resolve("report").resolve("scan_results.json"),
                List.of("apps/pb/main.bal", "pa/main.bal"), true);
        assertResults(out.resolve("pa").resolve("report").resolve("scan_results.json"), List.of("main.bal"), false);
        assertResults(out.resolve("apps").resolve("pb").resolve("report").resolve("scan_results.json"),
                List.of("main.bal"), false);
        assertResults(out.resolve("_external").resolve("test").resolve("ext").resolve("report")
                .resolve("scan_results.json"), List.of("main.bal"), false);
        Assert.assertFalse(Files.exists(workspace.resolve("pa").resolve("out")),
                "a package does not get a directory of its own");
        Assert.assertFalse(Files.exists(outside.resolve("out")), "a package does not get a directory of its own");
    }

    @Test(description = "test the SARIF and HTML reports of an absolute --target-dir are kept apart")
    void testAbsoluteTargetDirWithSarifAndScanReport() throws IOException {
        Path out = tempRoot.resolve("abs-out");
        scan("--target-dir=" + out, "--format=sarif", "--scan-report");

        Path combined = out.resolve("report").resolve("scan_results.sarif");
        Assert.assertEquals(sarifResultCount(combined), 3, "the combined report has every package's issues");
        for (Path packageDir : List.of(out.resolve("pa"), out.resolve("apps").resolve("pb"),
                out.resolve("_external").resolve("test").resolve("ext"))) {
            Assert.assertEquals(sarifResultCount(packageDir.resolve("report").resolve("scan_results.sarif")), 1,
                    "each package keeps its own report: " + packageDir);
        }
        Assert.assertTrue(Files.isRegularFile(out.resolve("report").resolve("index.html")));
        Assert.assertTrue(readOutput(true).contains(out.resolve("report").resolve("index.html").toUri().toString()));
    }

    @Test(description = "test the reports of a workspace scan without --target-dir stay in each target directory")
    void testDefaultTargetDirs() throws IOException {
        scan();

        assertResults(workspace.resolve("target").resolve("report").resolve("scan_results.json"),
                List.of("apps/pb/main.bal", "pa/main.bal"), true);
        assertResults(workspace.resolve("pa").resolve("target").resolve("report").resolve("scan_results.json"),
                List.of("main.bal"), false);
        assertResults(outside.resolve("target").resolve("report").resolve("scan_results.json"), List.of("main.bal"),
                false);
    }

    @Test(description = "test a package outside the workspace root is kept apart from an inside package whose "
            + "path is its name")
    void testOutsidePackageNamedLikeInsidePath() throws IOException {
        Path otherWorkspace = tempRoot.resolve("ws-util");
        createPackage(otherWorkspace.resolve("util"), "inner_util");
        createPackage(tempRoot.resolve("shared").resolve("util"), "util");
        createWorkspace(otherWorkspace, "util", "../shared/util");
        Path out = tempRoot.resolve("abs-out");

        Assert.assertEquals(scan(otherWorkspace, "--target-dir=" + out), 0, readOutput(true));

        assertResults(out.resolve("util").resolve("report").resolve("scan_results.json"), List.of("main.bal"), false);
        assertResults(out.resolve("_external").resolve("test").resolve("util").resolve("report")
                .resolve("scan_results.json"), List.of("main.bal"), false);
        JsonArray innerIssues = JsonParser.parseString(Files.readString(out.resolve("util").resolve("report")
                .resolve("scan_results.json"), StandardCharsets.UTF_8)).getAsJsonArray();
        Assert.assertTrue(innerIssues.get(0).getAsJsonObject().get("filePath").getAsString()
                .endsWith(Path.of("ws-util", "util", "main.bal").toString()), innerIssues.toString());
    }

    @Test(description = "test two packages that would save their results to the same directory fail the scan before "
            + "anything is saved")
    void testReportDirConflict() throws IOException {
        Path otherWorkspace = tempRoot.resolve("ws-conflict");
        createPackage(otherWorkspace.resolve("_external").resolve("test").resolve("lib"), "lib_copy");
        createPackage(tempRoot.resolve("lib"), "lib");
        createWorkspace(otherWorkspace, "_external/test/lib", "../lib");
        Path out = tempRoot.resolve("abs-out");

        int exitCode = scan(otherWorkspace, "--target-dir=" + out);
        String output = readOutput(true);
        Assert.assertEquals(exitCode, 1, output);
        // The packages are named in the order they are prepared, which follows the dependency graph
        String conflict = " would save their results to the same directory "
                + out.resolve("_external").resolve("test").resolve("lib")
                + "; move one of them, or scan without --target-dir";
        String inside = "_external/test/lib";
        String outsideRoot = tempRoot.resolve("lib").toString();
        Assert.assertTrue(output.contains("bal scan: error: " + inside + " and " + outsideRoot + conflict)
                || output.contains("bal scan: error: " + outsideRoot + " and " + inside + conflict), output);
        Assert.assertFalse(Files.exists(out), "nothing is saved");

        Assert.assertEquals(scan(otherWorkspace), 0, "the packages are scanned without --target-dir");
    }

    private void scan(String... args) {
        Assert.assertEquals(scan(workspace, args), 0);
    }

    private int scan(Path workspaceRoot, String... args) {
        System.setProperty("user.dir", workspaceRoot.toString());
        try {
            ScanCmd scanCmd = new ScanCmd(printStream);
            List<String> allArgs = new ArrayList<>(List.of(args));
            allArgs.add("--include-rules=ballerina:1");
            new CommandLine(scanCmd).parseArgs(allArgs.toArray(String[]::new));
            scanCmd.execute();
            return scanCmd.exitCode();
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private static void createWorkspace(Path root, String... packages) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("Ballerina.toml"), "[workspace]\npackages = [" + String.join(", ",
                Stream.of(packages).map(path -> "\"" + path + "\"").toList()) + "]\n");
        WorkspaceProject project = (WorkspaceProject) ProjectLoader.load(root).project();
        for (BuildProject buildProject : project.projects()) {
            buildProject.currentPackage().getResolution();
            buildProject.save();
        }
    }

    /**
     * Asserts the file paths of the issues in a JSON results file. A package outside the workspace root is given by
     * its file name in the combined results, so only the paths inside the root are compared there.
     */
    private static void assertResults(Path resultsFile, List<String> filePaths, boolean combined) throws IOException {
        Assert.assertTrue(Files.isRegularFile(resultsFile), "missing " + resultsFile);
        JsonArray issues = JsonParser.parseString(Files.readString(resultsFile, StandardCharsets.UTF_8))
                .getAsJsonArray();
        List<String> actual = new ArrayList<>();
        for (JsonElement issue : issues) {
            actual.add(issue.getAsJsonObject().getAsJsonObject("location").get("filePath").getAsString());
        }
        if (combined) {
            Assert.assertEquals(actual.size(), 3, resultsFile + ": " + actual);
            actual = actual.stream().filter(filePath -> filePath.contains("/")).toList();
        }
        Assert.assertEquals(actual, filePaths, resultsFile.toString());
    }

    private static int sarifResultCount(Path sarifFile) throws IOException {
        JsonObject sarif = JsonParser.parseString(Files.readString(sarifFile, StandardCharsets.UTF_8))
                .getAsJsonObject();
        return sarif.getAsJsonArray("runs").get(0).getAsJsonObject().getAsJsonArray("results").size();
    }

    private static void createPackage(Path root, String name) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"" + name + "\"\nversion = \"0.1.0\"\n");
        Files.writeString(root.resolve("main.bal"), SOURCE);
    }
}
