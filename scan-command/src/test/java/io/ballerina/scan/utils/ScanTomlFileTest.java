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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Tests for how a Scan.toml file records the settings it writes.
 *
 * @since 0.13.0
 */
public class ScanTomlFileTest {

    @Test(description = "test only the settings a file writes are recorded, with their TOML values")
    void testRecordsWrittenSettings() {
        ScanTomlFile scanToml = new ScanTomlFile();
        scanToml.writeRules(false, List.of("ballerina:1", "ballerina:2"));
        scanToml.setAnalyzer(new ScanTomlFile.Analyzer("exampleOrg", "example_analyzer", "1.0.0", null));
        scanToml.setSuppressionReasonRequired(true);
        scanToml.setFailOnSeverity(GateSeverity.HIGH);
        scanToml.writeExcludeTests(true);

        Assert.assertEquals(scanToml.getWrittenSettings(), Map.of(
                ScanTomlFile.RULE_EXCLUDE_SETTING, "[\"ballerina:1\", \"ballerina:2\"]",
                "[[analyzer]] exampleOrg/example_analyzer:1.0.0", "",
                ScanTomlFile.SUPPRESSION_REQUIRE_REASON_SETTING, "true",
                ScanTomlFile.GATE_FAIL_ON_SEVERITY_SETTING, "\"HIGH\"",
                ScanTomlFile.ANALYSIS_EXCLUDE_TESTS_SETTING, "true"));
        Assert.assertTrue(scanToml.getWrittenValue(ScanTomlFile.GATE_FAIL_ON_COUNT_SETTING).isEmpty());
        Assert.assertTrue(scanToml.isSuppressionEnabled(), "a setting not written keeps its default");
    }

    @Test(description = "test two files are the same when read from the same normalized path or the same URL")
    void testIsSameFile() {
        ScanTomlFile workspaceFile = new ScanTomlFile();
        workspaceFile.setPath(Path.of("workspace", "Scan.toml"));
        ScanTomlFile packageFile = new ScanTomlFile();
        packageFile.setPath(Path.of("workspace", "pkg", "..", "Scan.toml"));
        Assert.assertTrue(packageFile.isSameFile(workspaceFile));
        packageFile.setPath(Path.of("workspace", "pkg", "Scan.toml"));
        Assert.assertFalse(packageFile.isSameFile(workspaceFile));
        Assert.assertFalse(new ScanTomlFile().isSameFile(workspaceFile), "a package without a file");

        ScanTomlFile remoteFile = new ScanTomlFile();
        remoteFile.setPath(Path.of("workspace", "Scan.toml"));
        remoteFile.setRemoteLocation("https://example.com/Scan.toml");
        Assert.assertFalse(remoteFile.isSameFile(workspaceFile), "a cached copy is not the local file");
        ScanTomlFile otherRemoteFile = new ScanTomlFile();
        otherRemoteFile.setRemoteLocation("https://example.com/Scan.toml");
        Assert.assertTrue(otherRemoteFile.isSameFile(remoteFile));
    }
}
