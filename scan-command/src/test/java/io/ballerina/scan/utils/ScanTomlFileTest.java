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
import java.util.Map;
import java.util.Set;

/**
 * Tests for how a package Scan.toml inherits the tables of a workspace Scan.toml.
 *
 * @since 0.13.0
 */
public class ScanTomlFileTest {

    @Test(description = "test a package inherits every table it does not set")
    void testInheritsUnsetTables() {
        ScanTomlFile workspace = workspaceScanToml();
        ScanTomlFile merged = new ScanTomlFile().inherit(workspace);

        Assert.assertEquals(merged.getRulesToExclude(), Set.of(new ScanTomlFile.RuleToFilter("ballerina:1")));
        Assert.assertEquals(merged.getAnalyzers().size(), 1);
        Assert.assertTrue(merged.isSuppressionReasonRequired());
        Assert.assertEquals(merged.getFailOnSeverity().orElseThrow(), "HIGH");
        Assert.assertEquals(merged.getFailOnCount().orElseThrow(), 3);
        Assert.assertTrue(merged.isExcludeTests());
        Assert.assertTrue(merged.isRuleTableInherited());
        Assert.assertEquals(merged.getRuleTableDisplayPath(Path.of(".")).orElseThrow(), "Scan.toml");
        Assert.assertEquals(merged.getGateDisplayPath(Path.of(".")).orElseThrow(), "Scan.toml");
    }

    @Test(description = "test the [rule] and [gate] tables a package sets replace the workspace ones as a whole")
    void testReplacesRuleAndGateTables() {
        ScanTomlFile pkg = new ScanTomlFile();
        pkg.markRuleTableSet();
        pkg.setRuleToInclude(new ScanTomlFile.RuleToFilter("ballerina:3"));
        pkg.markGateTableSet();
        pkg.setFailOnCount(5);

        ScanTomlFile merged = pkg.inherit(workspaceScanToml());
        Assert.assertEquals(merged.getRulesToInclude(), Set.of(new ScanTomlFile.RuleToFilter("ballerina:3")));
        Assert.assertTrue(merged.getRulesToExclude().isEmpty(), "the workspace exclude is not inherited");
        Assert.assertFalse(merged.isRuleTableInherited());
        Assert.assertTrue(merged.getFailOnSeverity().isEmpty(), "the workspace severity is not inherited");
        Assert.assertEquals(merged.getFailOnCount().orElseThrow(), 5);
        Assert.assertTrue(merged.getRuleTableDisplayPath(Path.of(".")).isEmpty());
        Assert.assertTrue(merged.getGateDisplayPath(Path.of(".")).isEmpty());
    }

    @Test(description = "test an empty table clears the inherited table")
    void testEmptyTableClearsInheritedTable() {
        ScanTomlFile pkg = new ScanTomlFile();
        pkg.markRuleTableSet();
        pkg.markGateTableSet();

        ScanTomlFile merged = pkg.inherit(workspaceScanToml());
        Assert.assertTrue(merged.getRulesToExclude().isEmpty());
        Assert.assertFalse(merged.isRuleTableInherited());
        Assert.assertFalse(merged.hasGate());
    }

    @Test(description = "test the other tables and the analyzer list a package sets replace the workspace ones")
    void testReplacesOtherTables() {
        ScanTomlFile workspace = workspaceScanToml();
        ScanTomlFile pkg = new ScanTomlFile();
        pkg.markAnalyzerListSet();
        pkg.markSuppressionTableSet();
        pkg.markAnalysisTableSet();
        pkg.setPlatform(new ScanTomlFile.Platform("examplePlatform", "plugin.jar", Map.of()));

        ScanTomlFile merged = pkg.inherit(workspace);
        Assert.assertTrue(merged.getAnalyzers().isEmpty(), "analyzer = [] clears the analyzers");
        Assert.assertFalse(merged.isSuppressionReasonRequired());
        Assert.assertFalse(merged.isExcludeTests());
        Assert.assertEquals(merged.getPlatforms().size(), 1);
    }

    private static ScanTomlFile workspaceScanToml() {
        ScanTomlFile workspace = new ScanTomlFile();
        workspace.setPath(Path.of("Scan.toml"));
        workspace.markRuleTableSet();
        workspace.setRuleToExclude(new ScanTomlFile.RuleToFilter("ballerina:1"));
        workspace.markAnalyzerListSet();
        workspace.setAnalyzer(new ScanTomlFile.Analyzer("exampleOrg", "example_analyzer", null, null));
        workspace.markSuppressionTableSet();
        workspace.setSuppressionReasonRequired(true);
        workspace.markGateTableSet();
        workspace.setFailOnSeverity("HIGH");
        workspace.setFailOnCount(3);
        workspace.markAnalysisTableSet();
        workspace.setExcludeTests(true);
        return workspace;
    }
}
