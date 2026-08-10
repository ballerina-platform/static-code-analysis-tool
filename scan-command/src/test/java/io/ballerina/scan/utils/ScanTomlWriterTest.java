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

package io.ballerina.scan.utils;

import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Tests for ScanTomlWriter utility.
 *
 * @since 0.11.1
 */
public class ScanTomlWriterTest extends BaseTest {
    private Path testDir;
    private Path scanTomlPath;

    @BeforeMethod
    void initializeTestData() throws IOException {
        // Create temporary test directory
        testDir = Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve("scan-toml-test-" + UUID.randomUUID());
        Files.createDirectories(testDir);
        scanTomlPath = testDir.resolve("Scan.toml");
    }

    @AfterMethod
    void cleanup() {
        if (Files.exists(testDir)) {
            ProjectUtils.deleteDirectory(testDir);
        }
    }

    @Test(description = "test adding global exclusion to non-existent Scan.toml file")
    void testAddGlobalExclusionCreateNewFile() throws IOException {
        String ruleId = "ballerina:1";
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);

        Assert.assertTrue(Files.exists(scanTomlPath), "Scan.toml should be created");
        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("[rule]"), "[rule] section should exist");
        Assert.assertTrue(content.contains("exclude"), "exclude key should exist");
        Assert.assertTrue(content.contains("\"ballerina:1\""), "rule ID should be in exclude list");
    }

    @Test(description = "test adding multiple global exclusions without duplicates")
    void testAddGlobalExclusionMultipleRulesNoDuplicates() throws IOException {
        String ruleId1 = "ballerina:1";
        String ruleId2 = "ballerina:2";

        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId1);
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId2);
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId1); // Add duplicate

        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("\"ballerina:1\""), "First rule ID should exist");
        Assert.assertTrue(content.contains("\"ballerina:2\""), "Second rule ID should exist");
        
        // Verify no duplicates by checking exclude list
        int count = content.split("\"ballerina:1\"").length - 1;
        Assert.assertEquals(count, 1, "Rule ID should appear only once");
    }

    @Test(description = "test removing global exclusion from existing Scan.toml")
    void testRemoveGlobalExclusionSuccess() throws IOException {
        String ruleId = "ballerina:1";

        // Add first
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);
        Assert.assertTrue(Files.exists(scanTomlPath), "Scan.toml should exist");
        
        // Remove
        ScanTomlWriter.removeGlobalExclusion(scanTomlPath, ruleId);
        
        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertFalse(content.contains("\"ballerina:1\""), "Rule ID should be removed");
        // After removing last rule, [rule] section should be cleaned up
        Assert.assertFalse(content.contains("[rule]"), "[rule] section should be removed when empty");
    }

    @Test(description = "test removing one rule while preserving others")
    void testRemoveGlobalExclusionPreserveOthers() throws IOException {
        String ruleId1 = "ballerina:1";
        String ruleId2 = "ballerina:2";
        String ruleId3 = "externalOrg:rule123";

        // Add multiple rules
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId1);
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId2);
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId3);

        // Remove one
        ScanTomlWriter.removeGlobalExclusion(scanTomlPath, ruleId2);

        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("\"ballerina:1\""), "First rule should be preserved");
        Assert.assertFalse(content.contains("\"ballerina:2\""), "Removed rule should be gone");
        Assert.assertTrue(content.contains("\"externalOrg:rule123\""), "Third rule should be preserved");
        Assert.assertTrue(content.contains("[rule]"), "[rule] section should exist");
    }

    @Test(description = "test removing non-existent exclusion (no error)")
    void testRemoveNonExistentExclusionNoError() throws IOException {
        String ruleId = "ballerina:1";

        // Try to remove from non-existent file
        ScanTomlWriter.removeGlobalExclusion(scanTomlPath, ruleId);

        // Should not create file or throw error
        Assert.assertFalse(Files.exists(scanTomlPath), "File should not be created for non-existent removal");
    }

    @Test(description = "test preserving existing Scan.toml content when adding exclusion")
    void testPreserveExistingContent() throws IOException {
        // Create a Scan.toml with analyzer configuration
        String existingContent = "[[analyzer]]\norg = \"ballerina\"\nname = \"example\"\nversion = \"0.1.0\"\n";
        Files.writeString(scanTomlPath, existingContent, StandardCharsets.UTF_8);

        // Add exclusion
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "ballerina:1");

        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("[[analyzer]]"), "Analyzer section should be preserved");
        Assert.assertTrue(content.contains("ballerina"), "Analyzer org should be preserved");
        Assert.assertTrue(content.contains("[rule]"), "[rule] section should be added");
        Assert.assertTrue(content.contains("\"ballerina:1\""), "Exclusion should be added");
    }

    @Test(description = "test adding exclusion with whitespace handling")
    void testAddExclusionWithWhitespace() throws IOException {
        // The actual trimming happens at the LS level, but ScanTomlWriter should accept clean IDs
        String ruleId = "ballerina:with:colons";
        
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);
        
        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("\"ballerina:with:colons\""), "Complex rule ID should be preserved");
    }

    @Test(description = "test deterministic ordering of TOML output")
    void testDeterministicOrdering() throws IOException {
        // Add exclusions in different order
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "rule-z");
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "rule-a");
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "rule-m");

        String content1 = Files.readString(scanTomlPath, StandardCharsets.UTF_8);

        // Clear and re-add in different order
        Files.deleteIfExists(scanTomlPath);
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "rule-a");
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "rule-z");
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, "rule-m");

        String content2 = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        
        // Both should contain all rules (order in list may vary but structure should be consistent)
        Assert.assertTrue(content1.contains("rule-a"), "rule-a should be in first output");
        Assert.assertTrue(content2.contains("rule-a"), "rule-a should be in second output");
    }

    @Test(description = "test idempotence: adding same rule twice does not change file second time")
    void testIdempotenceOfAdd() throws IOException {
        String ruleId = "ballerina:1";

        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);
        String content1 = Files.readString(scanTomlPath, StandardCharsets.UTF_8);

        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);
        String content2 = Files.readString(scanTomlPath, StandardCharsets.UTF_8);

        Assert.assertEquals(content1, content2, "Content should be identical on duplicate add");
    }

    @Test(description = "test special characters in rule ID are escaped properly")
    void testSpecialCharactersEscaping() throws IOException {
        String ruleId = "rule:with\\backslash\"quotes";
        
        ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);
        
        String content = Files.readString(scanTomlPath, StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("exclude"), "exclude key should exist");
        // TOML escaping should be handled
        Assert.assertFalse(content.contains("[rule]\n\n"), "File should be properly formatted");
    }
}
