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
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.ScanReportTestUtils;
import io.ballerina.scan.Source;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.diagnostics.Location;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tests that the reports list issues in the same order whatever order they were found in, as the modules and
 * packages of a project are not always scanned in the same order.
 *
 * @since 0.11.2
 */
public class IssueOrderTest extends BaseTest {
    private static final int SHUFFLES = 20;

    private Path projectRoot;
    private final Rule rule1 = RuleFactory.createRule(1, "rule 1", RuleKind.CODE_SMELL);
    private final Rule rule2 = RuleFactory.createRule(2, "rule 2", RuleKind.CODE_SMELL);
    private final Rule rule10 = RuleFactory.createRule(10, "rule 10", RuleKind.CODE_SMELL);
    private final Rule externalRule = RuleFactory.createRule(1, "external rule 1", RuleKind.BUG, "exampleOrg",
            "exampleName");
    private Project project;
    private Path reportDir;
    // In the order the reports must list them
    private List<Issue> sortedIssues;
    private Map<Issue, SuppressedIssue> suppressions;

    @BeforeClass
    void createIssues() throws IOException {
        projectRoot = Files.createTempDirectory("issue-order-project").toRealPath();
        Files.writeString(projectRoot.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"issue_order\"\nversion = \"0.1.0\"\n");
        String source = "public function main() {\n" + "    int value = 1;\n".repeat(12) + "}\n";
        Files.writeString(projectRoot.resolve("main.bal"), source);
        Files.writeString(projectRoot.resolve("rotation.bal"), "function rotate() {\n}\n");
        Path vault = Files.createDirectories(projectRoot.resolve("modules").resolve("vault"));
        Files.writeString(vault.resolve("vault.bal"), "public function read() {\n}\n");
        project = ProjectLoader.loadProject(projectRoot);
        reportDir = Files.createTempDirectory("issue-order");
        sortedIssues = List.of(
                issue("main.bal", 2, 0, 2, 5, rule2),
                issue("main.bal", 2, 0, 2, 5, rule10),
                issue("main.bal", 2, 0, 2, 5, externalRule),
                issue("main.bal", 2, 0, 2, 9, rule1),
                issue("main.bal", 2, 0, 3, 1, rule1),
                issue("main.bal", 2, 4, 2, 5, rule1),
                issue("main.bal", 10, 0, 10, 1, rule1),
                issue("modules/vault/vault.bal", 1, 0, 1, 1, rule1),
                issue("rotation.bal", 0, 0, 0, 1, rule1));
        suppressions = Map.of(sortedIssues.get(1), new SuppressedIssue(sortedIssues.get(1), List.of(), "reviewed"));
    }

    @AfterClass
    void deleteReportDir() {
        ProjectUtils.deleteDirectory(reportDir);
        ProjectUtils.deleteDirectory(projectRoot);
    }

    @Test(description = "test the JSON report lists issues by file, position and rule ID, comparing rule IDs by "
            + "number")
    void testJsonOrder() {
        JsonArray issues = JsonParser.parseString(ScanUtils.convertIssuesToJsonString(shuffled(new Random(1)),
                project, suppressions)).getAsJsonArray();
        List<String> keys = new ArrayList<>();
        for (JsonElement element : issues) {
            JsonObject issue = element.getAsJsonObject();
            JsonObject location = issue.getAsJsonObject("location");
            keys.add(location.get("filePath").getAsString() + ":" + location.get("startLine").getAsInt() + ":"
                    + location.get("startColumn").getAsInt() + "-" + location.get("endLine").getAsInt() + ":"
                    + location.get("endColumn").getAsInt() + " " + issue.getAsJsonObject("rule").get("id")
                    .getAsString());
        }
        Assert.assertEquals(keys, List.of(
                "main.bal:3:1-3:6 ballerina:2",
                "main.bal:3:1-3:6 ballerina:10",
                "main.bal:3:1-3:6 exampleOrg/exampleName:1",
                "main.bal:3:1-3:10 ballerina:1",
                "main.bal:3:1-4:2 ballerina:1",
                "main.bal:3:5-3:6 ballerina:1",
                "main.bal:11:1-11:2 ballerina:1",
                "modules/vault/vault.bal:2:1-2:2 ballerina:1",
                "rotation.bal:1:1-1:2 ballerina:1"));
        Assert.assertTrue(issues.get(1).getAsJsonObject().has("suppressions"),
                "a suppressed issue keeps its place: " + issues);
    }

    @Test(description = "test the JSON report is the same whatever order the issues are found in")
    void testJsonIsDeterministic() {
        String expected = ScanUtils.convertIssuesToJsonString(sortedIssues, project, suppressions);
        Random random = new Random(42);
        for (int i = 0; i < SHUFFLES; i++) {
            Assert.assertEquals(ScanUtils.convertIssuesToJsonString(shuffled(random), project, suppressions),
                    expected);
        }
    }

    @Test(description = "test the SARIF report lists its results and rules in the same order whatever order the "
            + "issues are found in")
    void testSarifIsDeterministic() throws IOException {
        String expected = sarif(sortedIssues);
        JsonObject run = JsonParser.parseString(expected).getAsJsonObject().getAsJsonArray("runs").get(0)
                .getAsJsonObject();
        List<String> resultRuleIds = new ArrayList<>();
        run.getAsJsonArray("results").forEach(result -> resultRuleIds.add(result.getAsJsonObject().get("ruleId")
                .getAsString()));
        Assert.assertEquals(resultRuleIds, sortedIssues.stream().map(issue -> issue.rule().id()).toList());
        List<String> ruleIds = new ArrayList<>();
        run.getAsJsonObject("tool").getAsJsonObject("driver").getAsJsonArray("rules")
                .forEach(rule -> ruleIds.add(rule.getAsJsonObject().get("id").getAsString()));
        Assert.assertEquals(ruleIds, List.of("ballerina:2", "ballerina:10", "exampleOrg/exampleName:1",
                "ballerina:1"));

        Random random = new Random(7);
        for (int i = 0; i < SHUFFLES; i++) {
            Assert.assertEquals(sarif(shuffled(random)),
                    expected);
        }
    }

    @Test(description = "test the HTML report lists its files by path and their issues in order whatever order "
            + "the issues are found in")
    void testScanReportIsDeterministic() throws IOException {
        JsonObject expected = scanReportData(sortedIssues);
        List<String> files = new ArrayList<>();
        for (JsonElement file : expected.getAsJsonArray("scannedFiles")) {
            files.add(file.getAsJsonObject().get("relativePath").getAsString());
        }
        Assert.assertEquals(files, List.of("main.bal", "modules/vault/vault.bal", "rotation.bal"));
        JsonArray mainIssues = expected.getAsJsonArray("scannedFiles").get(0).getAsJsonObject()
                .getAsJsonArray("issues");
        List<String> mainRuleIds = new ArrayList<>();
        mainIssues.forEach(issue -> mainRuleIds.add(issue.getAsJsonObject().get("ruleID").getAsString()));
        Assert.assertEquals(mainRuleIds, List.of("ballerina:2", "ballerina:10", "exampleOrg/exampleName:1",
                "ballerina:1", "ballerina:1", "ballerina:1", "ballerina:1"));

        Random random = new Random(3);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(scanReportData(shuffled(random)), expected);
        }
    }

    // The SARIF converter that takes suppressions is private on this line, so the report is saved and read back
    private String sarif(List<Issue> issues) throws IOException {
        Path report = ScanUtils.saveSarifToDirectory(issues, project, reportDir.toString(), suppressions);
        return Files.readString(report, StandardCharsets.UTF_8);
    }

    @Test(description = "test an issue of another Issue implementation is ordered by the file name of its location")
    void testOtherIssueImplementations() {
        Issue second = otherIssue("b.bal");
        Issue first = otherIssue("a.bal");
        Assert.assertEquals(ScanUtils.inReportOrder(List.of(second, first)), List.of(first, second));
    }

    private Issue otherIssue(String fileName) {
        Location location = new BLangDiagnosticLocation(fileName, 0, 0, 0, 1, 0, 1);
        return new Issue() {
            @Override
            public Location location() {
                return location;
            }

            @Override
            public Rule rule() {
                return rule1;
            }

            @Override
            public Source source() {
                return Source.EXTERNAL;
            }
        };
    }

    private JsonObject scanReportData(List<Issue> issues) throws IOException {
        Path report = ScanUtils.generateScanReport(issues, project, reportDir.toString(), suppressions);
        return ScanReportTestUtils.readScanReportData(report);
    }

    private List<Issue> shuffled(Random random) {
        List<Issue> issues = new ArrayList<>(sortedIssues);
        Collections.shuffle(issues, random);
        return issues;
    }

    private Issue issue(String relativePath, int startLine, int startColumn, int endLine, int endColumn, Rule rule) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        BLangDiagnosticLocation location = new BLangDiagnosticLocation(fileName, startLine, endLine, startColumn,
                endColumn, 0, 1);
        Source source = rule == externalRule ? Source.EXTERNAL : Source.BUILT_IN;
        return new IssueImpl(location, rule, source, fileName, projectRoot.resolve(relativePath).toString());
    }
}
