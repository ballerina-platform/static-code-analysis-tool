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

package io.ballerina.scan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.testng.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for asserting on the generated HTML scan report.
 *
 * <p>The report page is a prebuilt React bundle whose JS/CSS file names carry content hashes, so comparing the
 * whole page against a stored copy breaks on every UI rebuild. These helpers check the page structure instead and
 * return the embedded scan data so tests can assert on the fields they care about.</p>
 *
 * @since 0.12.0
 */
public final class ScanReportTestUtils {
    private static final Pattern SCAN_DATA_PATTERN =
            Pattern.compile("<script id=\"scanData\" type=\"text/json\">(.*?)</script>", Pattern.DOTALL);
    private static final Pattern JS_BUNDLE_PATTERN = Pattern.compile("src=\"\\./(static/js/main\\.[^\"]+\\.js)\"");
    private static final Pattern CSS_BUNDLE_PATTERN = Pattern.compile("href=\"\\./(static/css/main\\.[^\"]+\\.css)\"");

    private ScanReportTestUtils() {
    }

    /**
     * Verifies the report page loads the React app and returns the scan data embedded in it.
     *
     * @param reportPath path to the generated report {@code index.html}
     * @return the parsed scan data JSON
     * @throws IOException if the report cannot be read
     */
    public static JsonObject readScanReportData(Path reportPath) throws IOException {
        String html = Files.readString(reportPath, StandardCharsets.UTF_8);
        Assert.assertTrue(html.contains("<div id=\"root\"></div>"), "Report is missing the React root element");
        assertBundleExists(html, JS_BUNDLE_PATTERN, reportPath);
        assertBundleExists(html, CSS_BUNDLE_PATTERN, reportPath);

        Matcher scanData = SCAN_DATA_PATTERN.matcher(html);
        Assert.assertTrue(scanData.find(), "Report is missing the embedded scan data");
        return JsonParser.parseString(scanData.group(1)).getAsJsonObject();
    }

    private static void assertBundleExists(String html, Pattern bundlePattern, Path reportPath) {
        Matcher bundle = bundlePattern.matcher(html);
        Assert.assertTrue(bundle.find(), "Report does not reference a bundle matching " + bundlePattern.pattern());
        Path bundlePath = reportPath.resolveSibling(bundle.group(1));
        Assert.assertTrue(Files.isRegularFile(bundlePath), "Referenced bundle was not extracted: " + bundlePath);
    }
}
