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

/**
 * Verifies the SARIF (one-based, half-open-exclusive-end) / Ballerina (zero-based) position and
 * region conversion semantics: {@code Ballerina = SARIF - 1} for every line/column field, and
 * identical {@code charOffset}/{@code length} across both formats, for a one-character region, a
 * zero-width insertion-point region, and a multi-line region.
 *
 * @since 0.1.0
 */
public class RegionConversionTest extends BaseTest {
    private final Path balProject = testResources.resolve("test-resources")
            .resolve("bal-project-with-analyzer-configurations");

    @Test(description = "test position/region conversion for a one-character region")
    void testOneCharacterRegion() throws Exception {
        assertRegionConversion(new BLangDiagnosticLocation("main.bal", 5, 5, 10, 11, 100, 1));
    }

    @Test(description = "test position/region conversion for a zero-width insertion-point region")
    void testZeroWidthInsertionRegion() throws Exception {
        assertRegionConversion(new BLangDiagnosticLocation("main.bal", 5, 5, 10, 10, 100, 0));
    }

    @Test(description = "test position/region conversion for a multi-line region")
    void testMultiLineRegion() throws Exception {
        assertRegionConversion(new BLangDiagnosticLocation("main.bal", 5, 8, 10, 3, 100, 50));
    }

    private void assertRegionConversion(BLangDiagnosticLocation location) throws Exception {
        Issue issue = new IssueImpl(location, CoreRule.AVOID_CHECKPANIC.rule(), Source.BUILT_IN, "main.bal",
                balProject.resolve("main.bal").toString());

        String json = ScanUtils.convertIssuesToJsonString(List.of(issue));
        JsonObject jsonLocation = JsonParser.parseString(json).getAsJsonArray().get(0).getAsJsonObject()
                .getAsJsonObject("location");

        Project project = ProjectLoader.load(balProject).project();
        String sarif = ScanUtils.convertIssuesToSarifString(List.of(issue), project);
        JsonArray sarifResults = JsonParser.parseString(sarif).getAsJsonObject().getAsJsonArray("runs")
                .get(0).getAsJsonObject().getAsJsonArray("results");
        JsonObject sarifRegion = sarifResults.get(0).getAsJsonObject().getAsJsonArray("locations").get(0)
                .getAsJsonObject().getAsJsonObject("physicalLocation").getAsJsonObject("region");

        Assert.assertEquals(sarifRegion.get("startLine").getAsInt(), jsonLocation.get("startLine").getAsInt() + 1,
                "SARIF startLine should be one greater than the Ballerina startLine");
        Assert.assertEquals(sarifRegion.get("endLine").getAsInt(), jsonLocation.get("endLine").getAsInt() + 1,
                "SARIF endLine should be one greater than the Ballerina endLine");
        Assert.assertEquals(sarifRegion.get("startColumn").getAsInt(), jsonLocation.get("startColumn").getAsInt() + 1,
                "SARIF startColumn should be one greater than the Ballerina startColumn");
        Assert.assertEquals(sarifRegion.get("endColumn").getAsInt(), jsonLocation.get("endColumn").getAsInt() + 1,
                "SARIF endColumn should be one greater than the Ballerina endColumn");
        Assert.assertEquals(sarifRegion.get("charOffset").getAsInt(), jsonLocation.get("startOffset").getAsInt(),
                "charOffset/startOffset should be identical across both formats");
        Assert.assertEquals(sarifRegion.get("charLength").getAsInt(), jsonLocation.get("length").getAsInt(),
                "charLength/length should be identical across both formats");

        if (location.lineRange().startLine().offset() == location.lineRange().endLine().offset()
                && location.lineRange().startLine().line() == location.lineRange().endLine().line()) {
            Assert.assertEquals(jsonLocation.get("startColumn").getAsInt(), jsonLocation.get("endColumn").getAsInt());
            Assert.assertEquals(sarifRegion.get("startColumn").getAsInt(), sarifRegion.get("endColumn").getAsInt());
        }
    }
}
