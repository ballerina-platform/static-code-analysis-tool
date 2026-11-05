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

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.scan.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Tests the scan command with the picocli version of the bal launcher, which is older than the one the tool is built
 * and tested with. A picocli API that the launcher does not have fails only when the tool runs in a distribution.
 *
 * @since 0.13.0
 */
public class LauncherPicocliTest extends BaseTest {
    private final Path singleFile = testResources.resolve("test-resources").resolve("valid-single-file-project")
            .resolve("main.bal").toAbsolutePath();
    private URLClassLoader launcherLoader;

    @BeforeClass
    void loadWithLauncherPicocli() throws IOException, URISyntaxException {
        Path libDir = Paths.get(System.getProperty("ballerina.home"), "bre", "lib");
        Path launcherPicocli;
        try (Stream<Path> jars = Files.list(libDir)) {
            launcherPicocli = jars.filter(jar -> String.valueOf(jar.getFileName()).matches("picocli-.*\\.jar"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("no picocli JAR in " + libDir));
        }
        URL scanClasses = ScanCmd.class.getProtectionDomain().getCodeSource().getLocation();
        Path messages = Paths.get(Objects.requireNonNull(ScanCmd.class.getResource("/scan.properties")).toURI());
        URL scanResources = Objects.requireNonNull(messages.getParent()).toUri().toURL();
        URL cliClasses = BLauncherCmd.class.getProtectionDomain().getCodeSource().getLocation();
        launcherLoader = new LauncherClassLoader(new URL[]{launcherPicocli.toUri().toURL(), scanClasses,
                scanResources, cliClasses}, ScanCmd.class.getClassLoader());
    }

    @AfterClass
    void close() throws IOException {
        launcherLoader.close();
    }

    @Test(description = "test the scan command is loaded with the picocli version of the launcher")
    void testLauncherPicocliIsUsed() throws ReflectiveOperationException {
        String launcherVersion = (String) launcherLoader.loadClass("picocli.CommandLine").getField("VERSION").get(null);
        Assert.assertNotEquals(launcherVersion, CommandLine.VERSION, "the launcher picocli must be isolated");
    }

    @DataProvider(name = "exclude-tests-arguments")
    public Object[][] excludeTestsArguments() {
        String file = singleFile.toString();
        String noTests = "a single bal file has no tests to exclude";
        return new Object[][]{
                {new String[]{"--exclude-tests", file}, 0, noTests},
                {new String[]{file, "--exclude-tests"}, 0, noTests},
                {new String[]{"--exclude-tests=true", file}, 0, noTests},
                {new String[]{"--exclude-tests", "true", file}, 0, noTests},
                {new String[]{"--exclude-tests=false", file}, 0, "Running Scans"},
                {new String[]{"--exclude-tests=maybe", file}, 2,
                        "invalid value 'maybe' for --exclude-tests; expected true or false"},
        };
    }

    @Test(description = "test --exclude-tests is parsed and run with the picocli version of the launcher",
            dataProvider = "exclude-tests-arguments")
    void testExcludeTests(String[] args, int exitCode, String expectedOutput)
            throws IOException, ReflectiveOperationException {
        int actualExitCode = runAsLauncher(args);
        String output = readOutput(true);
        Assert.assertEquals(actualExitCode, exitCode, output);
        Assert.assertTrue(output.contains(expectedOutput), output);
        if (exitCode == 0) {
            Assert.assertTrue(output.contains("Running Scans"), "the file is scanned: " + output);
        }
    }

    /**
     * Parses the arguments as the launcher does, as the arguments of a scan subcommand, and runs the command.
     */
    private int runAsLauncher(String[] args) throws ReflectiveOperationException {
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
    }

    /**
     * Loads picocli and the scan command from its own URLs first, so that the command runs on the launcher's picocli
     * while the Ballerina compiler is shared with the tests.
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
