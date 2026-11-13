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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Verifies the location the Ballerina JSON and SARIF reports give for an issue: one-based lines and columns, the
 * {@code charOffset}/{@code length}, and the path of the file relative to the root of the project, or its file
 * name when it has no path within the root.
 *
 * @since 0.1.0
 */
public class RegionConversionTest extends BaseTest {
    // A scan gives absolute document paths, as a project stores its root as an absolute path
    private final Path testResourcesDir = testResources.resolve("test-resources").toAbsolutePath();
    private final Path balProject = testResourcesDir.resolve("bal-project-with-analyzer-configurations");

    @Test(description = "test position/region conversion for a one-character region")
    void testOneCharacterRegion() {
        JsonObject[] locations = locations(new BLangDiagnosticLocation("main.bal", 5, 5, 10, 11, 100, 1),
                balProject.resolve("main.bal"), balProject);
        for (JsonObject location : locations) {
            assertRegion(location, 6, 11, 6, 12);
        }
        Assert.assertEquals(locations[0].get("startOffset").getAsInt(), 100);
        Assert.assertEquals(locations[0].get("length").getAsInt(), 1);
        Assert.assertEquals(locations[1].get("charOffset").getAsInt(), 100);
        Assert.assertEquals(locations[1].get("charLength").getAsInt(), 1);
    }

    @Test(description = "test position/region conversion for a zero-width insertion-point region")
    void testZeroWidthInsertionRegion() {
        JsonObject[] locations = locations(new BLangDiagnosticLocation("main.bal", 5, 5, 10, 10, 100, 0),
                balProject.resolve("main.bal"), balProject);
        for (JsonObject location : locations) {
            assertRegion(location, 6, 11, 6, 11);
        }
        Assert.assertEquals(locations[0].get("length").getAsInt(), 0);
        Assert.assertEquals(locations[1].get("charLength").getAsInt(), 0);
    }

    @Test(description = "test position/region conversion for a multi-line region")
    void testMultiLineRegion() {
        JsonObject[] locations = locations(new BLangDiagnosticLocation("main.bal", 5, 8, 10, 3, 100, 50),
                balProject.resolve("main.bal"), balProject);
        for (JsonObject location : locations) {
            assertRegion(location, 6, 11, 9, 4);
        }
    }

    @Test(description = "test the file path of an issue in a non-default module is relative to the package root")
    void testModuleFilePath() {
        Path moduleProject = testResourcesDir.resolve("bal-project-with-secret-tracing");
        assertFilePath(new BLangDiagnosticLocation("vault.bal", 0, 0, 0, 1, 0, 1),
                moduleProject.resolve("modules").resolve("vault").resolve("vault.bal"), moduleProject,
                "modules/vault/vault.bal");
    }

    @Test(description = "test the file path of an issue in a workspace package is relative to the workspace root")
    void testWorkspaceFilePath() {
        Path workspace = testResourcesDir.resolve("workspace-project");
        assertFilePath(new BLangDiagnosticLocation("main.bal", 0, 0, 0, 1, 0, 1),
                workspace.resolve("bal-project-with-analyzer-configurations").resolve("main.bal"), workspace,
                "bal-project-with-analyzer-configurations/main.bal");
    }

    @Test(description = "test the file path of an issue in a single file project is its file name")
    void testSingleFilePath() {
        Path file = testResourcesDir.resolve("core-rules").resolve("rule001_rule_checkpanic.bal");
        assertFilePath(new BLangDiagnosticLocation("rule001_rule_checkpanic.bal", 0, 0, 0, 1, 0, 1), file, file,
                "rule001_rule_checkpanic.bal");
    }

    @Test(description = "test the file path of a document with no path on disk, which is only its name, is its "
            + "file name")
    void testRelativeFilePath() {
        assertFilePath(new BLangDiagnosticLocation("main.bal", 0, 0, 0, 1, 0, 1), Path.of("main.bal"), balProject,
                "main.bal");
    }

    @Test(description = "test a relative path is not resolved against the working directory, even when it would "
            + "point inside the project root")
    void testRelativePathInsideRootFromWorkingDirectory() {
        Path moduleProject = testResourcesDir.resolve("bal-project-with-secret-tracing");
        Path relativeModuleFile = Path.of("").toAbsolutePath()
                .relativize(moduleProject.resolve("modules").resolve("vault").resolve("vault.bal"));
        assertFilePath(new BLangDiagnosticLocation("vault.bal", 0, 0, 0, 1, 0, 1), relativeModuleFile,
                moduleProject, "vault.bal");
    }

    @Test(description = "test the file path of a file outside the project root is its file name")
    void testFileOutsideRoot() {
        Path otherProject = testResourcesDir.resolve("bal-project-with-secret-tracing");
        assertFilePath(new BLangDiagnosticLocation("main.bal", 0, 0, 0, 1, 0, 1),
                balProject.resolve("main.bal"), otherProject, "main.bal");
    }

    @Test(description = "test the deprecated JSON overloads without a project give one-based positions and the file "
            + "name")
    @SuppressWarnings("deprecation")
    void testDeprecatedJsonOverloads() {
        Issue issue = new IssueImpl(new BLangDiagnosticLocation("vault.bal", 5, 5, 10, 11, 100, 1),
                CoreRule.AVOID_CHECKPANIC.rule(), Source.BUILT_IN, "vault.bal",
                testResourcesDir.resolve("bal-project-with-secret-tracing").resolve("modules").resolve("vault")
                        .resolve("vault.bal").toString());
        for (String json : List.of(ScanUtils.convertIssuesToJsonString(List.of(issue)),
                ScanUtils.convertIssuesToJsonString(List.of(issue), Map.of()))) {
            JsonObject location = JsonParser.parseString(json).getAsJsonArray().get(0).getAsJsonObject()
                    .getAsJsonObject("location");
            assertRegion(location, 6, 11, 6, 12);
            Assert.assertEquals(location.get("filePath").getAsString(), "vault.bal");
        }
    }

    private void assertFilePath(BLangDiagnosticLocation location, Path filePath, Path projectPath,
                                String expectedPath) {
        JsonObject[] locations = locations(location, filePath, projectPath);
        Assert.assertEquals(locations[0].get("filePath").getAsString(), expectedPath);
        Assert.assertEquals(locations[1].get("uri").getAsString(), expectedPath);
    }

    private static void assertRegion(JsonObject location, int startLine, int startColumn, int endLine,
                                     int endColumn) {
        Assert.assertEquals(location.get("startLine").getAsInt(), startLine);
        Assert.assertEquals(location.get("startColumn").getAsInt(), startColumn);
        Assert.assertEquals(location.get("endLine").getAsInt(), endLine);
        Assert.assertEquals(location.get("endColumn").getAsInt(), endColumn);
    }

    /**
     * Returns the location of the issue in the Ballerina JSON report, and its SARIF region with the artifact
     * {@code uri} added.
     */
    private static JsonObject[] locations(BLangDiagnosticLocation location, Path filePath, Path projectPath) {
        Issue issue = new IssueImpl(location, CoreRule.AVOID_CHECKPANIC.rule(), Source.BUILT_IN,
                location.lineRange().fileName(), filePath.toString());
        Project project = ProjectLoader.load(projectPath).project();
        String json = ScanUtils.convertIssuesToJsonString(List.of(issue), project, Map.of());
        JsonObject jsonLocation = JsonParser.parseString(json).getAsJsonArray().get(0).getAsJsonObject()
                .getAsJsonObject("location");

        String sarif = ScanUtils.convertIssuesToSarifString(List.of(issue), project);
        JsonArray sarifResults = JsonParser.parseString(sarif).getAsJsonObject().getAsJsonArray("runs")
                .get(0).getAsJsonObject().getAsJsonArray("results");
        JsonObject physicalLocation = sarifResults.get(0).getAsJsonObject().getAsJsonArray("locations").get(0)
                .getAsJsonObject().getAsJsonObject("physicalLocation");
        JsonObject sarifRegion = physicalLocation.getAsJsonObject("region").deepCopy();
        sarifRegion.add("uri", physicalLocation.getAsJsonObject("artifactLocation").get("uri"));
        return new JsonObject[]{jsonLocation, sarifRegion};
    }
}
