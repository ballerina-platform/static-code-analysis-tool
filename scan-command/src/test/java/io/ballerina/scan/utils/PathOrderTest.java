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

import io.ballerina.cli.utils.OsUtils;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.file.Path;

/**
 * Tests the order of the file paths the reports list issues and files in.
 *
 * @since 0.14.0
 */
public class PathOrderTest {
    private final Path root = Path.of("").toAbsolutePath();

    @Test(description = "test file paths are compared folder by folder, so that a folder's files stay together")
    void testPathsAreComparedByFolder() {
        Assert.assertTrue(ScanUtils.comparePaths(root.resolve("a").resolve("x.bal").toString(),
                root.resolve("a-b.bal").toString()) < 0);
        Assert.assertTrue(ScanUtils.comparePaths(root.resolve("a").toString(),
                root.resolve("a").resolve("b.bal").toString()) < 0);
        Assert.assertEquals(ScanUtils.comparePaths(root.resolve("a.bal").toString(),
                root.resolve("x").resolve("..").resolve("a.bal").toString()), 0);
    }

    @Test(description = "test paths on different Windows drives or shares are never equal")
    void testPathsOnDifferentRoots() {
        if (!OsUtils.isWindows()) {
            throw new SkipException("drive letters and UNC roots exist only on Windows");
        }
        Assert.assertTrue(ScanUtils.comparePaths(windowsPath("C:", "w", "a.bal"),
                windowsPath("D:", "w", "a.bal")) < 0);
        Assert.assertNotEquals(ScanUtils.comparePaths(windowsPath("", "", "server", "share", "w", "a.bal"),
                windowsPath("C:", "w", "a.bal")), 0);
        Assert.assertEquals(ScanUtils.comparePaths(windowsPath("C:", "w", "a.bal"),
                windowsPath("C:", "w", "x", "..", "a.bal")), 0);
    }

    // Built from its parts, as a path literal reads to SpotBugs as a hard-coded file
    private static String windowsPath(String... parts) {
        return String.join("\\", parts);
    }
}
