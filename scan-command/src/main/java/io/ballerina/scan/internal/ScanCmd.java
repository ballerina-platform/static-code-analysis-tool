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

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.Issue;
import io.ballerina.scan.PlatformPluginContext;
import io.ballerina.scan.Rule;
import io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static io.ballerina.scan.internal.ScanToolConstants.SCAN_COMMAND;

/**
 * Represents the "bal scan" command.
 *
 * @since 0.1.0
 * */
@CommandLine.Command(name = SCAN_COMMAND, description = "Perform static code analysis for Ballerina packages")
public class ScanCmd implements BLauncherCmd {
    private final PrintStream outputStream;

    @CommandLine.Parameters (arity = "0..1")
    private final Path projectPath;

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = "--platform-triggered",
            description = "Specify whether the scan command is triggered from an external analysis platform tool",
            hidden = true)
    private boolean platformTriggered;

    @CommandLine.Option(names = "--target-dir", description = "Target directory path")
    private String targetDir;

    @CommandLine.Option(names = "--scan-report", description = "Enable HTML scan report generation")
    private boolean scanReport;

    @CommandLine.Option(names = "--list-rules",
            description = "List the rules available in the Ballerina scan tool")
    private boolean listRules;

    @CommandLine.Option(names = "--include-rules",
            converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to include specific analysis issues")
    private List<String> includeRules = new ArrayList<>();

    @CommandLine.Option(names = "--exclude-rules",
            converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to exclude specific analysis issues")
    private List<String> excludeRules = new ArrayList<>();

    @CommandLine.Option(names = "--platforms",
            converter = StringToListConverter.class,
            description = "Specify the comma separated list of static code analysis platforms to report issues")
    private List<String> platforms = new ArrayList<>();

    private final List<Rule> allRules = new ArrayList<>();

    public ScanCmd() {
        this(System.out);
    }

    ScanCmd(PrintStream outputStream) {
        this.projectPath = Paths.get(System.getProperty(ProjectConstants.USER_DIR));
        this.outputStream = outputStream;
    }

    protected ScanCmd(
            Path projectPath,
            PrintStream outputStream,
            boolean helpFlag,
            boolean platformTriggered,
            String targetDir,
            boolean scanReport,
            boolean listRules,
            List<Rule> includeRules,
            List<Rule> excludeRules,
            List<String> platforms
    ) {
        this.projectPath = projectPath;
        this.outputStream = outputStream;
        this.helpFlag = helpFlag;
        this.platformTriggered = platformTriggered;
        this.targetDir = targetDir;
        this.scanReport = scanReport;
        this.listRules = listRules;
        this.includeRules.addAll(includeRules.stream().map(Rule::id).toList());
        this.excludeRules.addAll(excludeRules.stream().map(Rule::id).toList());
        this.platforms.addAll(platforms);
    }

    @Override
    public String getName() {
        return SCAN_COMMAND;
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        StringBuilder builder = helpMessage();
        out.append(builder);
    }

    @Override
    public void printUsage(StringBuilder out) {
        out.append("Tool providing static code analysis support for Ballerina");
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
        // As there are no sub-commands for the scan command, this method is left empty
    }

    @Override
    public void execute() {
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        Optional<Project> project = getProject();
        if (project.isEmpty()) {
            return;
        }

        if (ProjectUtils.isProjectEmpty(project.get())) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.EMPTY_PACKAGE));
            return;
        }

        Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(project.get(), outputStream);
        if (scanTomlFile.isEmpty()) {
           return;
        }

        ProjectAnalyzer projectAnalyzer = getProjectAnalyzer(project.get(), scanTomlFile.get());
        List<Rule> coreRules = CoreRule.rules();
        Map<String, List<Rule>> externalAnalyzers;
        try {
            externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        } catch (RuntimeException ex) {
            outputStream.println(ex.getMessage());
            return;
        }

        allRules.addAll(coreRules);
        externalAnalyzers.values().forEach(allRules::addAll);
        if (listRules) {
            ScanUtils.printRulesToConsole(allRules, outputStream);
            return;
        }

        List<String> externalJarFilePaths = new ArrayList<>();
        Map<String, PlatformPluginContext> platformContexts = new HashMap<>();
        scanTomlFile.get().getPlatforms().forEach(platform -> {
            String platformName = platform.name();
            externalJarFilePaths.add(platform.path());
            Map<String, String> platformArgs = new HashMap<>();
            platform.arguments().forEach((key, value) -> platformArgs.put(key, value.toString()));
            platformContexts.put(platformName, new PlatformPluginContextImpl(platformArgs, platformTriggered));
            if (!platformTriggered || platforms.size() != 1 || !platforms.contains(platformName)) {
                  platforms.add(platformName);
            }
        });

        URLClassLoader ucl = loadPlatformPlugins(externalJarFilePaths);
        ServiceLoader<StaticCodeAnalysisPlatformPlugin> scannerPlatformPlugins = ServiceLoader.load(
                StaticCodeAnalysisPlatformPlugin.class, ucl);
        scannerPlatformPlugins.forEach(staticCodeAnalysisPlatformPlugin -> {
            if (platforms.contains(staticCodeAnalysisPlatformPlugin.platform())) {
                PlatformPluginContext platformPluginContext = platformContexts.get(staticCodeAnalysisPlatformPlugin
                        .platform());
                staticCodeAnalysisPlatformPlugin.init(platformPluginContext);
            }
        });

        scanTomlFile.get().getRulesToInclude().stream().map(ScanTomlFile.RuleToFilter::id).forEach(includeRules::add);
        scanTomlFile.get().getRulesToExclude().stream().map(ScanTomlFile.RuleToFilter::id).forEach(excludeRules::add);
        if (!includeRules.isEmpty() && !excludeRules.isEmpty()) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.ATTEMPT_TO_INCLUDE_AND_EXCLUDE));
            return;
        }

        outputStream.println();
        outputStream.println("Running Scans");

        List<Issue> issues = projectAnalyzer.analyze(coreRules);
        issues.addAll(projectAnalyzer.runExternalAnalyzers(externalAnalyzers));

        if (!includeRules.isEmpty()) {
            issues.removeIf(issue -> !includeRules.contains(issue.rule().id()));
        }
        if (!excludeRules.isEmpty()) {
            issues.removeIf(issue -> excludeRules.contains(issue.rule().id()));
        }

        if (platforms.isEmpty() && !platformTriggered) {
            ScanUtils.printToConsole(issues, outputStream);
            if (project.get().kind().equals(ProjectKind.BUILD_PROJECT)) {
                Path reportPath = ScanUtils.saveToDirectory(issues, project.get(), targetDir);
                outputStream.println();
                outputStream.println("View scan results at:");
                outputStream.println("\t" + reportPath.toUri() + System.lineSeparator());
                if (scanReport) {
                    Path scanReportPath = ScanUtils.generateScanReport(issues, project.get(), targetDir);
                    outputStream.println();
                    outputStream.println("View scan report at:");
                    outputStream.println("\t" + scanReportPath.toUri() + System.lineSeparator());
                }
            } else {
                if (targetDir != null) {
                    outputStream.println();
                    outputStream.println(DiagnosticLog.warning(DiagnosticCode.REPORT_NOT_SUPPORTED));
                }

                if (scanReport) {
                    outputStream.println();
                    outputStream.println(DiagnosticLog.warning(DiagnosticCode.SCAN_REPORT_NOT_SUPPORTED));
                }
            }
        }

        scannerPlatformPlugins.forEach(staticCodeAnalysisPlatformPlugin -> {
            if (platforms.contains(staticCodeAnalysisPlatformPlugin.platform())) {
                outputStream.println("Reporting issues to: " + staticCodeAnalysisPlatformPlugin.platform());
                staticCodeAnalysisPlatformPlugin.onScan(issues);
                platforms.removeAll(Collections.singleton(staticCodeAnalysisPlatformPlugin.platform()));
            }
        });
        platforms.forEach(remainingPlatform -> {
            outputStream.println();
            outputStream.println("The specified platform '" + remainingPlatform + "' is not available.");
            outputStream.println("Please ensure that the required platform plugin path is specified in 'Scan.toml'.");
        });
    }

    private StringBuilder helpMessage() {
        InputStream inputStream = ScanCmd.class.getResourceAsStream("/cli-help/ballerina-scan.help");
        StringBuilder builder = new StringBuilder();
        if (inputStream != null) {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(inputStreamReader)
            ) {
                String content = br.readLine();
                builder.append(content);
                while ((content = br.readLine()) != null) {
                    builder.append(System.lineSeparator()).append(content);
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return builder;
    }

    protected Optional<Project> getProject() {
        try {
            if (projectPath.toFile().isDirectory()) {
                return Optional.of(BuildProject.load(projectPath));
            }
            return Optional.of(SingleFileProject.load(projectPath));
        } catch (RuntimeException ex) {
            outputStream.println(ex.getMessage());
            return Optional.empty();
        }
    }

    protected ProjectAnalyzer getProjectAnalyzer(Project project, ScanTomlFile scanTomlFile) {
        return new ProjectAnalyzer(project, scanTomlFile);
    }

    /**
     * Get the list of all rules available for the given project.
     *
     * @return an unmodifiable list of all rules
     */
    public List<Rule> getAllRules() {
        return Collections.unmodifiableList(allRules);
    }

    private URLClassLoader loadPlatformPlugins(List<String> jarPaths) {
        List<URL> jarUrls = new ArrayList<>(jarPaths.size());
        jarPaths.forEach(jarPath -> {
            try {
                URL jarUrl = Path.of(jarPath).toUri().toURL();
                jarUrls.add(jarUrl);
            } catch (MalformedURLException ex) {
                throw new ScanToolException(ex.getMessage());
            }
        });
        return new URLClassLoader(jarUrls.toArray(URL[]::new), this.getClass().getClassLoader());
    }

    private static class StringToListConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(",", -1)).map(String::trim).toList();
        }
    }
}
