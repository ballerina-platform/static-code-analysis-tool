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
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.ProjectLoadResult;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.environment.PackageLockingMode;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectPaths;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.Issue;
import io.ballerina.scan.PlatformPluginContext;
import io.ballerina.scan.ReportFormat;
import io.ballerina.scan.Rule;
import io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.utils.Constants;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_PACKAGE_SCAN_RULES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_WORKSPACE_DEPENDENCIES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_WORKSPACE_SCAN_RULES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RUNNING_SCANS_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.SCAN_COMMAND;
import static io.ballerina.scan.utils.ScanUtils.convertIssuesToJsonString;
import static io.ballerina.scan.utils.ScanUtils.convertIssuesToSarifString;

/**
 * Represents the "bal scan" command.
 *
 * @since 0.1.0
 */
@CommandLine.Command(name = SCAN_COMMAND, description = "Perform static code analysis for Ballerina packages")
public class ScanCmd implements BLauncherCmd {
    private static final String FAIL_ON_SEVERITY_OPTION = "--fail-on-severity";
    private static final String FAIL_ON_COUNT_OPTION = "--fail-on-count";
    private static final String LIST_RULES_HINT = "  Run 'bal scan --list-rules' to see the available rules.";

    private final PrintStream outputStream;
    private final PrintStream errorStream;
    // Records the exit code instead of exiting, so that tests can assert it
    private Consumer<ScanFailure> exitHandler = this::recordExitCode;
    private int exitCode;

    // Collects every positional argument, so that extra ones exit as a usage error instead of a launcher error
    @CommandLine.Parameters
    private List<String> targets = new ArrayList<>();

    private Path projectPath;

    @CommandLine.Option(names = { "--help", "-h", "?" }, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = "--platform-triggered",
            description = "Specify whether the scan command is triggered from an external analysis platform tool",
            hidden = true)
    private boolean platformTriggered;

    @CommandLine.Option(names = "--target-dir", description = "Target directory path")
    private String targetDir;

    @CommandLine.Option(names = "--scan-report", description = "Enable HTML scan report generation")
    private boolean scanReport;

    // Validated when the command runs, so that an invalid value exits as a usage error
    @CommandLine.Option(names = "--format",
            description = "Specify the format of the report (json/sarif). Default is json")
    private String format = ReportFormat.BALLERINA.getFormat();

    @CommandLine.Option(names = "--list-rules", description = "List the rules available in the Ballerina scan tool")
    private boolean listRules;

    @CommandLine.Option(names = "--include-rules", converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to include specific analysis issues")
    private List<String> includeRules = new ArrayList<>();

    @CommandLine.Option(names = "--exclude-rules", converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to exclude specific analysis issues")
    private List<String> excludeRules = new ArrayList<>();

    @CommandLine.Option(names = "--platforms", converter = StringToListConverter.class,
            description = "Specify the comma separated list of static code analysis platforms to report issues")
    private List<String> platforms = new ArrayList<>();

    @CommandLine.Option(names = "--ignore-suppressions",
            description = "Disregard @scan:Suppress annotations and report every issue")
    private boolean ignoreSuppressions;

    @CommandLine.Option(names = "--require-suppression-reason",
            description = "Fail the scan when a @scan:Suppress annotation gives no reason")
    private boolean requireSuppressionReason;

    @CommandLine.Option(names = "--require-suppression-rule-id",
            description = "Fail the scan when a @scan:Suppress annotation names no rule")
    private boolean requireSuppressionRuleId;

    @CommandLine.Option(names = FAIL_ON_SEVERITY_OPTION,
            description = "Fail the scan on issues at or above the severity (BLOCKER/HIGH/MEDIUM/LOW/INFO/NONE)")
    private String failOnSeverity;

    @CommandLine.Option(names = FAIL_ON_COUNT_OPTION,
            description = "Fail the scan when the number of issues at or above --fail-on-severity reaches the count")
    private String failOnCount;

    private final List<Rule> allRules = new ArrayList<>();
    private final Set<String> allRuleIds = new HashSet<>();
    private final List<Issue> allIssues;
    private final Map<Issue, SuppressedIssue> allSuppressions = new IdentityHashMap<>();
    private final List<Issue> workspaceConsoleIssues = new ArrayList<>();
    private boolean printWorkspaceIssues;
    private boolean scanningWorkspace;
    private ReportFormat reportFormat = ReportFormat.BALLERINA;
    private String gateSeverity;
    private Integer gateCount;

    public ScanCmd() {
        this(System.out, System.err);
        this.exitHandler = failure -> Runtime.getRuntime().exit(failure.exitCode().code());
    }

    ScanCmd(PrintStream outputStream) {
        this(outputStream, outputStream);
    }

    ScanCmd(PrintStream outputStream, PrintStream errorStream) {
        this.projectPath = Paths.get(System.getProperty(ProjectConstants.USER_DIR));
        this.outputStream = outputStream;
        this.errorStream = errorStream;
        this.allIssues = new ArrayList<>();
    }

    protected ScanCmd(
            Path projectPath,
            PrintStream outputStream,
            boolean helpFlag,
            boolean platformTriggered,
            String targetDir,
            boolean scanReport,
            ReportFormat format,
            boolean listRules,
            List<Rule> includeRules,
            List<Rule> excludeRules,
            List<String> platforms) {
        this.projectPath = projectPath;
        this.outputStream = outputStream;
        this.errorStream = outputStream;
        this.helpFlag = helpFlag;
        this.platformTriggered = platformTriggered;
        this.targetDir = targetDir;
        this.scanReport = scanReport;
        this.format = format.getFormat();
        this.reportFormat = format;
        this.listRules = listRules;
        this.includeRules.addAll(includeRules.stream().map(Rule::id).toList());
        this.excludeRules.addAll(excludeRules.stream().map(Rule::id).toList());
        this.platforms.addAll(platforms);
        this.allIssues = new ArrayList<>();
        // Test runners run the scan in their own JVM, so a failure is thrown instead of exiting it
        this.exitHandler = failure -> {
            throw new ScanToolException(failure.getMessage(), failure.details());
        };
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
        try {
            run();
        } catch (RuntimeException ex) {
            fail(ScanFailure.from(ex));
        }
    }

    int exitCode() {
        return exitCode;
    }

    private void recordExitCode(ScanFailure failure) {
        exitCode = failure.exitCode().code();
    }

    private void fail(ScanFailure failure) {
        outputStream.flush();
        failure.reportLines().forEach(errorStream::println);
        errorStream.flush();
        exitHandler.accept(failure);
    }

    private void warn(String message) {
        errorStream.println(ScanFailure.warning(message));
    }

    private void run() {
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        resolveOptions();
        Project project = getProject().orElseThrow(() -> ScanFailure.invalidTarget(
                DiagnosticLog.error(DiagnosticCode.INVALID_PROJECT_PATH, projectPath), List.of()));
        if (project.kind() == ProjectKind.WORKSPACE_PROJECT) {
            executeWorkspace((WorkspaceProject) project);
            return;
        }
        if (ProjectUtils.isProjectEmpty(project)) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.EMPTY_PACKAGE));
        }
        executeProject(project);
    }

    private void resolveOptions() {
        if (targets.size() > 1) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.INVALID_NUMBER_OF_ARGUMENTS, targets.size(),
                    String.join(" ", targets)));
        }
        if (!targets.isEmpty()) {
            projectPath = Paths.get(targets.get(0));
        }
        try {
            reportFormat = ReportFormat.fromString(format);
        } catch (IllegalArgumentException ex) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.INVALID_FORMAT, format));
        }
        if (!listRules && !includeRules.isEmpty() && !excludeRules.isEmpty()) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.ATTEMPT_TO_INCLUDE_AND_EXCLUDE));
        }
        if (failOnSeverity != null) {
            gateSeverity = ScanUtils.parseGateSeverity(failOnSeverity).orElseThrow(() -> ScanFailure.usage(
                    DiagnosticLog.error(DiagnosticCode.INVALID_OPTION_VALUE, failOnSeverity, FAIL_ON_SEVERITY_OPTION,
                            ScanUtils.GATE_SEVERITY_VALUES)));
        }
        if (failOnCount != null) {
            gateCount = ScanUtils.parseGateCount(failOnCount).orElseThrow(() -> ScanFailure.usage(
                    DiagnosticLog.error(DiagnosticCode.INVALID_OPTION_VALUE, failOnCount, FAIL_ON_COUNT_OPTION,
                            ScanUtils.GATE_COUNT_VALUES)));
        }
    }

    private void executeWorkspace(WorkspaceProject workspaceProject) {
        errorStream.println();
        errorStream.println(listRules ? RESOLVING_WORKSPACE_SCAN_RULES_LOG : RESOLVING_WORKSPACE_DEPENDENCIES_LOG);
        scanningWorkspace = true;
        Optional<ScanTomlFile> workspaceScanToml = listRules ? Optional.empty()
                : readWorkspaceScanToml(workspaceProject);

        // Every package is prepared before any is scanned, so that rule IDs are checked against all their rules
        Map<BuildProject, ScanFailure> failures = new LinkedHashMap<>();
        List<PackageScan> packageScans = new ArrayList<>();
        for (BuildProject buildProject : workspaceProject.getResolution().dependencyGraph()
                .toTopologicallySortedList()) {
            if (ProjectUtils.isProjectEmpty(buildProject)) {
                warn(DiagnosticLog.warning(DiagnosticCode.EMPTY_PACKAGE_SKIPPED,
                        packageLabel(workspaceProject, buildProject)));
                continue;
            }
            try {
                packageScans.add(preparePackage(buildProject));
            } catch (RuntimeException ex) {
                failures.put(buildProject, ScanFailure.from(ex));
            }
        }
        // A package that failed to prepare has no rules, so its rule IDs would be reported as unknown
        if (failures.isEmpty()) {
            failOnUnknownRuleOptions();
        }

        if (listRules) {
            if (!allRules.isEmpty()) {
                errorStream.println();
                ScanUtils.printRulesToConsole(allRules, outputStream);
            }
            failOnPackageFailures(workspaceProject, failures);
            return;
        }

        boolean workspaceGated = !platformTriggered && (gateSeverity != null || gateCount != null
                || workspaceScanToml.map(ScanTomlFile::hasGate).orElse(false));
        QualityGate workspaceGate = QualityGate.OFF;
        Map<Project, QualityGate> packageGates = new LinkedHashMap<>();
        if (workspaceGated) {
            workspaceGate = resolveGate(workspaceScanToml.orElse(null), workspaceProject.sourceRoot());
            warnIgnoredPackageGates(workspaceProject, packageScans);
        } else {
            packageScans.forEach(packageScan -> packageGates.put(packageScan.project(),
                    resolveGate(packageScan.scanToml(), packageScan.project().sourceRoot())));
        }

        List<PackageResult> results = new ArrayList<>();
        for (PackageScan packageScan : packageScans) {
            BuildProject buildProject = (BuildProject) packageScan.project();
            try {
                PackageResult result = scanPackage(packageScan);
                results.add(result);
                if (result.suppressionFailure() != null) {
                    failures.put(buildProject, result.suppressionFailure());
                }
            } catch (RuntimeException ex) {
                failures.put(buildProject, ScanFailure.from(ex));
            }
        }
        // Nothing is reported until every package is scanned without an error
        failOnPackageFailures(workspaceProject, failures);

        results.forEach(this::report);
        // The packages' issues are printed as one document, so that standard output stays valid JSON
        if (printWorkspaceIssues) {
            ScanUtils.printToConsole(workspaceConsoleIssues, outputStream, ReportFormat.SARIF.equals(reportFormat),
                    workspaceProject);
        }
        errorStream.println();
        writeWorkspaceReports(workspaceProject);
        List<Issue> activeIssues = results.stream().flatMap(result -> result.activeIssues().stream()).toList();
        printSummary(activeIssues.size(), allSuppressions.size());

        if (workspaceGated) {
            workspaceGate.evaluate(activeIssues).ifPresent(message -> {
                throw ScanFailure.runtime(message);
            });
            return;
        }
        List<String> gateFailures = new ArrayList<>();
        for (PackageResult result : results) {
            Project packageProject = result.packageScan().project();
            packageGates.getOrDefault(packageProject, QualityGate.OFF).evaluate(result.activeIssues())
                    .ifPresent(message -> gateFailures.add(packageError(workspaceProject, packageProject, message)));
        }
        if (!gateFailures.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PACKAGES_FAILED_GATE, gateFailures.size(),
                    String.valueOf(gateFailures.size())), gateFailures);
        }
    }

    private Optional<ScanTomlFile> readWorkspaceScanToml(WorkspaceProject workspaceProject) {
        Path scanTomlFilePath = workspaceProject.sourceRoot().resolve(Constants.SCAN_FILE);
        if (!Files.isRegularFile(scanTomlFilePath)) {
            return Optional.empty();
        }
        errorStream.println("Loading scan tool configurations from " + scanTomlFilePath);
        return Optional.of(ScanUtils.readWorkspaceScanToml(scanTomlFilePath, Constants.SCAN_FILE, this::warn));
    }

    private void writeWorkspaceReports(WorkspaceProject workspaceProject) {
        accumulateWorkspaceReports(workspaceProject);
        if (scanReport) {
            Path scanReportPath = ScanUtils.generateScanReport(allIssues, workspaceProject, targetDir,
                    allSuppressions);
            errorStream.println();
            errorStream.println("View scan report at:");
            errorStream.println("\t" + scanReportPath.toUri() + System.lineSeparator());
        }
    }

    private void warnIgnoredPackageGates(WorkspaceProject workspaceProject, List<PackageScan> packageScans) {
        List<String> ignoredGates = packageScans.stream()
                .filter(packageScan -> packageScan.scanToml().hasGate())
                .map(packageScan -> packageScan.scanToml().getDisplayPath(workspaceProject.sourceRoot())
                        .orElse(packageLabel(workspaceProject, packageScan.project())))
                .toList();
        if (!ignoredGates.isEmpty()) {
            warn(DiagnosticLog.warning(DiagnosticCode.PACKAGE_GATE_IGNORED, joinWithAnd(ignoredGates)));
        }
    }

    // Errors win over the gate, so this runs before the gate is evaluated
    private static void failOnPackageFailures(WorkspaceProject workspaceProject,
                                              Map<BuildProject, ScanFailure> failures) {
        if (failures.isEmpty()) {
            return;
        }
        List<String> diagnostics = new ArrayList<>();
        ExitCode exitCode = ExitCode.RUNTIME_ERROR;
        for (Map.Entry<BuildProject, ScanFailure> entry : failures.entrySet()) {
            ScanFailure failure = entry.getValue();
            diagnostics.add(packageError(workspaceProject, entry.getKey(), failure.getMessage()));
            diagnostics.addAll(failure.details());
            if (failure.exitCode() == ExitCode.INVALID_TARGET) {
                exitCode = ExitCode.INVALID_TARGET;
            }
        }
        throw ScanFailure.of(exitCode, DiagnosticLog.error(DiagnosticCode.PACKAGES_FAILED, failures.size(),
                String.valueOf(failures.size())), diagnostics);
    }

    private static String packageError(WorkspaceProject workspaceProject, Project packageProject, String message) {
        return packageLabel(workspaceProject, packageProject) + ": error: " + message;
    }

    private static String packageLabel(WorkspaceProject workspaceProject, Project packageProject) {
        return ScanUtils.displayPath(workspaceProject.sourceRoot(), packageProject.sourceRoot());
    }

    private void accumulateWorkspaceReports(WorkspaceProject workspaceProject) {
        boolean isSarifFormat = ReportFormat.SARIF.equals(reportFormat);
        String reportFileName = isSarifFormat ? Constants.RESULTS_SARIF_FILE : Constants.RESULTS_JSON_FILE;
        Path finalReportPath;
        try {
            finalReportPath = ScanUtils.getTargetPath(workspaceProject, targetDir).getReportPath()
                    .resolve(reportFileName);
            String finalReportContent = isSarifFormat
                    ? convertIssuesToSarifString(allIssues, workspaceProject, allSuppressions)
                    : convertIssuesToJsonString(allIssues, allSuppressions);
            Files.writeString(finalReportPath, finalReportContent);
        } catch (IOException e) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.FAILED_TO_WRITE_REPORT, reportFileName,
                    e.getMessage()));
        }
        errorStream.println("View scan results at:");
        errorStream.println("\t" + finalReportPath.toUri());
        errorStream.println();
    }

    /**
     * Scans a package or a single file, and fails the scan on an error or a tripped gate.
     *
     * @param project the package or single file project
     */
    public void executeProject(Project project) {
        PackageScan packageScan = preparePackage(project);
        failOnUnknownRuleOptions();
        if (listRules) {
            if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
                errorStream.println();
            }
            ScanUtils.printRulesToConsole(allRules, outputStream);
            return;
        }

        QualityGate gate = resolveGate(packageScan.scanToml(), project.sourceRoot());
        PackageResult result = scanPackage(packageScan);
        // Nothing is reported until the package meets the suppression requirements
        if (result.suppressionFailure() != null) {
            throw result.suppressionFailure();
        }
        report(result);
        printSummary(result.activeIssues().size(), result.suppressions().size());
        gate.evaluate(result.activeIssues()).ifPresent(message -> {
            throw ScanFailure.runtime(message);
        });
    }

    private PackageScan preparePackage(Project project) {
        ScanTomlFile scanTomlFile = ScanUtils.readScanTomlConfigurations(project, errorStream);

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT && !listRules) {
            printUnsupportedSingleFileFlagWarnings();
        }
        if (listRules && project.kind() == ProjectKind.BUILD_PROJECT && !scanningWorkspace) {
            errorStream.println();
            errorStream.println(RESOLVING_PACKAGE_SCAN_RULES_LOG);
        }

        ProjectAnalyzer projectAnalyzer = getProjectAnalyzer(project, scanTomlFile);
        List<Rule> coreRules = CoreRule.rules();
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        addRules(coreRules);
        externalAnalyzers.values().forEach(this::addRules);

        List<String> projectIncludeRules = new ArrayList<>(includeRules);
        List<String> projectExcludeRules = new ArrayList<>(excludeRules);
        scanTomlFile.getRulesToInclude().stream().map(ScanTomlFile.RuleToFilter::id)
                .forEach(projectIncludeRules::add);
        scanTomlFile.getRulesToExclude().stream().map(ScanTomlFile.RuleToFilter::id)
                .forEach(projectExcludeRules::add);
        if (!listRules && !projectIncludeRules.isEmpty() && !projectExcludeRules.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.ATTEMPT_TO_INCLUDE_AND_EXCLUDE));
        }
        return new PackageScan(project, scanTomlFile, projectAnalyzer, coreRules, externalAnalyzers,
                new RuleSelection(projectIncludeRules, projectExcludeRules));
    }

    private void failOnUnknownRuleOptions() {
        if (listRules) {
            return;
        }
        List<String> unknownRules = Stream.concat(includeRules.stream(), excludeRules.stream())
                .filter(ruleId -> !allRuleIds.contains(ruleId))
                .distinct()
                .map(ruleId -> "'" + ruleId + "'")
                .toList();
        if (!unknownRules.isEmpty()) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.UNKNOWN_RULE_OPTION,
                    joinWithAnd(unknownRules), unknownRules.size()), LIST_RULES_HINT);
        }
    }

    // Each setting is resolved on its own, and the command line overrides Scan.toml. A platform that triggers the
    // scan applies its own quality gate, so the tool gate is off
    private QualityGate resolveGate(ScanTomlFile scanTomlFile, Path root) {
        if (platformTriggered) {
            return QualityGate.OFF;
        }
        String tomlSeverity = scanTomlFile == null ? null : scanTomlFile.getFailOnSeverity().orElse(null);
        Integer tomlCount = scanTomlFile == null ? null : scanTomlFile.getFailOnCount().orElse(null);
        String scanTomlLabel = scanTomlFile == null ? Constants.SCAN_FILE
                : scanTomlFile.getDisplayPath(root).orElse(Constants.SCAN_FILE);
        if (gateSeverity != null && tomlSeverity != null && !gateSeverity.equals(tomlSeverity)) {
            warn(DiagnosticLog.warning(DiagnosticCode.GATE_SETTING_OVERRIDDEN,
                    FAIL_ON_SEVERITY_OPTION + "=" + gateSeverity, "failOnSeverity = \"" + tomlSeverity + "\"",
                    scanTomlLabel));
        }
        if (gateCount != null && tomlCount != null && !gateCount.equals(tomlCount)) {
            warn(DiagnosticLog.warning(DiagnosticCode.GATE_SETTING_OVERRIDDEN,
                    FAIL_ON_COUNT_OPTION + "=" + gateCount, "failOnCount = " + tomlCount, scanTomlLabel));
        }
        String severity = gateSeverity != null ? gateSeverity : tomlSeverity;
        // A count on the command line turns on a gate that Scan.toml turns off, counting every severity
        if (gateSeverity == null && gateCount != null && ScanUtils.GATE_SEVERITY_NONE.equals(tomlSeverity)) {
            warn(DiagnosticLog.warning(DiagnosticCode.GATE_SETTING_OVERRIDDEN,
                    FAIL_ON_COUNT_OPTION + "=" + gateCount, "failOnSeverity = \"" + tomlSeverity + "\"",
                    scanTomlLabel));
            severity = null;
        }
        return QualityGate.of(severity, gateCount != null ? gateCount : tomlCount);
    }

    private PackageResult scanPackage(PackageScan packageScan) {
        Project project = packageScan.project();
        errorStream.println();
        errorStream.println(RUNNING_SCANS_LOG);

        // Each package has its own list, since a workspace reports its packages only after scanning all of them
        List<String> packagePlatforms = new ArrayList<>(platforms);
        List<String> externalJarFilePaths = new ArrayList<>();
        Map<String, PlatformPluginContext> platformContexts = new HashMap<>();
        packageScan.scanToml().getPlatforms().forEach(platform -> {
            String platformName = platform.name();
            externalJarFilePaths.add(platform.path());
            Map<String, String> platformArgs = new HashMap<>();
            platform.arguments().forEach((key, value) -> platformArgs.put(key, value.toString()));
            platformContexts.put(platformName, new PlatformPluginContextImpl(platformArgs, platformTriggered));
            if (!platformTriggered || packagePlatforms.size() != 1 || !packagePlatforms.contains(platformName)) {
                packagePlatforms.add(platformName);
            }
        });

        ProjectAnalyzer projectAnalyzer = packageScan.analyzer();
        List<Issue> externalIssues = projectAnalyzer.runExternalAnalyzers(packageScan.externalAnalyzers());
        failOnCompilationErrors(projectAnalyzer);

        URLClassLoader ucl = loadPlatformPlugins(externalJarFilePaths);
        ServiceLoader<StaticCodeAnalysisPlatformPlugin> scannerPlatformPlugins = ServiceLoader.load(
                StaticCodeAnalysisPlatformPlugin.class, ucl);
        scannerPlatformPlugins.forEach(staticCodeAnalysisPlatformPlugin -> {
            if (packagePlatforms.contains(staticCodeAnalysisPlatformPlugin.platform())) {
                PlatformPluginContext platformPluginContext = platformContexts.get(staticCodeAnalysisPlatformPlugin
                        .platform());
                runPlatformPlugin(staticCodeAnalysisPlatformPlugin,
                        () -> staticCodeAnalysisPlatformPlugin.init(platformPluginContext));
            }
        });

        List<Issue> issues = projectAnalyzer.analyze(packageScan.coreRules());
        issues.addAll(externalIssues);

        RuleSelection ruleSelection = packageScan.ruleSelection();
        issues.removeIf(issue -> !ruleSelection.selects(issue.rule().id()));

        SuppressionOutcome suppressionOutcome = SuppressionFilter.apply(project, issues,
                ProjectAnalyzer.rules(packageScan.coreRules(), packageScan.externalAnalyzers()), ruleSelection,
                SuppressionOptions.of(packageScan.scanToml(), ignoreSuppressions, requireSuppressionReason,
                        requireSuppressionRuleId));
        printSuppressionWarnings(suppressionOutcome.diagnostics());
        return new PackageResult(packageScan, issues, suppressionOutcome.activeIssues(),
                suppressionOutcome.suppressionsByIssue(), scannerPlatformPlugins, packagePlatforms,
                suppressionFailure(suppressionOutcome));
    }

    private void report(PackageResult result) {
        Project project = result.packageScan().project();
        List<Issue> activeIssues = result.activeIssues();
        Map<Issue, SuppressedIssue> suppressions = result.suppressions();
        allIssues.addAll(result.issues());
        allSuppressions.putAll(suppressions);

        if (result.platforms().isEmpty() && !platformTriggered) {
            boolean isSarifFormat = ReportFormat.SARIF.equals(reportFormat);
            if (scanningWorkspace) {
                workspaceConsoleIssues.addAll(activeIssues);
                printWorkspaceIssues = true;
            } else {
                ScanUtils.printToConsole(activeIssues, outputStream, isSarifFormat, project);
            }
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                Path reportPath;
                if (isSarifFormat) {
                    reportPath = ScanUtils.saveSarifToDirectory(result.issues(), project, targetDir, suppressions);
                } else {
                    reportPath = ScanUtils.saveToDirectory(result.issues(), project, targetDir, suppressions);
                }

                if (project.workspaceProject().isEmpty()) {
                    errorStream.println();
                    errorStream.println("View scan results at:");
                    errorStream.println("\t" + reportPath.toUri());
                    errorStream.println();
                    if (scanReport) {
                        Path scanReportPath = ScanUtils.generateScanReport(result.issues(), project, targetDir,
                                suppressions);
                        errorStream.println();
                        errorStream.println("View scan report at:");
                        errorStream.println("\t" + scanReportPath.toUri() + System.lineSeparator());
                    }
                }
            }
        }

        List<String> unreportedPlatforms = new ArrayList<>(result.platforms());
        result.platformPlugins().forEach(staticCodeAnalysisPlatformPlugin -> {
            if (unreportedPlatforms.contains(staticCodeAnalysisPlatformPlugin.platform())) {
                errorStream.println("Reporting issues to: " + staticCodeAnalysisPlatformPlugin.platform());
                runPlatformPlugin(staticCodeAnalysisPlatformPlugin,
                        () -> staticCodeAnalysisPlatformPlugin.onScan(activeIssues));
                unreportedPlatforms.removeAll(Collections.singleton(staticCodeAnalysisPlatformPlugin.platform()));
            }
        });
        if (!unreportedPlatforms.isEmpty()) {
            List<String> missingPlatforms = unreportedPlatforms.stream().distinct()
                    .map(platform -> "'" + platform + "'").toList();
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_NOT_AVAILABLE,
                    missingPlatforms.size(), joinWithAnd(missingPlatforms)));
        }
    }

    private static void runPlatformPlugin(StaticCodeAnalysisPlatformPlugin platformPlugin, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_FAILED, platformPlugin.platform(),
                    ScanFailure.describe(ex)));
        }
    }

    private static void failOnCompilationErrors(ProjectAnalyzer projectAnalyzer) {
        List<Diagnostic> compilationErrors = projectAnalyzer.getCompilationErrors();
        if (compilationErrors.isEmpty()) {
            return;
        }
        throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.COMPILATION_CONTAINS_ERRORS),
                compilationErrors.stream().map(ScanCmd::formatDiagnostic).toList());
    }

    private static ScanFailure suppressionFailure(SuppressionOutcome suppressionOutcome) {
        long invalidSuppressionCount = suppressionOutcome.invalidSuppressionCount();
        if (invalidSuppressionCount == 0) {
            return null;
        }
        List<String> diagnostics = suppressionOutcome.diagnostics().stream()
                .filter(SuppressionDiagnostic::isError)
                .map(diagnostic -> DiagnosticLog.fileError(diagnostic.fileName(), diagnostic.location(),
                        diagnostic.message()))
                .toList();
        return ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.SUPPRESSION_REQUIREMENTS_NOT_MET,
                invalidSuppressionCount, String.valueOf(invalidSuppressionCount)), diagnostics);
    }

    private static String formatDiagnostic(Diagnostic diagnostic) {
        Location location = diagnostic.location();
        LineRange lineRange = location == null ? null : location.lineRange();
        if (lineRange == null || lineRange.fileName() == null || lineRange.fileName().isEmpty()) {
            return "error: " + diagnostic.message();
        }
        return DiagnosticLog.fileError(lineRange.fileName(), lineRange, diagnostic.message());
    }

    private static String joinWithAnd(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    private void printSuppressionWarnings(List<SuppressionDiagnostic> diagnostics) {
        diagnostics.stream()
                .filter(diagnostic -> !diagnostic.isError())
                .map(diagnostic -> DiagnosticLog.fileWarning(diagnostic.fileName(), diagnostic.location(),
                        diagnostic.message()))
                .forEach(errorStream::println);
    }

    private void printSummary(int activeIssueCount, int suppressedIssueCount) {
        errorStream.println();
        errorStream.println(suppressedIssueCount == 0 ? activeIssueCount + " issue(s)"
                : activeIssueCount + " issue(s), " + suppressedIssueCount + " suppressed");
    }

    private void printUnsupportedSingleFileFlagWarnings() {
        if (targetDir != null) {
            warn(DiagnosticLog.warning(DiagnosticCode.REPORT_NOT_SUPPORTED));
        }

        if (scanReport) {
            warn(DiagnosticLog.warning(DiagnosticCode.SCAN_REPORT_NOT_SUPPORTED));
        }
    }

    private StringBuilder helpMessage() {
        InputStream inputStream = ScanCmd.class.getResourceAsStream("/cli-help/ballerina-scan.help");
        StringBuilder builder = new StringBuilder();
        if (inputStream != null) {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(inputStreamReader)) {
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

    /**
     * Loads the project to scan.
     *
     * @return the project
     * @throws RuntimeException if the path is not a valid Ballerina project, which exits as an invalid target, or
     *                          if the dependencies of a package are not locked
     */
    protected Optional<Project> getProject() {
        Project project = loadProject();
        ScanUtils.requireLockedDependencies(project);
        return Optional.of(project);
    }

    private Project loadProject() {
        if (!isBallerinaProjectPath()) {
            Optional<Path> packageRoot = getEnclosingPackageRoot();
            if (packageRoot.isPresent()) {
                throw ScanFailure.invalidTarget(DiagnosticLog.error(DiagnosticCode.FILE_IN_PACKAGE, projectPath,
                        packageRoot.get()), List.of());
            }
            throw ScanFailure.invalidTarget(DiagnosticLog.error(DiagnosticCode.INVALID_PROJECT_PATH, projectPath),
                    List.of());
        }

        String loadFailure = DiagnosticLog.error(DiagnosticCode.PROJECT_LOAD_FAILED, projectPath);
        ProjectLoadResult loadResult;
        try {
            // The scan never updates Dependencies.toml, so it uses the locked versions instead of looking for
            // newer ones in Central after every source change.
            BuildOptions buildOptions = BuildOptions.builder().setLockingMode(PackageLockingMode.HARD).build();
            loadResult = ProjectLoader.load(projectPath, buildOptions);
        } catch (RuntimeException ex) {
            throw ScanFailure.invalidTarget(loadFailure, List.of("error: " + ScanFailure.describe(ex)));
        }
        if (loadResult.diagnostics().hasErrors()) {
            throw ScanFailure.invalidTarget(loadFailure,
                    loadResult.diagnostics().errors().stream().map(ScanCmd::formatDiagnostic).toList());
        }
        return loadResult.project();
    }

    private boolean isBallerinaProjectPath() {
        try {
            return ProjectPaths.isBuildProjectRoot(projectPath)
                    || ProjectPaths.isStandaloneBalFile(projectPath)
                    || ProjectPaths.isWorkspaceProjectRoot(projectPath);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Optional<Path> getEnclosingPackageRoot() {
        try {
            if (!Files.isRegularFile(projectPath)
                    || !projectPath.toString().endsWith(ProjectConstants.BLANG_SOURCE_EXT)) {
                return Optional.empty();
            }
            return Optional.of(ProjectPaths.packageRoot(projectPath.toAbsolutePath().normalize()));
        } catch (RuntimeException ex) {
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

    private void addRules(List<Rule> rules) {
        for (Rule rule : rules) {
            if (allRuleIds.add(rule.id())) {
                allRules.add(rule);
            }
        }
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

    private record PackageScan(Project project, ScanTomlFile scanToml, ProjectAnalyzer analyzer,
                               List<Rule> coreRules, Map<String, List<Rule>> externalAnalyzers,
                               RuleSelection ruleSelection) {
    }

    private record PackageResult(PackageScan packageScan, List<Issue> issues, List<Issue> activeIssues,
                                 Map<Issue, SuppressedIssue> suppressions,
                                 ServiceLoader<StaticCodeAnalysisPlatformPlugin> platformPlugins,
                                 List<String> platforms, ScanFailure suppressionFailure) {
    }

    private static class StringToListConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(",", -1)).map(String::trim).toList();
        }
    }
}
