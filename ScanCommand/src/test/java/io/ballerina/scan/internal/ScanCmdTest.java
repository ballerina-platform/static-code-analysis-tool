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

import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Scan command tests.
 *
 * @since 0.1.0
 */
public class ScanCmdTest extends BaseTest {
    @Test(description = "test scan command override methods")
    void testScanCommandOverrideMethods() throws IOException {
        ScanCmd scanCmd = new ScanCmd();
        String result = scanCmd.getName();
        String expected = "scan";
        Assert.assertEquals(result, expected);

        StringBuilder usageResult = new StringBuilder();
        scanCmd.printUsage(usageResult);
        expected = "Tool providing static code analysis support for Ballerina";
        Assert.assertEquals(usageResult.toString(), expected);

        StringBuilder longDescription = new StringBuilder();
        scanCmd.printLongDesc(longDescription);
        Path helpTextPath = testResources.resolve("command-outputs").resolve("tool-help.txt");
        expected = Files.readString(helpTextPath, StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
        Assert.assertEquals(longDescription.toString().replace("\r\n", "\n"), expected);
    }

    @Test(description = "test scan command with help flag")
    void testScanCommandWithHelpFlag() throws IOException {
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"--help"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true);
        Path helpTextPath = testResources.resolve("command-outputs").resolve("tool-help.txt");
        String expected = Files.readString(helpTextPath, StandardCharsets.UTF_8).replace("\r\n", "\n");
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test scan command with Ballerina project")
    void testScanCommandProject() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-bal-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        String scanLog = readOutput(true);
        System.setProperty("user.dir", userDir);
        String expected = "Running Scans...";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with Ballerina project with single file as argument")
    void testScanCommandProjectWithArgument() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-bal-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {validBalProject.resolve("main.bal").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true);
        System.setProperty("user.dir", userDir);
        String expected = "The source file '" + validBalProject.resolve("main.bal") +
                "' belongs to a Ballerina package.";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with single file project with single file as argument")
    void testScanCommandSingleFileProject() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {validBalProject.resolve("main.bal").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true);
        System.setProperty("user.dir", userDir);
        String expected = "Running Scans...";
        Assert.assertEquals(scanLog.trim().split("\n")[0], expected);
    }

    @Test(description = "test scan command with single file project with project directory as argument")
    void testScanCommandSingleFileProjectWithDirectoryAsArgument() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path parentDirectory = testResources.resolve("test-resources").toAbsolutePath();
        System.setProperty("user.dir", parentDirectory.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {parentDirectory.resolve("valid-single-file-project").toString()};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true).trim();
        System.setProperty("user.dir", userDir);
        String expected = "Invalid Ballerina package directory: " +
                parentDirectory.resolve("valid-single-file-project") + ", cannot find 'Ballerina.toml' file.";
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test scan command with single file project without arguments")
    void testScanCommandSingleFileProjectWithoutArgument() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        scanCmd.execute();
        String scanLog = readOutput(true).trim();
        System.setProperty("user.dir", userDir);
        String expected = "Invalid Ballerina package directory: " + validBalProject +
                ", cannot find 'Ballerina.toml' file.";
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test scan command with single file project with too many arguments")
    void testScanCommandSingleFileProjectWithTooManyArguments() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path validBalProject = testResources.resolve("test-resources").resolve("valid-single-file-project");
        System.setProperty("user.dir", validBalProject.toString());
        ScanCmd scanCmd = new ScanCmd(printStream);
        String[] args = {"main.bal", "argument2"};
        new CommandLine(scanCmd).parseArgs(args);
        scanCmd.execute();
        String scanLog = readOutput(true).trim();
        System.setProperty("user.dir", userDir);
        String expected = "Invalid number of arguments, expected one argument received 2";
        Assert.assertEquals(scanLog, expected);
    }
}
