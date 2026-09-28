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

import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.ScanUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests verifying that optional rule metadata fields are included in, or omitted from, the
 * Ballerina JSON and SARIF output depending on whether the underlying rule's bundled metadata
 * specifies them.
 *
 * @since 0.1.0
 */
public final class CoreRuleOutputTest extends BaseTest {
    private final Path balProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-analyzer-configurations");

    private final BLangDiagnosticLocation location = buildLocationWithinFile(balProject.resolve("main.bal"));

    private static BLangDiagnosticLocation buildLocationWithinFile(Path filePath) {
        try {
            int length = (int) Math.min(20, Files.size(filePath));
            return new BLangDiagnosticLocation("main.bal", 1, 0, 1, length, 0, length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test(description = "test that standards is omitted, tags stay general, and severity/snippet are present "
            + "in the Ballerina JSON output")
    void testOptionalFieldsOmittedFromJsonOutput() {
        Issue issue = new IssueImpl(location, CoreRule.PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT.rule(), Source.BUILT_IN,
                "main.bal", balProject.resolve("main.bal").toString());
        String json = ScanUtils.convertIssuesToJsonString(List.of(issue));

        Assert.assertFalse(json.contains("\"standards\""),
                "standards should be omitted since rule 3's metadata does not specify any CWE/OWASP coverage");
        Assert.assertTrue(json.contains("\"severity\": \"LOW\""),
                "severity should be present since every core rule now specifies one");
        Assert.assertTrue(json.contains("\"tags\""), "tags should still be present");
        Assert.assertTrue(json.contains("\"details\""),
                "details should be serialized as \"details\" in the Ballerina JSON output");
        Assert.assertTrue(json.contains("\"snippet\": \""), "snippet should be a plain string in Ballerina JSON");
    }

    @Test(description = "test that standards is included in the Ballerina JSON output when specified")
    void testStandardsIncludedInJsonOutputWhenSpecified() {
        Issue issue = new IssueImpl(location, CoreRule.HARD_CODED_SECRET.rule(), Source.BUILT_IN, "main.bal",
                balProject.resolve("main.bal").toString());
        String json = ScanUtils.convertIssuesToJsonString(List.of(issue));

        Assert.assertTrue(json.contains("\"severity\": \"HIGH\""),
                "severity should be present since rule 13's metadata specifies one");
        Assert.assertTrue(json.contains("\"standards\""),
                "standards should be present since rule 13's metadata specifies CWE/OWASP coverage");
        Assert.assertTrue(json.contains("\"cwe\": ["), "standards.cwe should be present");
        Assert.assertTrue(json.contains("\"owasp\": ["), "standards.owasp should be present");
        Assert.assertFalse(json.contains("\"external/cwe"),
                "Ballerina JSON tags should not embed external/cwe entries (that's SARIF-only)");
    }

    @Test(description = "test that standards-derived tags are present in the SARIF output but not in tags")
    void testStandardsDerivedTagsInSarifOutput() throws Exception {
        Project project = ProjectLoader.load(balProject).project();
        Issue issue = new IssueImpl(location, CoreRule.AVOID_CHECKPANIC.rule(), Source.BUILT_IN, "main.bal",
                balProject.resolve("main.bal").toString());
        String sarif = ScanUtils.convertIssuesToSarifString(List.of(issue), project);

        Assert.assertTrue(sarif.contains("\"external/cwe/cwe-248\""),
                "SARIF properties.tags should include a generated CWE tag");
        Assert.assertTrue(sarif.contains("\"external/cwe/cwe-636\""),
                "SARIF properties.tags should include a generated CWE tag");
        Assert.assertTrue(sarif.contains("\"external/owasp/owasp-a10-2025\""),
                "SARIF properties.tags should include a generated OWASP tag");
        Assert.assertTrue(sarif.contains("\"level\": \"note\""),
                "rule 1's LOW severity should resolve to the SARIF level note");
        Assert.assertFalse(sarif.contains("\"enabled\""), "defaultConfiguration should no longer include enabled");
        Assert.assertTrue(sarif.contains("\"snippet\": {"), "SARIF region.snippet should stay a nested object");
    }

    @Test(description = "test that a rule with no severity falls back to the RuleKind-based SARIF level")
    void testMissingSeverityFallsBackToRuleKindLevel() throws Exception {
        Project project = ProjectLoader.load(balProject).project();
        Rule externalRule = RuleFactory.createRule(101, "external rule 101", RuleKind.BUG);
        Issue issue = new IssueImpl(location, externalRule, Source.EXTERNAL, "main.bal",
                balProject.resolve("main.bal").toString());
        String sarif = ScanUtils.convertIssuesToSarifString(List.of(issue), project);

        Assert.assertTrue(sarif.contains("\"level\": \"error\""),
                "a rule with no severity should fall back to the RuleKind-based level "
                        + "(BUG -> error), matching the tool's original/upstream behavior");
    }

    @Test(description = "test that a rule created via the basic (no rich metadata) API does not duplicate its "
            + "description into a redundant SARIF fullDescription, nor into a \"name\" field in the Ballerina "
            + "JSON output")
    void testBasicRuleOmitsFullDescriptionAndNameWhenNotAuthored() throws Exception {
        Project project = ProjectLoader.load(balProject).project();
        Rule externalRule = RuleFactory.createRule(101, "external rule 101", RuleKind.BUG, "exampleOrg",
                "exampleModule");
        Issue issue = new IssueImpl(location, externalRule, Source.EXTERNAL, "main.bal",
                balProject.resolve("main.bal").toString());

        String sarif = ScanUtils.convertIssuesToSarifString(List.of(issue), project);
        Assert.assertTrue(sarif.contains("\"shortDescription\""), "shortDescription should always be present");
        Assert.assertFalse(sarif.contains("\"fullDescription\""),
                "a rule with no distinct full description should not duplicate shortDescription's text into "
                        + "a redundant SARIF fullDescription");

        String json = ScanUtils.convertIssuesToJsonString(List.of(issue));
        Assert.assertFalse(json.contains("\"name\""),
                "a rule with no authored name should not duplicate description into a redundant \"name\" "
                        + "field in the Ballerina JSON output");
        Assert.assertFalse(json.contains("\"details\""),
                "a rule with no authored full description should not duplicate description into a "
                        + "redundant \"details\" field in the Ballerina JSON output");
    }
}
