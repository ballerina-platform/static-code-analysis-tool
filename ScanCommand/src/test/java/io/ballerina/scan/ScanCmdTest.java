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

package io.ballerina.scan;

import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Scan command tests.
 *
 * @since 0.1.0
 */
public class ScanCmdTest {
    @Test(description = "test scan command")
    void testScanCommand() throws IOException {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        ScanCmd scanCmd = new ScanCmd(outputStream);
        scanCmd.execute();
        String scanLog = console.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        console.close();
        outputStream.close();
        String expected = "Running Scans...";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with help flag")
    void testScanCommandWithHelpFlag() throws IOException {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        ScanCmd scanCmd = new ScanCmd(outputStream);
        String[] args = {"--help"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = console.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        console.close();
        outputStream.close();
        Path helpTextPath = Paths.get("src", "test", "resources", "command-outputs", "tool-help.txt");
        String expected = Files.readString(helpTextPath, StandardCharsets.UTF_8).replace("\r\n", "\n");
        Assert.assertEquals(scanLog, expected);
    }
}
