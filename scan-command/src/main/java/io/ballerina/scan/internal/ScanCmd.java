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
import io.ballerina.scan.Rule;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.ballerina.scan.internal.ScanToolConstants.SCAN_COMMAND;

/**
 * Represents the "bal scan" command.
 *
 * @since 0.1.0
 * */
@CommandLine.Command(name = SCAN_COMMAND, description = "Perform static code analysis for Ballerina packages")
public class ScanCmd implements BLauncherCmd {
    private final PrintStream outputStream;

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

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

    public ScanCmd() {
        this.outputStream = System.out;
    }

    ScanCmd(PrintStream outputStream) {
        this.outputStream = outputStream;
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

        outputStream.println();
        outputStream.println("Running Scans...");

        Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(project.get(), outputStream);
        if (scanTomlFile.isEmpty()) {
           return;
        }

        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(project.get(), scanTomlFile.get());
        List<Rule> coreRules = CoreRule.rules();
        ExternalAnalyzerResult externalAnalyzerResult = projectAnalyzer.getExternalAnalyzers(outputStream);
        if (externalAnalyzerResult.hasAnalyzerPluginIssue()) {
            return;
        }

        if (listRules) {
            externalAnalyzerResult.externalAnalyzers().values().forEach(coreRules::addAll);
            ScanUtils.printRulesToConsole(coreRules, outputStream);
            return;
        }

        List<Issue> issues = projectAnalyzer.analyze(coreRules);
        issues.addAll(projectAnalyzer.runExternalAnalyzers(externalAnalyzerResult.externalAnalyzers()));
        if (!platforms.isEmpty() || platformTriggered) {
            return;
        }

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

    private Optional<Project> getProject() {
        if (argList.isEmpty()) {
            try {
                return Optional.of(BuildProject.load(Paths.get(System.getProperty(ProjectConstants.USER_DIR))));
            } catch (RuntimeException ex) {
                outputStream.println(ex.getMessage());
                return Optional.empty();
            }
        }

        if (argList.size() != 1) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.INVALID_NUMBER_OF_ARGUMENTS, argList.size()));
            return Optional.empty();
        }

        Path path = Paths.get(argList.get(0));
        try {
            if (path.toFile().isDirectory()) {
                return Optional.of(BuildProject.load(path));
            }
            return Optional.of(SingleFileProject.load(path));
        } catch (RuntimeException ex) {
            outputStream.println(ex.getMessage());
            return Optional.empty();
        }
    }

    private static class StringToListConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(",", -1)).map(String::trim).toList();
        }
    }
}
