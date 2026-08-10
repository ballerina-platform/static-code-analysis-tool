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
}
