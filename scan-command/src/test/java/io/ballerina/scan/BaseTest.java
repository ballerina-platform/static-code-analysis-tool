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

import io.ballerina.cli.utils.OsUtils;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.util.ProjectConstants;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;

/**
 * Parent test class for providing common setup methods for tests.
 *
 * @since 0.1.0
 */
public abstract class BaseTest {
    protected static final String UNLOCKED_PROJECT = "bal-project-without-dependencies-toml";
    protected final Path testResources = Paths.get("src", "test", "resources");
    private ByteArrayOutputStream console;
    protected PrintStream printStream;
    protected final String userDir = System.getProperty("user.dir");

    // A scan requires the dependencies to be locked, so each package is locked as `bal build` would lock it
    @BeforeSuite
    protected void lockTestProjectDependencies() throws IOException {
        List<Path> packageRoots;
        try (Stream<Path> files = Files.walk(testResources.resolve("test-resources"))) {
            packageRoots = files.filter(file -> file.endsWith(ProjectConstants.BALLERINA_TOML))
                    .map(Path::getParent)
                    .filter(root -> !root.endsWith(UNLOCKED_PROJECT))
                    .filter(root -> !Files.exists(root.resolve(ProjectConstants.DEPENDENCIES_TOML)))
                    .toList();
        }
        for (Path root : packageRoots) {
            Project project = ProjectLoader.load(root).project();
            List<BuildProject> packages = project instanceof WorkspaceProject workspace
                    ? List.copyOf(workspace.projects()) : List.of((BuildProject) project);
            for (BuildProject buildProject : packages) {
                buildProject.currentPackage().getResolution();
                buildProject.save();
            }
        }
    }

    @BeforeMethod
    protected void setup() {
        this.console = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.console, true, StandardCharsets.UTF_8);
    }

    @AfterMethod(alwaysRun = true)
    protected void afterMethod() throws IOException {
        console.close();
        printStream.close();
    }

    protected String readOutput(boolean silent) throws IOException {
        String output = "";
        output = console.toString(StandardCharsets.UTF_8).replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        console.close();
        console = new ByteArrayOutputStream();
        printStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        if (!silent) {
            PrintStream out = System.out;
            out.println(output);
        }
        return output;
    }

    protected String getExpectedOutput(String path) throws IOException {
        Path expectedFilePath = testResources.resolve("command-outputs").resolve("common").resolve(path);
        if (!expectedFilePath.toFile().isFile()) {
            expectedFilePath = testResources.resolve("command-outputs")
                    .resolve(OsUtils.isWindows() ? "windows" : "unix").resolve(path);
        }
        return Files.readString(expectedFilePath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR).trim();
    }
}
