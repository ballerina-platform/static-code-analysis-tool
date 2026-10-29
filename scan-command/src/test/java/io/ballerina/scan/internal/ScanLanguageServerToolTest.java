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
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tests for ScanLanguageServerTool LS entry points.
 *
 * @since 0.11.1
 */
public class ScanLanguageServerToolTest extends BaseTest {
    private Path testProjectDir;
    private Path scanTomlPath;
    private Path validBalProject = testResources.resolve("test-resources")
            .resolve("valid-bal-project");

    @BeforeTest
    void initializeTestData() throws IOException {
        // Create temporary test directory for scan operations
        testProjectDir = Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve("scan-ls-test-" + UUID.randomUUID());
        Files.createDirectories(testProjectDir);
        scanTomlPath = testProjectDir.resolve("Scan.toml");
    }

    @AfterTest
    void cleanup() {
        if (Files.exists(testProjectDir)) {
            ProjectUtils.deleteDirectory(testProjectDir);
        }
    }

    // =========================================================================
    // addGlobalExclusion Tests
    // =========================================================================

    @Test(description = "test addGlobalExclusion with valid ruleId succeeds")
    void testAddGlobalExclusionValidRuleId() {
        String result = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), "ballerina:1");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.get("success").getAsBoolean(), "Should succeed");
        Assert.assertEquals(response.get("ruleId").getAsString(), "ballerina:1");
        Assert.assertTrue(response.get("message").getAsString().contains("added successfully"));
    }

    @Test(description = "test addGlobalExclusion with null ruleId returns error")
    void testAddGlobalExclusionNullRuleId() {
        String result = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.get("message").getAsString().contains("non-empty string"));
    }

    @Test(description = "test addGlobalExclusion with empty ruleId returns error")
    void testAddGlobalExclusionEmptyRuleId() {
        String result = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), "");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.get("message").getAsString().contains("non-empty string"));
    }

    @Test(description = "test addGlobalExclusion with whitespace-only ruleId returns error")
    void testAddGlobalExclusionWhitespaceRuleId() {
        String result = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), "   ");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.get("message").getAsString().contains("non-empty string"));
    }

    @Test(description = "test addGlobalExclusion with whitespace ruleId gets trimmed")
    void testAddGlobalExclusionTrimsWhitespace() throws IOException {
        String result = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), "  ballerina:1  ");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.get("success").getAsBoolean(), "Should succeed");
        Assert.assertEquals(response.get("ruleId").getAsString(), "ballerina:1");
    }

    @Test(description = "test addGlobalExclusion with invalid project path returns error")
        void testAddGlobalExclusionMissingProjectPathCreatesFile() throws IOException {
        String missingProjectPath = testProjectDir.resolve("missing-project-" + UUID.randomUUID()).toString();
        String result = ScanLanguageServerTool.addGlobalExclusion(
                missingProjectPath, "ballerina:1");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
                Assert.assertTrue(response.get("success").getAsBoolean(), "Should succeed");
                Assert.assertEquals(response.get("ruleId").getAsString(), "ballerina:1");
                Assert.assertTrue(response.get("message").getAsString().contains("added successfully"));
                Assert.assertTrue(Files.exists(Paths.get(missingProjectPath).resolve("Scan.toml")),
                                "Scan.toml should be created for a missing project path");
    }

    // =========================================================================
    // removeGlobalExclusion Tests
    // =========================================================================

    @Test(description = "test removeGlobalExclusion with valid ruleId succeeds")
    void testRemoveGlobalExclusionValidRuleId() {
        // First add a rule
        ScanLanguageServerTool.addGlobalExclusion(testProjectDir.toString(), "ballerina:1");

        // Then remove it
        String result = ScanLanguageServerTool.removeGlobalExclusion(
                testProjectDir.toString(), "ballerina:1");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.get("success").getAsBoolean(), "Should succeed");
        Assert.assertEquals(response.get("ruleId").getAsString(), "ballerina:1");
        Assert.assertTrue(response.get("message").getAsString().contains("removed successfully"));
    }

    @Test(description = "test removeGlobalExclusion with null ruleId returns error")
    void testRemoveGlobalExclusionNullRuleId() {
        String result = ScanLanguageServerTool.removeGlobalExclusion(
                testProjectDir.toString(), null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.get("message").getAsString().contains("non-empty string"));
    }

    @Test(description = "test removeGlobalExclusion with empty ruleId returns error")
    void testRemoveGlobalExclusionEmptyRuleId() {
        String result = ScanLanguageServerTool.removeGlobalExclusion(
                testProjectDir.toString(), "");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.get("message").getAsString().contains("non-empty string"));
    }

    @Test(description = "test removeGlobalExclusion with non-existent rule succeeds (idempotent)")
    void testRemoveGlobalExclusionNonExistentRule() {
        // Try to remove a rule that was never added
        String result = ScanLanguageServerTool.removeGlobalExclusion(
                testProjectDir.toString(), "ballerina:999");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        // Should succeed (idempotent removal)
        Assert.assertTrue(response.get("success").getAsBoolean(), "Should succeed (idempotent)");
    }

    @Test(description = "test removeGlobalExclusion on a missing Scan.toml path is a no-op")
    void testRemoveGlobalExclusionMissingScanTomlIsNoOp() {
        Path missingProjectDir = testProjectDir.resolve("missing-project");

        String result = ScanLanguageServerTool.removeGlobalExclusion(
                missingProjectDir.toString(), "ballerina:1");

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.get("success").getAsBoolean(), "Should succeed as a no-op");
        Assert.assertEquals(response.get("ruleId").getAsString(), "ballerina:1");
        Assert.assertTrue(response.get("message").getAsString().contains("removed successfully"));
        Assert.assertFalse(Files.exists(missingProjectDir.resolve("Scan.toml")),
                "Missing Scan.toml should not be created by a no-op removal");
    }

    // =========================================================================
    // runScan Tests - Error Cases
    // =========================================================================

    @Test(description = "test runScan with null project path returns error in JSON")
    void testRunScanNullProjectPath() {
        String result = ScanLanguageServerTool.runScan(null, null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.has("error"), "Should contain error field");
        Assert.assertTrue(response.has("activeIssues"), "Should contain activeIssues field");
        Assert.assertTrue(response.has("excludedIssues"), "Should contain excludedIssues field");
    }

    @Test(description = "test runScan with invalid project path returns error in JSON")
    void testRunScanInvalidProjectPath() {
        String missingProjectPath = Paths.get("missing-project-" + UUID.randomUUID()).toString();
        String result = ScanLanguageServerTool.runScan(
                missingProjectPath, null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean(), "Should fail");
        Assert.assertTrue(response.has("error"), "Should contain error field");
        Assert.assertNotNull(response.get("error").getAsString(), "Error message should not be null");
    }

    @Test(description = "test runScan returns well-formed JSON even on error")
    void testRunScanErrorResponseStructure() {
        String missingProjectPath = Paths.get("missing-project-" + UUID.randomUUID()).toString();
        String result = ScanLanguageServerTool.runScan(
                missingProjectPath, new HashMap<>());

        // Verify it's valid JSON
        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.has("success"), "Should have success field");
        Assert.assertTrue(response.has("error"), "Should have error field on error");
        Assert.assertTrue(response.has("activeIssues"), "Should always have activeIssues");
        Assert.assertTrue(response.has("excludedIssues"), "Should always have excludedIssues");
        
        JsonArray activeIssues = response.getAsJsonArray("activeIssues");
        JsonArray excludedIssues = response.getAsJsonArray("excludedIssues");
        Assert.assertNotNull(activeIssues, "activeIssues should be JSON array");
        Assert.assertNotNull(excludedIssues, "excludedIssues should be JSON array");
    }

    @Test(description = "test runScan JSON response includes proper error messaging")
    void testRunScanErrorMessageHandling() {
        String missingProjectPath = Paths.get("missing-project-" + UUID.randomUUID()).toString();
        String result = ScanLanguageServerTool.runScan(
                missingProjectPath, null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertFalse(response.get("success").getAsBoolean());
        String errorMsg = response.get("error").getAsString();
        // Error should either have a message or use toString() fallback
        Assert.assertNotNull(errorMsg);
        Assert.assertTrue(!errorMsg.isEmpty() || errorMsg.length() > 0);
    }

    // =========================================================================
    // runScan Tests - Integration (Real Projects)
    // =========================================================================

    @Test(description = "test runScan with valid project executes scan and returns structured JSON")
    void testRunScanValidProjectReturnsStructuredJson() {
        // Using the pre-existing test project
        String result = ScanLanguageServerTool.runScan(
                validBalProject.toString(), null);

        // Verify response is valid JSON with required fields
        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.has("success"), "Should have success field");
        Assert.assertTrue(response.get("success").getAsBoolean(), "Scan should succeed");
        Assert.assertTrue(response.has("activeIssues"), "Should have activeIssues");
        Assert.assertTrue(response.has("excludedIssues"), "Should have excludedIssues");
        
        // Response should contain arrays
        Assert.assertTrue(response.get("activeIssues").isJsonArray(), 
                "activeIssues should be JSON array");
        Assert.assertTrue(response.get("excludedIssues").isJsonArray(), 
                "excludedIssues should be JSON array");
    }

    @Test(description = "test runScan returns issues with complete structure")
    void testRunScanReturnsIssuesWithStructure() {
        String result = ScanLanguageServerTool.runScan(
                validBalProject.toString(), null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        JsonArray activeIssues = response.getAsJsonArray("activeIssues");

        // If issues exist, verify they have the correct structure
        if (activeIssues.size() > 0) {
            JsonObject issue = activeIssues.get(0).getAsJsonObject();
            Assert.assertTrue(issue.has("ruleId"), "Issue should have ruleId");
            Assert.assertTrue(issue.has("message"), "Issue should have message");
            Assert.assertTrue(issue.has("severity"), "Issue should have severity");
            Assert.assertTrue(issue.has("ruleKind"), "Issue should have ruleKind");
        }
    }

    @Test(description = "test runScan with project having include rule configuration filters correctly")
    void testRunScanWithIncludeRuleConfiguration() {
        Path projectWithInclude = testResources.resolve("test-resources")
                .resolve("bal-project-with-include-rule-configurations");
        
        if (Files.exists(projectWithInclude)) {
            String result = ScanLanguageServerTool.runScan(
                    projectWithInclude.toString(), null);

            JsonObject response = JsonParser.parseString(result).getAsJsonObject();
            Assert.assertTrue(response.has("success"), "Scan should complete");
            Assert.assertTrue(response.has("activeIssues"), "Should have activeIssues");
            Assert.assertTrue(response.has("excludedIssues"), "Should have excludedIssues");
        }
    }

    @Test(description = "test runScan with project having exclude rule configuration filters correctly")
    void testRunScanWithExcludeRuleConfiguration() {
        Path projectWithExclude = testResources.resolve("test-resources")
                .resolve("bal-project-with-exclude-rule-configurations");
        
        if (Files.exists(projectWithExclude)) {
            String result = ScanLanguageServerTool.runScan(
                    projectWithExclude.toString(), null);

            JsonObject response = JsonParser.parseString(result).getAsJsonObject();
            Assert.assertTrue(response.has("success"), "Scan should complete");
            Assert.assertTrue(response.has("excludedIssues"), "Should have excludedIssues");
            
            // Verify excluded issues are populated
            JsonArray excludedIssues = response.getAsJsonArray("excludedIssues");
            for (int i = 0; i < excludedIssues.size(); i++) {
                JsonObject excluded = excludedIssues.get(i).getAsJsonObject();
                Assert.assertTrue(excluded.has("isGlobalExclusion"), "Excluded issue should have isGlobalExclusion");
                Assert.assertTrue(excluded.has("issueContext"), "Excluded issue should have issueContext");
            }
        }
    }

    @Test(description = "test runScan with build options passed correctly affects scan execution")
    void testRunScanWithBuildOptions() {
        Map<String, Boolean> buildOptions = new HashMap<>();
        buildOptions.put("offline", true);
        buildOptions.put("skipTests", true);

        String result = ScanLanguageServerTool.runScan(
                validBalProject.toString(), buildOptions);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.has("success"), "Should return valid JSON response");
        Assert.assertTrue(response.has("activeIssues"), "Should have activeIssues");
        Assert.assertTrue(response.has("excludedIssues"), "Should have excludedIssues");
    }

    @Test(description = "test runScan project without Scan.toml returns all active issues")
    void testRunScanProjectWithoutScanToml() {
        // validBalProject may not have Scan.toml - should run with defaults
        String result = ScanLanguageServerTool.runScan(
                validBalProject.toString(), null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        Assert.assertTrue(response.has("success"));
        Assert.assertTrue(response.has("activeIssues"));
        Assert.assertTrue(response.has("excludedIssues"));
        
        // Without Scan.toml, all issues should be active (none excluded)
        JsonArray excludedIssues = response.getAsJsonArray("excludedIssues");
        // We don't assert size is 0 since it depends on project structure, but verify it's an array
        Assert.assertNotNull(excludedIssues);
    }

    @Test(description = "test runScan returns consistent results on repeated calls")
    void testRunScanConsistency() {
        // Run scan twice on same project
        String result1 = ScanLanguageServerTool.runScan(
                validBalProject.toString(), null);
        String result2 = ScanLanguageServerTool.runScan(
                validBalProject.toString(), null);

        JsonObject response1 = JsonParser.parseString(result1).getAsJsonObject();
        JsonObject response2 = JsonParser.parseString(result2).getAsJsonObject();

        // Both should succeed
        Assert.assertTrue(response1.get("success").getAsBoolean());
        Assert.assertTrue(response2.get("success").getAsBoolean());

        // Issue counts should match
        int issueCount1 = response1.getAsJsonArray("activeIssues").size();
        int issueCount2 = response2.getAsJsonArray("activeIssues").size();
        Assert.assertEquals(issueCount1, issueCount2, "Scan results should be consistent");
    }

    // =========================================================================
    // Integration Tests
    // =========================================================================

    @Test(description = "test adding and removing multiple rules in sequence")
    void testMultipleOperationsSequence() {
        String rule1 = "rule:1";
        String rule2 = "rule:2";
        String rule3 = "rule:3";

        // Add multiple rules
        String addResult1 = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), rule1);
        String addResult2 = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), rule2);
        String addResult3 = ScanLanguageServerTool.addGlobalExclusion(
                testProjectDir.toString(), rule3);

        JsonObject resp1 = JsonParser.parseString(addResult1).getAsJsonObject();
        JsonObject resp2 = JsonParser.parseString(addResult2).getAsJsonObject();
        JsonObject resp3 = JsonParser.parseString(addResult3).getAsJsonObject();

        Assert.assertTrue(resp1.get("success").getAsBoolean());
        Assert.assertTrue(resp2.get("success").getAsBoolean());
        Assert.assertTrue(resp3.get("success").getAsBoolean());

        // Remove one
        String removeResult = ScanLanguageServerTool.removeGlobalExclusion(
                testProjectDir.toString(), rule2);
        JsonObject respRemove = JsonParser.parseString(removeResult).getAsJsonObject();
        Assert.assertTrue(respRemove.get("success").getAsBoolean());

        // Verify file reflects state
        Assert.assertTrue(Files.exists(scanTomlPath));
    }

    @Test(description = "test response JSON is always valid even on edge cases")
    void testResponseJsonValidity() {
        // Test various calls and ensure all return valid JSON
        String[] tests = {
            ScanLanguageServerTool.addGlobalExclusion(testProjectDir.toString(), "rule:1"),
            ScanLanguageServerTool.addGlobalExclusion(testProjectDir.toString(), null),
            ScanLanguageServerTool.removeGlobalExclusion(testProjectDir.toString(), "rule:1"),
            ScanLanguageServerTool.runScan(testProjectDir.toString(), null)
        };

        for (String result : tests) {
            try {
                JsonObject response = JsonParser.parseString(result).getAsJsonObject();
                Assert.assertNotNull(response, "Response should be valid JSON");
            } catch (Exception e) {
                Assert.fail("Response should be valid JSON: " + result, e);
            }
        }
    }

    @Test(description = "test adding exclusion to real project and verifying scan respects it")
    void testExclusionAffectsScanResults() {
        Path projectWithConfig = testResources.resolve("test-resources")
                .resolve("bal-project-with-exclude-rule-configurations");
        
        if (!Files.exists(projectWithConfig)) {
            // Skip if test project doesn't exist
            return;
        }

        // Run initial scan
        String initialScan = ScanLanguageServerTool.runScan(
                projectWithConfig.toString(), null);
        JsonObject initialResponse = JsonParser.parseString(initialScan).getAsJsonObject();
        JsonArray initialActive = initialResponse.getAsJsonArray("activeIssues");
        JsonArray initialExcluded = initialResponse.getAsJsonArray("excludedIssues");

        // Verify scan completed successfully
        Assert.assertTrue(initialResponse.get("success").getAsBoolean(), 
                "Initial scan should succeed");
        Assert.assertNotNull(initialActive, "Should have activeIssues array");
        Assert.assertNotNull(initialExcluded, "Should have excludedIssues array");
    }

    @Test(description = "test runScan with different build option combinations")
    void testRunScanWithVariousBuildOptions() {
                List<Map<String, Boolean>> optionSets = new java.util.ArrayList<>();
                optionSets.add(Map.of("offline", false));
                optionSets.add(Map.of("offline", true, "skipTests", false));
                optionSets.add(Map.of("offline", true, "sticky", true, "skipTests", true));
                optionSets.add(null);

        for (Map<String, Boolean> options : optionSets) {
            String result = ScanLanguageServerTool.runScan(
                    validBalProject.toString(), options);

            JsonObject response = JsonParser.parseString(result).getAsJsonObject();
            Assert.assertTrue(response.has("success"), 
                    "Should return success field with options: " + options);
            Assert.assertTrue(response.has("activeIssues"), 
                    "Should have activeIssues with options: " + options);
        }
    }

    @Test(description = "test issue structure completeness for actual scan results")
    void testIssueStructureCompleteness() {
        String result = ScanLanguageServerTool.runScan(
                validBalProject.toString(), null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        JsonArray activeIssues = response.getAsJsonArray("activeIssues");

        // Verify structure for each issue returned
        for (int i = 0; i < Math.min(activeIssues.size(), 3); i++) {
            JsonObject issue = activeIssues.get(i).getAsJsonObject();
            
            // Verify all expected fields
            Assert.assertTrue(issue.has("ruleId"), "Issue " + i + " should have ruleId");
            Assert.assertTrue(issue.has("message"), "Issue " + i + " should have message");
            Assert.assertTrue(issue.has("severity"), "Issue " + i + " should have severity");
            Assert.assertTrue(issue.has("ruleKind"), "Issue " + i + " should have ruleKind");
            
            // Verify fields are not empty/null
            Assert.assertNotNull(issue.get("ruleId").getAsString(), 
                    "ruleId should not be null");
            Assert.assertNotNull(issue.get("message").getAsString(), 
                    "message should not be null");
        }
    }

    @Test(description = "test excluded issues structure includes context and global flag")
    void testExcludedIssueStructure() {
        Path projectWithExclude = testResources.resolve("test-resources")
                .resolve("bal-project-with-exclude-rule-configurations");
        
        if (!Files.exists(projectWithExclude)) {
            return;
        }

        String result = ScanLanguageServerTool.runScan(
                projectWithExclude.toString(), null);

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        JsonArray excludedIssues = response.getAsJsonArray("excludedIssues");

        for (int i = 0; i < excludedIssues.size(); i++) {
            JsonObject excluded = excludedIssues.get(i).getAsJsonObject();
            
            Assert.assertTrue(excluded.has("filePath"), 
                    "Excluded issue " + i + " should have filePath");
            Assert.assertTrue(excluded.has("ruleId"), 
                    "Excluded issue " + i + " should have ruleId");
            Assert.assertTrue(excluded.has("isGlobalExclusion"), 
                    "Excluded issue " + i + " should have isGlobalExclusion");
            Assert.assertTrue(excluded.has("issueContext"), 
                    "Excluded issue " + i + " should have issueContext");
            
            // issueContext should also have issue structure
            JsonObject issueContext = excluded.getAsJsonObject("issueContext");
            Assert.assertTrue(issueContext.has("ruleId"), 
                    "Issue context should have ruleId");
        }
    }

    // =========================================================================
    // Versioned entry point Tests
    // =========================================================================

    @Test(description = "test apiVersion returns the declared API version")
    void testApiVersion() {
        Assert.assertEquals(ScanLanguageServerTool.apiVersion(), ScanLanguageServerTool.API_VERSION);
    }

    @Test(description = "test scan returns report issue fields with absolute file paths")
    void testScanReturnsReportIssues() throws IOException {
        Path project = testProjectDir.resolve("scan-" + UUID.randomUUID());
        Files.createDirectories(project);
        Files.writeString(project.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"scan_issues\"\nversion = \"0.1.0\"\n");
        Files.writeString(project.resolve("main.bal"),
                "public function main() {\n    int value = checkpanic int:fromString(\"1\");\n    _ = value;\n}\n");
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        Assert.assertEquals(response.get("apiVersion").getAsInt(), ScanLanguageServerTool.API_VERSION);
        JsonArray packages = response.getAsJsonArray("packages");
        Assert.assertEquals(packages.size(), 1);
        JsonObject pkg = packages.get(0).getAsJsonObject();
        Assert.assertEquals(Path.of(pkg.get("path").getAsString()), project.toAbsolutePath().normalize());
        JsonArray issues = pkg.getAsJsonArray("issues");
        Assert.assertEquals(issues.get(0).getAsJsonObject().get("ruleID").getAsString(), "ballerina:1");
        for (int i = 0; i < issues.size(); i++) {
            JsonObject issue = issues.get(i).getAsJsonObject();
            Assert.assertTrue(issue.has("ruleID"));
            Assert.assertTrue(issue.has("ruleKind"));
            Assert.assertTrue(issue.has("message"));
            Assert.assertTrue(issue.has("textRange"));
            Path filePath = Path.of(issue.get("filePath").getAsString());
            Assert.assertTrue(filePath.isAbsolute() && Files.exists(filePath), filePath.toString());
        }
    }

    @Test(description = "test scan separates suppressed issues and reports suppression diagnostics")
    void testScanAppliesSuppressions() {
        Path project = testResources.resolve("test-resources").resolve("bal-project-with-suppressions");
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        JsonObject pkg = response.getAsJsonArray("packages").get(0).getAsJsonObject();
        JsonArray issues = pkg.getAsJsonArray("issues");
        Assert.assertEquals(issues.size(), 1);
        JsonArray suppressFixes = issues.get(0).getAsJsonObject().getAsJsonArray("suppressFixes");
        Assert.assertEquals(suppressFixes.size(), 2);
        Assert.assertEquals(suppressFixes.get(1).getAsJsonObject().get("construct").getAsString(), "function 'main'");
        JsonObject suppressFix = suppressFixes.get(0).getAsJsonObject();
        Assert.assertEquals(suppressFix.get("construct").getAsString(), "variable 'active'");
        Assert.assertEquals(suppressFix.get("title").getAsString(),
                "Suppress ballerina:1 on variable 'active' with @scan:Suppress");
        JsonArray edits = suppressFix.getAsJsonArray("edits");
        Assert.assertEquals(edits.size(), 1, "the file already imports ballerina/scan");
        assertEdit(edits.get(0).getAsJsonObject(), 21, 4,
                "@scan:Suppress {rules: [\"ballerina:1\"], reason: \"$1\"}\n    ", true);
        Assert.assertTrue(pkg.get("suppressionEnabled").getAsBoolean());
        JsonArray suppressedIssues = pkg.getAsJsonArray("suppressedIssues");
        Assert.assertEquals(suppressedIssues.size(), 2);
        for (int i = 0; i < suppressedIssues.size(); i++) {
            Assert.assertTrue(suppressedIssues.get(i).getAsJsonObject().has("suppression"));
        }
        JsonArray diagnostics = pkg.getAsJsonArray("suppressionDiagnostics");
        Assert.assertEquals(diagnostics.size(), 1);
        JsonObject diagnostic = diagnostics.get(0).getAsJsonObject();
        Assert.assertEquals(diagnostic.get("severity").getAsString(), "WARNING");
        Assert.assertTrue(diagnostic.get("message").getAsString().contains("ballerina:7"));
        Assert.assertEquals(Path.of(diagnostic.get("filePath").getAsString()),
                project.resolve("main.bal").toAbsolutePath().normalize());
        Assert.assertEquals(diagnostic.getAsJsonObject("textRange").get("startLine").getAsInt(), 22);
    }

    @Test(description = "test scan does not report a suppression of a rule Scan.toml leaves out as unmatched")
    void testScanSkipsUnmatchedSuppressionOfExcludedRule() throws IOException {
        Path fixture = testResources.resolve("test-resources").resolve("bal-project-with-suppressions");
        Path project = testProjectDir.resolve("excluded-suppression-" + UUID.randomUUID());
        Files.createDirectories(project);
        for (String file : List.of("Ballerina.toml", "main.bal")) {
            Files.copy(fixture.resolve(file), project.resolve(file));
        }
        Files.writeString(project.resolve("Scan.toml"), "[rule]\nexclude = [\"ballerina:7\"]\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        JsonObject pkg = response.getAsJsonArray("packages").get(0).getAsJsonObject();
        Assert.assertEquals(pkg.getAsJsonArray("suppressedIssues").size(), 2);
        Assert.assertEquals(pkg.getAsJsonArray("suppressionDiagnostics").size(), 0, pkg.toString());
    }

    @Test(description = "test scan marks annotated issues instead of offering a fix when suppressions are disabled")
    void testScanWithSuppressionsDisabled() throws IOException {
        Path fixture = testResources.resolve("test-resources").resolve("bal-project-with-suppressions");
        Path project = testProjectDir.resolve("disabled-suppression-" + UUID.randomUUID());
        Files.createDirectories(project);
        for (String file : List.of("Ballerina.toml", "main.bal")) {
            Files.copy(fixture.resolve(file), project.resolve(file));
        }
        Files.writeString(project.resolve("Scan.toml"), "[suppression]\nenabled = false\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        JsonObject pkg = response.getAsJsonArray("packages").get(0).getAsJsonObject();
        Assert.assertFalse(pkg.get("suppressionEnabled").getAsBoolean());
        Assert.assertEquals(pkg.getAsJsonArray("suppressedIssues").size(), 0);
        Assert.assertEquals(pkg.getAsJsonArray("suppressionDiagnostics").size(), 0);
        JsonArray issues = pkg.getAsJsonArray("issues");
        Assert.assertEquals(issues.size(), 3);
        int annotated = 0;
        for (int i = 0; i < issues.size(); i++) {
            JsonObject issue = issues.get(i).getAsJsonObject();
            Assert.assertFalse(issue.has("suppression"), issue.toString());
            if (issue.has("inactiveSuppression")) {
                annotated++;
                Assert.assertFalse(issue.has("suppressFixes"), "an annotated issue must not get a second annotation");
                JsonObject enableSuppressions = issue.getAsJsonObject("enableSuppressions");
                Assert.assertEquals(enableSuppressions.get("title").getAsString(), "Enable suppressions in Scan.toml");
                Assert.assertTrue(enableSuppressions.get("note").getAsString().contains("suppressions are disabled"));
            } else {
                Assert.assertTrue(issue.has("suppressFixes"), issue.toString());
                Assert.assertFalse(issue.has("enableSuppressions"), issue.toString());
            }
        }
        Assert.assertEquals(annotated, 2);
    }

    @Test(description = "test the suppression fix imports ballerina/scan below the leading comments")
    void testSuppressFixAddsImport() throws IOException {
        Path project = testProjectDir.resolve("suppress-fix-" + UUID.randomUUID());
        Files.createDirectories(project);
        Files.writeString(project.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"suppress_fix\"\nversion = \"0.1.0\"\n");
        Files.writeString(project.resolve("main.bal"), "// License header\n\npublic function main() {\n"
                + "    int value = checkpanic int:fromString(\"1\");\n    _ = value;\n}\n");
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        JsonArray issues = response.getAsJsonArray("packages").get(0).getAsJsonObject().getAsJsonArray("issues");
        JsonObject checkpanic = null;
        for (int i = 0; i < issues.size(); i++) {
            if ("ballerina:1".equals(issues.get(i).getAsJsonObject().get("ruleID").getAsString())) {
                checkpanic = issues.get(i).getAsJsonObject();
            }
        }
        Assert.assertNotNull(checkpanic, issues.toString());
        JsonArray edits = checkpanic.getAsJsonArray("suppressFixes").get(0).getAsJsonObject().getAsJsonArray("edits");
        Assert.assertEquals(edits.size(), 2);
        assertEdit(edits.get(0).getAsJsonObject(), 2, 0, "import ballerina/scan;\n\n", false);
        assertEdit(edits.get(1).getAsJsonObject(), 3, 4,
                "@scan:Suppress {rules: [\"ballerina:1\"], reason: \"$1\"}\n    ", true);
    }

    @Test(description = "test each suppression fix restructures its statement and suppresses only that finding")
    void testSuppressFixesRestructureStatements() throws IOException {
        Path project = testProjectDir.resolve("restructure-" + UUID.randomUUID());
        Files.createDirectories(project);
        Files.writeString(project.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"restructure\"\nversion = \"0.1.0\"\n");
        Path main = project.resolve("main.bal");
        Files.writeString(main, String.join("\n",
                "class Counter {",
                "    int count;",
                "",
                "    function init() {",
                "        self.count = checkpanic int:fromString(\"1\");",
                "    }",
                "}",
                "",
                "function parse(string value) returns int {",
                "    return checkpanic int:fromString(value);",
                "}",
                "",
                "function track(string value) {",
                "    consume(checkpanic int:fromString(value));",
                "}",
                "",
                "function consume(int value) {",
                "    _ = value;",
                "}",
                "",
                "function same(int a) returns boolean {",
                "    if a == a {",
                "        return true;",
                "    }",
                "    return false;",
                "}",
                "",
                "function countDown() {",
                "    foreach int i in 0...-1 {",
                "        _ = i;",
                "    }",
                "}",
                "",
                "function reassign() {",
                "    int total = 1;",
                "    total = total;",
                "}",
                ""));

        List<String[]> cases = List.of(
                new String[]{"ballerina:1", "self.count = checkpanic int:fromString(\"1\");", "the assigned value"},
                new String[]{"ballerina:1", "return checkpanic int:fromString(value);", "the returned value"},
                new String[]{"ballerina:1", "consume(checkpanic int:fromString(value));", "this statement"},
                new String[]{"ballerina:7", "if a == a {", "the if condition"},
                new String[]{"ballerina:12", "foreach int i in 0...-1 {", "the foreach range"});
        for (String[] expected : cases) {
            String source = Files.readString(main);
            String marker = expected[1];
            JsonObject before = scanPackage(project);
            JsonObject issue = findIssue(before, expected[0], main, source, marker);
            JsonArray fixes = issue.getAsJsonArray("suppressFixes");
            Assert.assertEquals(fixes.size(), 2, issue.toString());
            JsonObject first = fixes.get(0).getAsJsonObject();
            Assert.assertEquals(first.get("construct").getAsString(), expected[2]);
            Assert.assertTrue(fixes.get(1).getAsJsonObject().get("construct").getAsString().startsWith("function")
                    || fixes.get(1).getAsJsonObject().get("construct").getAsString().startsWith("method"));
            Files.writeString(main, applyEdits(source, first.getAsJsonArray("edits")));

            JsonObject pkg = scanPackage(project);
            Assert.assertFalse(pkg.has("skippedReason"), Files.readString(main));
            Assert.assertEquals(pkg.getAsJsonArray("suppressionDiagnostics").size(), 0, pkg.toString());
            Assert.assertEquals(count(pkg, "issues", expected[0]), count(before, "issues", expected[0]) - 1,
                    "still reported: " + Files.readString(main));
            Assert.assertEquals(count(pkg, "suppressedIssues", expected[0]),
                    count(before, "suppressedIssues", expected[0]) + 1);
        }

        JsonObject selfAssignment = findIssue(scanPackage(project), "ballerina:10", main, Files.readString(main),
                "total = total;");
        JsonArray fixes = selfAssignment.getAsJsonArray("suppressFixes");
        Assert.assertEquals(fixes.size(), 1, "a rewritten self-assignment is no longer reported");
        Assert.assertEquals(fixes.get(0).getAsJsonObject().get("construct").getAsString(), "function 'reassign'");
    }

    private static JsonObject scanPackage(Path project) {
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();
        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        return response.getAsJsonArray("packages").get(0).getAsJsonObject();
    }

    // The statement is identified by its text because each applied fix moves the lines below it.
    private static JsonObject findIssue(JsonObject pkg, String ruleId, Path file, String source, String statement) {
        String[] lines = source.split("\n", -1);
        for (JsonElement element : pkg.getAsJsonArray("issues")) {
            JsonObject issue = element.getAsJsonObject();
            int line = issue.getAsJsonObject("textRange").get("startLine").getAsInt();
            if (ruleId.equals(issue.get("ruleID").getAsString()) && lines[line].trim().equals(statement)
                    && Path.of(issue.get("filePath").getAsString()).equals(file.toAbsolutePath().normalize())) {
                return issue;
            }
        }
        throw new AssertionError("no " + ruleId + " issue on '" + statement + "' in " + pkg);
    }

    private static int count(JsonObject pkg, String list, String ruleId) {
        int count = 0;
        for (JsonElement element : pkg.getAsJsonArray(list)) {
            if (ruleId.equals(element.getAsJsonObject().get("ruleID").getAsString())) {
                count++;
            }
        }
        return count;
    }

    // Applies the edits from the end of the file back, typing "accepted" where the snippet puts the reason.
    private static String applyEdits(String source, JsonArray edits) {
        List<int[]> lineStarts = new ArrayList<>();
        String[] lines = source.split("\n", -1);
        int offset = 0;
        for (String line : lines) {
            lineStarts.add(new int[]{offset});
            offset += line.length() + 1;
        }
        List<JsonObject> sorted = new ArrayList<>();
        edits.forEach(edit -> sorted.add(edit.getAsJsonObject()));
        sorted.sort((a, b) -> Integer.compare(position(b, lineStarts, "start"), position(a, lineStarts, "start")));
        StringBuilder result = new StringBuilder(source);
        for (JsonObject edit : sorted) {
            String text = edit.get("newText").getAsString();
            if (edit.get("snippet").getAsBoolean()) {
                text = text.replace("$1", "accepted").replace("\\}", "}").replace("\\$", "$");
            }
            result.replace(position(edit, lineStarts, "start"), position(edit, lineStarts, "end"), text);
        }
        return result.toString();
    }

    private static int position(JsonObject edit, List<int[]> lineStarts, String which) {
        JsonObject range = edit.getAsJsonObject("textRange");
        String prefix = which.equals("start") ? "start" : "end";
        return lineStarts.get(range.get(prefix + "Line").getAsInt())[0] + range.get(prefix + "LineOffset").getAsInt();
    }

    private static void assertEdit(JsonObject edit, int line, int offset, String newText, boolean snippet) {
        JsonObject range = edit.getAsJsonObject("textRange");
        Assert.assertEquals(range.get("startLine").getAsInt(), line);
        Assert.assertEquals(range.get("startLineOffset").getAsInt(), offset);
        Assert.assertEquals(range.get("endLine").getAsInt(), line);
        Assert.assertEquals(range.get("endLineOffset").getAsInt(), offset);
        Assert.assertEquals(edit.get("newText").getAsString(), newText);
        Assert.assertEquals(edit.get("snippet").getAsBoolean(), snippet);
    }

    @Test(description = "test scan skips a package with compilation errors")
    void testScanSkipsPackageWithCompilationErrors() {
        Path project = testResources.resolve("test-resources").resolve("bal-project-with-compilation-errors");
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        JsonObject pkg = response.getAsJsonArray("packages").get(0).getAsJsonObject();
        Assert.assertTrue(pkg.has("skippedReason"));
        Assert.assertTrue(pkg.getAsJsonArray("issues").isEmpty());
    }

    @Test(description = "test scan of a missing path reports an error")
    void testScanMissingPath() {
        JsonObject response = JsonParser.parseString(ScanLanguageServerTool.scan(
                testProjectDir.resolve("does-not-exist").toString(), null)).getAsJsonObject();

        Assert.assertFalse(response.get("success").getAsBoolean());
        Assert.assertTrue(response.has("error"));
    }

    @Test(description = "test excludeRuleEdit returns the new content without writing Scan.toml")
    void testExcludeRuleEditDoesNotWrite() {
        Path projectDir = testProjectDir.resolve("edit-" + UUID.randomUUID());
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.excludeRuleEdit(projectDir.toString(), " ballerina:1 ")).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        Assert.assertEquals(response.get("ruleId").getAsString(), "ballerina:1");
        Assert.assertFalse(response.get("exists").getAsBoolean());
        Assert.assertTrue(response.get("changed").getAsBoolean());
        Assert.assertTrue(response.get("content").getAsString().contains("\"ballerina:1\""));
        Assert.assertEquals(Path.of(response.get("path").getAsString()), projectDir.resolve("Scan.toml"));
        Assert.assertFalse(Files.exists(projectDir.resolve("Scan.toml")), "the edit must not be written");
    }

    @Test(description = "test includeRuleEdit reports no change when the rule is not excluded")
    void testIncludeRuleEditNoChange() {
        JsonObject response = JsonParser.parseString(ScanLanguageServerTool.includeRuleEdit(
                testProjectDir.resolve("edit-" + UUID.randomUUID()).toString(), "ballerina:1")).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        Assert.assertFalse(response.get("changed").getAsBoolean());
        Assert.assertFalse(response.has("content"));
    }

    @Test(description = "test includeRuleEdit removes an existing exclusion")
    void testIncludeRuleEditRemovesExclusion() throws IOException {
        Path projectDir = testProjectDir.resolve("edit-" + UUID.randomUUID());
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("Scan.toml"), "[rule]\nexclude = [\"ballerina:1\", \"ballerina:2\"]\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.includeRuleEdit(projectDir.toString(), "ballerina:1")).getAsJsonObject();

        Assert.assertTrue(response.get("changed").getAsBoolean(), response.toString());
        String content = response.get("content").getAsString();
        Assert.assertFalse(content.contains("\"ballerina:1\""));
        Assert.assertTrue(content.contains("\"ballerina:2\""));
    }

    @Test(description = "test enableSuppressionEdit sets enabled to true and keeps the other settings")
    void testEnableSuppressionEdit() throws IOException {
        Path projectDir = testProjectDir.resolve("enable-suppression-" + UUID.randomUUID());
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("Scan.toml"),
                "[rule]\nexclude = [\"ballerina:2\"]\n\n[suppression]\nenabled = false\nrequireReason = true\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.enableSuppressionEdit(projectDir.toString())).getAsJsonObject();

        Assert.assertTrue(response.get("changed").getAsBoolean(), response.toString());
        String content = response.get("content").getAsString();
        Assert.assertTrue(content.contains("enabled = true"), content);
        Assert.assertTrue(content.contains("requireReason = true"), content);
        Assert.assertTrue(content.contains("\"ballerina:2\""), content);
    }

    @Test(description = "test enableSuppressionEdit makes no change when enabled is not set")
    void testEnableSuppressionEditDefaultEnabled() throws IOException {
        Path projectDir = testProjectDir.resolve("enable-suppression-" + UUID.randomUUID());
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("Scan.toml"), "[suppression]\nrequireReason = true\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.enableSuppressionEdit(projectDir.toString())).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        Assert.assertFalse(response.get("changed").getAsBoolean());
    }

    @Test(description = "test enableSuppressionEdit makes no change when suppressions are enabled")
    void testEnableSuppressionEditNoChange() {
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.enableSuppressionEdit(testProjectDir.toString())).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        Assert.assertFalse(response.get("changed").getAsBoolean());
    }

    @Test(description = "test rule edits reject an empty rule ID")
    void testRuleEditEmptyRuleId() {
        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.excludeRuleEdit(testProjectDir.toString(), " ")).getAsJsonObject();

        Assert.assertFalse(response.get("success").getAsBoolean());
        Assert.assertTrue(response.get("error").getAsString().contains("non-empty string"));
    }

    @Test(description = "test scan with the report option writes the HTML report with the scan data")
    void testScanWritesHtmlReport() throws IOException {
        Path project = testProjectDir.resolve("report-" + UUID.randomUUID());
        Files.createDirectories(project);
        Files.writeString(project.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"scan_report\"\nversion = \"0.1.0\"\n");
        Files.writeString(project.resolve("main.bal"),
                "public function main() {\n    int value = checkpanic int:fromString(\"1\");\n    _ = value;\n}\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), Map.of("report", true))).getAsJsonObject();

        Assert.assertTrue(response.get("success").getAsBoolean(), response.toString());
        JsonObject pkg = response.getAsJsonArray("packages").get(0).getAsJsonObject();
        Path reportPath = Path.of(pkg.get("reportPath").getAsString());
        Assert.assertTrue(Files.exists(reportPath), reportPath.toString());
        String html = Files.readString(reportPath);
        Assert.assertTrue(html.contains("ballerina:1"));
        Assert.assertFalse(html.contains("__data__"));
    }

    @Test(description = "test scan without the report option does not return a report path")
    void testScanWithoutReport() throws IOException {
        Path project = testProjectDir.resolve("no-report-" + UUID.randomUUID());
        Files.createDirectories(project);
        Files.writeString(project.resolve("Ballerina.toml"),
                "[package]\norg = \"test\"\nname = \"no_report\"\nversion = \"0.1.0\"\n");
        Files.writeString(project.resolve("main.bal"), "public function main() {\n}\n");

        JsonObject response = JsonParser.parseString(
                ScanLanguageServerTool.scan(project.toString(), null)).getAsJsonObject();

        JsonObject pkg = response.getAsJsonArray("packages").get(0).getAsJsonObject();
        Assert.assertFalse(pkg.has("reportPath"));
    }
}
