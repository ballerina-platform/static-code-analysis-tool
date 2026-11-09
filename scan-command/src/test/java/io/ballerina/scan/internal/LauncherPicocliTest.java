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

package io.ballerina.scan.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.ballerina.cli.BLauncherCmd;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Tests the scan command with the picocli JAR of the bal launcher, which the launcher loads ahead of the tool. A
 * picocli API that the launcher does not have fails only when the tool runs in a distribution.
 *
 * @since 0.11.1
 */
public class LauncherPicocliTest extends BaseTest {
    private static final String NO_TESTS_WARNING = "a single bal file has no tests to exclude";
    private static final String TEST_FILE = "main_test.bal";

    private final Path testResourcesRoot = testResources.resolve("test-resources");
    private final Path singleFile = testResourcesRoot.resolve("valid-single-file-project").resolve("main.bal")
            .toAbsolutePath();
    private final Path testsProject = testResourcesRoot.resolve("bal-project-with-tests").toAbsolutePath();
    private Path launcherPicocli;
    private URLClassLoader launcherLoader;

    /**
     * Loads the scan command with the picocli JAR of the Ballerina distribution the tests run on.
     */
    @BeforeClass
    void loadWithLauncherPicocli() throws IOException {
        String ballerinaHome = Objects.requireNonNull(System.getProperty("ballerina.home"),
                "the ballerina.home system property must point to the Ballerina distribution");
        Path libDir = Paths.get(ballerinaHome, "bre", "lib");
        List<Path> picocliJars;
        try (Stream<Path> jars = Files.list(libDir)) {
            picocliJars = jars.filter(jar -> String.valueOf(jar.getFileName()).matches("picocli-.*\\.jar"))
                    .sorted().toList();
        }
        if (picocliJars.size() != 1) {
            throw new IllegalStateException("expected one picocli JAR in " + libDir + ", found " + picocliJars);
        }
        launcherPicocli = picocliJars.get(0);
        URL scanClasses = ScanCmd.class.getProtectionDomain().getCodeSource().getLocation();
        URL cliClasses = BLauncherCmd.class.getProtectionDomain().getCodeSource().getLocation();
        launcherLoader = new LauncherClassLoader(new URL[]{launcherPicocli.toUri().toURL(), scanClasses, cliClasses},
                ScanCmd.class.getClassLoader());
    }

    @AfterClass
    void close() throws IOException {
        launcherLoader.close();
        Path target = testsProject.resolve("target");
        if (Files.exists(target)) {
            ProjectUtils.deleteDirectory(target);
        }
    }

    @Test(description = "test the scan command is loaded with the picocli JAR of the launcher")
    void testLauncherPicocliIsUsed() throws ReflectiveOperationException {
        Class<?> commandLineClass = launcherLoader.loadClass("picocli.CommandLine");
        Assert.assertSame(commandLineClass.getClassLoader(), launcherLoader, "picocli must come from the launcher");
        Assert.assertSame(launcherLoader.loadClass(ScanCmd.class.getName()).getClassLoader(), launcherLoader,
                "the scan command must resolve picocli through the launcher loader");
        String version = (String) commandLineClass.getField("VERSION").get(null);
        Assert.assertEquals(launcherPicocli.getFileName().toString(), "picocli-" + version + ".jar");
    }

    @DataProvider(name = "exclude-tests-arguments")
    public Object[][] excludeTestsArguments() {
        String file = singleFile.toString();
        return new Object[][]{
                {new String[]{"--exclude-tests", file}, 0, NO_TESTS_WARNING, null},
                {new String[]{file, "--exclude-tests"}, 0, NO_TESTS_WARNING, null},
                {new String[]{"--exclude-tests=true", file}, 0, NO_TESTS_WARNING, null},
                {new String[]{"--exclude-tests", "true", file}, 0, NO_TESTS_WARNING, null},
                {new String[]{"--exclude-tests=false", file}, 0, "Running Scans", NO_TESTS_WARNING},
                {new String[]{"--exclude-tests", "false", file}, 0, "Running Scans", NO_TESTS_WARNING},
                {new String[]{"--exclude-tests=maybe", file}, 2,
                        "invalid value 'maybe' for --exclude-tests; expected true or false", null},
                {new String[]{"a.bal", "--exclude-tests", "b.bal"}, 2, "received 2: a.bal b.bal", null},
                {new String[]{"--exclude-tests", "--fail-on-severty=HIGH", file}, 2,
                        "bal scan: error: unknown option '--fail-on-severty=HIGH'", "Running Scans"},
        };
    }

    @Test(description = "test --exclude-tests is parsed and run with the picocli version of the launcher",
            dataProvider = "exclude-tests-arguments")
    void testExcludeTests(String[] args, int exitCode, String expectedOutput, String unexpectedOutput)
            throws IOException, ReflectiveOperationException {
        int actualExitCode = runAsLauncher(testResourcesRoot, args);
        String output = readOutput(true);
        Assert.assertEquals(actualExitCode, exitCode, output);
        Assert.assertTrue(output.contains(expectedOutput), output);
        if (unexpectedOutput != null) {
            Assert.assertFalse(output.contains(unexpectedOutput), output);
        }
        if (exitCode == 0) {
            Assert.assertTrue(output.contains("Running Scans"), "the file is scanned: " + output);
        }
    }

    @DataProvider(name = "exclude-tests-package-arguments")
    public Object[][] excludeTestsPackageArguments() {
        return new Object[][]{
                {testResourcesRoot, new String[]{"--exclude-tests", testsProject.toString()}},
                {testsProject, new String[]{"--exclude-tests", "--scan-report"}},
                {testsProject, new String[]{"--exclude-tests"}},
        };
    }

    @Test(description = "test --exclude-tests leaves the test sources of a package out with the launcher picocli",
            dataProvider = "exclude-tests-package-arguments")
    void testExcludeTestsOnPackage(Path workingDirectory, String[] args)
            throws IOException, ReflectiveOperationException {
        Path scanToml = testsProject.resolve("Scan.toml");
        String original = Files.readString(scanToml, StandardCharsets.UTF_8);
        // The analyzer of the fixture is left out, as its classes would come from the test loader
        Files.writeString(scanToml, "");
        try {
            int exitCode = runAsLauncher(workingDirectory, args);
            String output = readOutput(true);
            Assert.assertEquals(exitCode, 0, output);
            JsonArray issues = JsonParser.parseString(Files.readString(testsProject.resolve("target")
                    .resolve("report").resolve("scan_results.json"), StandardCharsets.UTF_8)).getAsJsonArray();
            Assert.assertFalse(issues.isEmpty(), "the main sources are scanned");
            for (JsonElement issue : issues) {
                Assert.assertFalse(issue.getAsJsonObject().get("filePath").getAsString().contains(TEST_FILE),
                        "no issue is reported in the test file: " + issues);
            }
            if (List.of(args).contains("--scan-report")) {
                Assert.assertTrue(output.contains("View scan report at:"), output);
            }
        } finally {
            Files.writeString(scanToml, original);
        }
    }

    /**
     * Parses the arguments as the launcher does, as the arguments of a scan subcommand, and runs the command from the
     * given working directory.
     */
    private int runAsLauncher(Path workingDirectory, String[] args) throws ReflectiveOperationException {
        System.setProperty("user.dir", workingDirectory.toAbsolutePath().toString());
        try {
            Class<?> scanCmdClass = launcherLoader.loadClass(ScanCmd.class.getName());
            Constructor<?> constructor = scanCmdClass.getDeclaredConstructor(PrintStream.class);
            constructor.setAccessible(true);
            Object scanCmd = constructor.newInstance(printStream);

            Class<?> commandLineClass = launcherLoader.loadClass("picocli.CommandLine");
            Class<?> commandSpecClass = launcherLoader.loadClass("picocli.CommandLine$Model$CommandSpec");
            Object rootSpec = commandSpecClass.getMethod("create").invoke(null);
            Object root = commandLineClass.getConstructor(Object.class).newInstance(rootSpec);
            commandLineClass.getMethod("addSubcommand", String.class, Object.class).invoke(root, "scan", scanCmd);
            String[] launcherArgs = Stream.concat(Stream.of("scan"), Stream.of(args)).toArray(String[]::new);
            commandLineClass.getMethod("parseArgs", String[].class).invoke(root, (Object) launcherArgs);

            try {
                scanCmdClass.getMethod("execute").invoke(scanCmd);
            } catch (InvocationTargetException ex) {
                // An error such as a NoClassDefFoundError is what the launcher reports as a crash
                throw new AssertionError("the scan crashed with " + List.of(args), ex.getCause());
            }
            Method exitCode = scanCmdClass.getDeclaredMethod("exitCode");
            exitCode.setAccessible(true);
            return (int) exitCode.invoke(scanCmd);
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    /**
     * Loads picocli and the scan command from its own URLs first, so that the command runs on the launcher's picocli
     * while the Ballerina compiler is shared with the tests. Resources are still found parent first.
     */
    private static final class LauncherClassLoader extends URLClassLoader {
        LauncherClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("picocli.") && !name.startsWith("io.ballerina.scan.")
                    && !name.equals(BLauncherCmd.class.getName())) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = findClass(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }
    }
}
