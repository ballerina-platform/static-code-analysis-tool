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
import io.ballerina.projects.Package;
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
import io.ballerina.scan.utils.AnalyzedSources;
import io.ballerina.scan.utils.Constants;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.GateSeverity;
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_RULE_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.COMBINING_RESULTS_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.LOADING_CONFIGURATIONS_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_PACKAGE_SCAN_RULES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_WORKSPACE_DEPENDENCIES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_WORKSPACE_SCAN_RULES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RUNNING_PACKAGE_SCANS_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RUNNING_SCANS_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.SCAN_COMMAND;
import static io.ballerina.scan.internal.ScanToolConstants.WORKSPACE_CONFIGURATIONS_LOG;
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
    private static final String EXCLUDE_TESTS_OPTION = "--exclude-tests";
    private static final String LIST_RULES_HINT = "  Run 'bal scan --list-rules' to see the available rules.";

    private final PrintStream outputStream;
    private final PrintStream errorStream;
    // Records the exit code instead of exiting, so that tests can assert it
    private Consumer<ScanFailure> exitHandler = this::recordExitCode;
    private int exitCode;
    // Rule tests run through the test runner inspect the issues, so a gate in their fixtures must not fail them
    private boolean gateEnabled = true;

    // Collects every positional argument, so that extra ones exit as a usage error instead of a launcher error
    @CommandLine.Parameters
    private List<String> targets = new ArrayList<>();

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

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

    // Written alone, the option means true; a value overrides [analysis] excludeTests either way. Validated when the
    // command runs, so that an invalid value exits as a usage error. The bal launcher runs on picocli 4.0.1, which
    // ignores the preprocessor, so the fallback value and resolveOptions cover the option written alone there
    @CommandLine.Option(names = EXCLUDE_TESTS_OPTION, arity = "0..1", fallbackValue = "true",
            preprocessor = OptionalBooleanValue.class,
            description = "Leave the test sources out of the scan (true/false). Default is false")
    private String excludeTestsOption;
    private Boolean excludeTests;

    private final List<Rule> allRules = new ArrayList<>();
    private final Set<String> allRuleIds = new HashSet<>();
    private final List<Issue> allIssues;
    private final Map<Issue, SuppressedIssue> allSuppressions = new IdentityHashMap<>();
    private final List<Issue> workspaceConsoleIssues = new ArrayList<>();
    private boolean printWorkspaceIssues;
    private boolean scanningWorkspace;
    private ReportFormat reportFormat = ReportFormat.BALLERINA;
    private GateSeverity gateSeverity;
    private Integer gateCount;
    // The defaults of the packages of a workspace, read once for all of them; null when the workspace has none
    private ScanTomlFile workspaceDefaults;
    private boolean workspaceDefaultsRead;
    private final Set<String> printedWarnings = new HashSet<>();
    // The sources each package is scanned with, keyed by its root, for the workspace HTML report
    private final Map<Path, AnalyzedSources> packageSources = new HashMap<>();
    // Each platform JAR is downloaded and loaded once for every package, and its loader is closed at the end
    private final Map<String, Path> platformJars = new HashMap<>();
    private final Map<Path, URLClassLoader> platformLoaders = new LinkedHashMap<>();
    // The --platforms names that at least one package declares
    private final Set<String> declaredPlatforms = new HashSet<>();
    // In a workspace the files are read quietly, so that each loading line can name the package it is for
    private final PrintStream quietStream = new PrintStream(OutputStream.nullOutputStream(), false,
            StandardCharsets.UTF_8);
    private boolean packageScansStarted;

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

    /**
     * Creates a scan command for test runners. The quality gate is off, so the issues of a fixture can be inspected
     * whatever its {@code [gate]} settings, and a failure is thrown as a {@link ScanToolException}, carrying the
     * problems that caused it in {@link ScanToolException#diagnostics()}, instead of exiting the JVM.
     * Before 0.13.0, failures were thrown as {@code BLauncherException}.
     */
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
        this.reportFormat = format == null ? ReportFormat.BALLERINA : format;
        this.format = this.reportFormat.getFormat();
        this.listRules = listRules;
        this.includeRules.addAll(includeRules.stream().map(Rule::id).toList());
        this.excludeRules.addAll(excludeRules.stream().map(Rule::id).toList());
        this.platforms.addAll(platforms);
        this.allIssues = new ArrayList<>();
        this.gateEnabled = false;
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

    /**
     * Runs the scan, reports a failure with its exit code, and releases the platform plugins.
     */
    @Override
    public void execute() {
        try {
            run();
        } catch (RuntimeException ex) {
            fail(ScanFailure.from(ex));
        } finally {
            closePlatformLoaders();
        }
    }

    /**
     * Closes the class loaders of the platform plugins. A loader keeps its JAR open, which on Windows locks the file
     * until the loader is closed.
     */
    private void closePlatformLoaders() {
        for (URLClassLoader loader : platformLoaders.values()) {
            try {
                loader.close();
            } catch (IOException ex) {
                // The scan is already over, and the JAR is released when the JVM exits
            }
        }
        platformLoaders.clear();
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

    /**
     * Prints a warning unless it was already printed, since packages of a workspace can share a setting.
     */
    private void warnOnce(String message) {
        if (printedWarnings.add(message)) {
            warn(message);
        }
    }

    /**
     * Loads the target and scans it as a workspace, a package or a single file.
     */
    private void run() {
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        resolveOptions();
        Project project = getProject();
        if (project.kind() == ProjectKind.WORKSPACE_PROJECT) {
            executeWorkspace((WorkspaceProject) project);
            return;
        }
        if (ProjectUtils.isProjectEmpty(project)) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.EMPTY_PACKAGE));
        }
        executeProject(project);
    }

    /**
     * Validates the command-line arguments, so that invalid input exits as a usage error before anything is loaded.
     */
    private void resolveOptions() {
        if (excludeTestsOption != null && !OptionalBooleanValue.isBoolean(excludeTestsOption)
                && isExcludeTestsWrittenAlone()) {
            targets.add(0, excludeTestsOption);
            excludeTestsOption = "true";
        }
        if (targets.size() > 1) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.INVALID_NUMBER_OF_ARGUMENTS, targets.size(),
                    String.join(" ", targets)));
        }
        if (!targets.isEmpty()) {
            try {
                projectPath = Paths.get(targets.get(0));
            } catch (InvalidPathException ex) {
                throw ScanFailure.invalidTarget(DiagnosticLog.error(DiagnosticCode.INVALID_PROJECT_PATH,
                        targets.get(0)), List.of());
            }
        }
        // The other options do not change the rules, so their values are not checked when listing them
        if (listRules) {
            return;
        }
        if (excludeTestsOption != null) {
            if (!excludeTestsOption.equalsIgnoreCase("true") && !excludeTestsOption.equalsIgnoreCase("false")) {
                throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.INVALID_OPTION_VALUE, excludeTestsOption,
                        EXCLUDE_TESTS_OPTION, "true or false"));
            }
            excludeTests = Boolean.parseBoolean(excludeTestsOption);
        }
        try {
            reportFormat = ReportFormat.fromString(format);
        } catch (IllegalArgumentException ex) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.INVALID_FORMAT, format));
        }
        if (!includeRules.isEmpty() && !excludeRules.isEmpty()) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.ATTEMPT_TO_INCLUDE_AND_EXCLUDE));
        }
        if (failOnSeverity != null) {
            gateSeverity = GateSeverity.parse(failOnSeverity).orElseThrow(() -> ScanFailure.usage(
                    DiagnosticLog.error(DiagnosticCode.INVALID_OPTION_VALUE, failOnSeverity, FAIL_ON_SEVERITY_OPTION,
                            GateSeverity.VALUES)));
        }
        if (failOnCount != null) {
            gateCount = ScanUtils.parseGateCount(failOnCount).orElseThrow(() -> ScanFailure.usage(
                    DiagnosticLog.error(DiagnosticCode.INVALID_OPTION_VALUE, failOnCount, FAIL_ON_COUNT_OPTION,
                            ScanUtils.GATE_COUNT_VALUES)));
        }
    }

    /**
     * Scans every package of a workspace. Every package's configuration is resolved before any is scanned, each
     * package's analyzers are loaded only while it is scanned, nothing is reported until all of them are scanned, and
     * each package is gated on its own issues.
     */
    private void executeWorkspace(WorkspaceProject workspaceProject) {
        errorStream.println();
        errorStream.println(listRules ? RESOLVING_WORKSPACE_SCAN_RULES_LOG : RESOLVING_WORKSPACE_DEPENDENCIES_LOG);
        scanningWorkspace = true;
        // Read before the packages, so that an error in it is reported once instead of for every package
        workspaceDefaults(workspaceProject);

        Map<BuildProject, ScanFailure> failures = new LinkedHashMap<>();
        Deque<PackageScan> packageScans = new ArrayDeque<>();
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
                ScanFailure failure = ScanFailure.from(ex);
                // A conflict with a command-line option fails the run once, before any package is scanned
                if (failure.exitCode() == ExitCode.USAGE_ERROR) {
                    throw failure;
                }
                failures.put(buildProject, failure);
            }
        }
        if (listRules) {
            listWorkspaceRules(workspaceProject, packageScans, failures);
            return;
        }
        if (failures.isEmpty()) {
            failOnUndeclaredWorkspacePlatforms();
        }
        // The analyzer rules are known only once each package is loaded, so only core rule IDs are checked up front
        addRules(CoreRule.rules());
        failOnUnknownRuleOptions(true);
        // The run fails anyway, so the other packages are not compiled and scanned
        failOnPackageFailures(workspaceProject, failures);

        // Each package's analyzers are loaded just before it is scanned and released after it, and its result once it
        // is reported
        Deque<PackageResult> results = new ArrayDeque<>();
        while (!packageScans.isEmpty()) {
            PackageScan packageScan = packageScans.poll();
            BuildProject buildProject = (BuildProject) packageScan.project();
            try {
                printRunningScans(buildProject);
                PackageResult result = scanPackage(packageScan, loadRules(packageScan));
                results.add(result);
                if (result.suppressionFailure() != null) {
                    failures.put(buildProject, result.suppressionFailure());
                }
            } catch (RuntimeException ex) {
                failures.put(buildProject, ScanFailure.from(ex));
            }
        }
        // A package that failed to load has no rules, so the analyzer rule IDs are complete only without failures
        if (failures.isEmpty()) {
            failOnUnknownRuleOptions(false);
        }
        // Nothing is reported until every package is scanned without an error
        failOnPackageFailures(workspaceProject, failures);
        errorStream.println();
        errorStream.println(COMBINING_RESULTS_LOG + results.size() + (results.size() == 1 ? " package" : " packages"));

        // A package that cannot report to its platforms does not stop the others from reporting
        int activeIssueCount = 0;
        List<String> gateFailures = new ArrayList<>();
        List<PlatformRun> platformRuns = new ArrayList<>();
        while (!results.isEmpty()) {
            PackageResult result = results.poll();
            Project packageProject = result.project();
            try {
                report(result);
            } catch (RuntimeException ex) {
                failures.put((BuildProject) packageProject, ScanFailure.from(ex));
            }
            result.platforms().forEach(platform ->
                    platformRuns.add(platformRun(packageProject, platform, result.activeIssues())));
            activeIssueCount += result.activeIssues().size();
            // Each package is gated on its own issues; errors win over the gate, so a failure is thrown last
            result.gate().evaluate(result.activeIssues())
                    .ifPresent(message -> gateFailures.add(packageError(workspaceProject, packageProject, message)));
        }
        // The packages' issues are printed as one document, so that standard output stays valid JSON
        if (printWorkspaceIssues) {
            ScanUtils.printToConsole(workspaceConsoleIssues, outputStream, ReportFormat.SARIF.equals(reportFormat),
                    workspaceProject);
        }
        errorStream.println();
        try {
            writeWorkspaceReports(workspaceProject);
        } catch (RuntimeException ex) {
            if (failures.isEmpty()) {
                throw ScanFailure.from(ex);
            }
            // The packages' failures are still reported, so a failed workspace report does not hide them
            ScanFailure.from(ex).reportLines().forEach(errorStream::println);
            failOnPackageFailures(workspaceProject, failures);
        }
        printSummary(activeIssueCount, allSuppressions.size());
        failOnPackageFailures(workspaceProject, failures);
        // Every package's platforms run one after another, after all the results are saved and before the gate
        reportToPlatforms(platformRuns);

        if (!gateFailures.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PACKAGES_FAILED_GATE, gateFailures.size()),
                    gateFailures);
        }
    }

    /**
     * Lists the rules of every package of a workspace, loading and releasing the analyzers of one package at a time.
     */
    private void listWorkspaceRules(WorkspaceProject workspaceProject, Deque<PackageScan> packageScans,
                                    Map<BuildProject, ScanFailure> failures) {
        while (!packageScans.isEmpty()) {
            PackageScan packageScan = packageScans.poll();
            try {
                loadRules(packageScan);
            } catch (RuntimeException ex) {
                failures.put((BuildProject) packageScan.project(), ScanFailure.from(ex));
            }
        }
        if (!allRules.isEmpty()) {
            errorStream.println();
            ScanUtils.printRulesToConsole(allRules, outputStream);
        }
        failOnPackageFailures(workspaceProject, failures);
    }

    /**
     * Returns the workspace Scan.toml file, read once for every package, or null when the workspace has none.
     */
    private ScanTomlFile workspaceDefaults(WorkspaceProject workspaceProject) {
        if (!workspaceDefaultsRead) {
            workspaceDefaultsRead = true;
            workspaceDefaults = ScanUtils.readWorkspaceScanToml(workspaceProject, quietStream, listRules).orElse(null);
            if (workspaceDefaults != null) {
                errorStream.println(LOADING_CONFIGURATIONS_LOG + "the workspace from "
                        + workspaceDefaults.getDisplayPath(workspaceProject.sourceRoot()).orElse(Constants.SCAN_FILE));
            }
        }
        return workspaceDefaults;
    }

    /**
     * Writes the combined results of a workspace, and its HTML report when asked for.
     */
    private void writeWorkspaceReports(WorkspaceProject workspaceProject) {
        accumulateWorkspaceReports(workspaceProject);
        if (scanReport) {
            Path scanReportPath = ScanUtils.generateScanReport(allIssues, workspaceProject, targetDir,
                    allSuppressions, this::analyzedSources);
            errorStream.println();
            errorStream.println("View scan report at:");
            errorStream.println("\t" + scanReportPath.toUri() + System.lineSeparator());
        }
    }

    private AnalyzedSources analyzedSources(Package analyzedPackage) {
        return packageSources.getOrDefault(packageKey(analyzedPackage.project()), AnalyzedSources.of(false));
    }

    private static Path packageKey(Project project) {
        return project.sourceRoot().toAbsolutePath().normalize();
    }

    /**
     * Fails the scan with the errors of every package that failed. Errors win over the gate, so this runs before the
     * gate is evaluated.
     */
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
        throw ScanFailure.of(exitCode, DiagnosticLog.error(DiagnosticCode.PACKAGES_FAILED, failures.size()),
                diagnostics);
    }

    private static String packageError(WorkspaceProject workspaceProject, Project packageProject, String message) {
        return packageLabel(workspaceProject, packageProject) + ": error: " + message;
    }

    private static String packageLabel(WorkspaceProject workspaceProject, Project packageProject) {
        return ScanUtils.displayPath(workspaceProject.sourceRoot(), packageProject.sourceRoot());
    }

    /**
     * Writes the issues of every package of a workspace to one results file.
     */
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
        PackageRules rules = loadRules(packageScan);
        failOnUnknownRuleOptions(false);
        if (listRules) {
            if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
                errorStream.println();
            }
            ScanUtils.printRulesToConsole(allRules, outputStream);
            return;
        }

        printRunningScans(project);
        PackageResult result = scanPackage(packageScan, rules);
        // Nothing is reported until the package meets the suppression requirements
        if (result.suppressionFailure() != null) {
            throw result.suppressionFailure();
        }
        report(result);
        printSummary(result.activeIssues().size(), result.suppressions().size());
        // The results are saved before the platforms run, and a failed platform wins over the gate
        reportToPlatforms(result.platforms().stream()
                .map(platform -> platformRun(project, platform, result.activeIssues())).toList());
        result.gate().evaluate(result.activeIssues()).ifPresent(message -> {
            throw ScanFailure.runtime(message);
        });
    }

    /**
     * Prepares a package for scanning: resolves its configuration and selects its platforms. Its analyzers are loaded
     * separately, by {@link #loadRules}, so that a workspace holds the analyzers of one package at a time.
     *
     * @param project the package or single file project
     * @return the prepared package
     */
    private PackageScan preparePackage(Project project) {
        ScanTomlFile scanTomlFile = chooseScanToml(project);
        PackageConfig config = resolveConfig(project, scanTomlFile);
        packageSources.put(packageKey(project), config.analyzedSources());

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT && !listRules) {
            printUnsupportedSingleFileFlagWarnings();
        }
        List<SelectedPlatform> selectedPlatforms = listRules ? List.of() : selectPlatforms(project, scanTomlFile);
        return new PackageScan(project, scanTomlFile, config, selectedPlatforms);
    }

    /**
     * Loads the analyzers of a package and adds their rules, with the core rules, to the rules of the scan.
     *
     * @param packageScan the prepared package
     * @return the analyzer and rules of the package
     */
    private PackageRules loadRules(PackageScan packageScan) {
        Project project = packageScan.project();
        ScanTomlFile scanTomlFile = packageScan.scanTomlFile();
        if (listRules && project.kind() == ProjectKind.BUILD_PROJECT && !scanningWorkspace) {
            errorStream.println();
            errorStream.println(RESOLVING_PACKAGE_SCAN_RULES_LOG);
        }

        ProjectAnalyzer projectAnalyzer = getProjectAnalyzer(project, scanTomlFile,
                packageScan.config().analyzedSources());
        List<Rule> coreRules = CoreRule.rules();
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        addRules(coreRules);
        externalAnalyzers.values().forEach(this::addRules);

        boolean usesWorkspaceFile = scanningWorkspace && workspaceDefaults != null && scanTomlFile == workspaceDefaults;
        if (!listRules && usesWorkspaceFile) {
            warnUnavailableWorkspaceRules(project, scanTomlFile, ProjectAnalyzer.rules(coreRules, externalAnalyzers));
        }
        return new PackageRules(projectAnalyzer, coreRules, externalAnalyzers);
    }

    /**
     * Applies the command-line options to the Scan.toml file a package uses, once, warning about each setting an
     * option overrides. The analysis, suppressions, reports and gate of the package all use the result.
     *
     * @param project      the package or single file project
     * @param scanTomlFile the Scan.toml file the package uses
     * @return the effective configuration of the package
     */
    private PackageConfig resolveConfig(Project project, ScanTomlFile scanTomlFile) {
        Path root = scanningWorkspace ? project.workspaceProject().orElseThrow().sourceRoot() : project.sourceRoot();
        String scanTomlLabel = scanTomlFile.getDisplayPath(root).orElse(Constants.SCAN_FILE);

        if (!listRules && excludeTests != null) {
            warnOverride(scanTomlFile, ScanTomlFile.ANALYSIS_EXCLUDE_TESTS_SETTING,
                    EXCLUDE_TESTS_OPTION + "=" + excludeTests, String.valueOf(excludeTests), scanTomlLabel);
        }
        AnalyzedSources analyzedSources = AnalyzedSources.of(excludeTests != null ? excludeTests
                : scanTomlFile.isExcludeTests());
        if (!listRules) {
            warnSuppressionOverrides(scanTomlFile, scanTomlLabel);
        }
        SuppressionOptions suppressionOptions = SuppressionOptions.of(scanTomlFile, analyzedSources,
                ignoreSuppressions, requireSuppressionReason, requireSuppressionRuleId);
        RuleSelection ruleSelection = resolveRules(scanTomlFile, scanTomlLabel);
        QualityGate gate = listRules ? QualityGate.OFF : resolveGate(scanTomlFile, scanTomlLabel);
        return new PackageConfig(analyzedSources, ruleSelection, suppressionOptions, gate);
    }

    /**
     * Returns the Scan.toml file a package uses: its own file as a whole, or the workspace one when it has none. The
     * two are never merged.
     */
    private ScanTomlFile chooseScanToml(Project project) {
        if (!scanningWorkspace) {
            ScanTomlFile packageFile = ScanUtils.readScanTomlConfigurations(project, errorStream, listRules);
            // A workspace package scanned on its own uses the workspace file too, as it does in the editor
            if (packageFile.getPath().isEmpty()) {
                return ScanUtils.readEnclosingWorkspaceScanToml(project, errorStream, listRules).orElse(packageFile);
            }
            return packageFile;
        }
        WorkspaceProject workspace = project.workspaceProject().orElseThrow();
        String label = packageLabel(workspace, project);
        ScanTomlFile packageFile = ScanUtils.readScanTomlConfigurations(project, quietStream, listRules);
        ScanTomlFile workspaceFile = workspaceDefaults(workspace);
        if (packageFile.getPath().isPresent()) {
            errorStream.println(LOADING_CONFIGURATIONS_LOG + label + " from "
                    + packageFile.getDisplayPath(workspace.sourceRoot()).orElse(Constants.SCAN_FILE));
        } else if (workspaceFile != null) {
            errorStream.println(WORKSPACE_CONFIGURATIONS_LOG + label);
        }
        if (workspaceFile == null) {
            return packageFile;
        }
        if (packageFile.getPath().isEmpty()) {
            return workspaceFile;
        }
        if (!listRules && !packageFile.getPath().equals(workspaceFile.getPath())) {
            warnWorkspaceFileOverridden(workspace, project, packageFile, workspaceFile);
        }
        return packageFile;
    }

    /**
     * Warns about each workspace setting that a package's own Scan.toml leaves out, since the package file replaces the
     * workspace one as a whole.
     */
    private void warnWorkspaceFileOverridden(WorkspaceProject workspaceProject, Project project,
                                             ScanTomlFile packageFile, ScanTomlFile workspaceFile) {
        // A setting the package writes itself is overridden on purpose, so only the ones it leaves out are listed
        Map<String, String> packageSettings = packageFile.getWrittenSettings();
        List<String> notApplied = workspaceFile.getWrittenSettings().entrySet().stream()
                .filter(setting -> !packageSettings.containsKey(setting.getKey()))
                .map(setting -> setting.getValue().isEmpty() ? setting.getKey()
                        : setting.getKey() + " = " + setting.getValue())
                .toList();
        if (notApplied.isEmpty()) {
            return;
        }
        Path root = workspaceProject.sourceRoot();
        warn(DiagnosticLog.warning(DiagnosticCode.SCAN_FILE_OVERRIDDEN,
                packageFile.getDisplayPath(root).orElse(Constants.SCAN_FILE),
                workspaceFile.getDisplayPath(root).orElse(Constants.SCAN_FILE),
                packageLabel(workspaceProject, project), String.join(", ", notApplied)));
    }

    // Stage 2: an option replaces one field, and warns only when the chosen file writes that field differently
    private void warnOverride(ScanTomlFile scanTomlFile, String setting, String option, String value,
                              String scanTomlLabel) {
        scanTomlFile.getWrittenValue(setting)
                .filter(writtenValue -> !writtenValue.equals(value))
                .ifPresent(writtenValue -> warnOnce(DiagnosticLog.warning(DiagnosticCode.SETTING_OVERRIDDEN, option,
                        setting + " = " + writtenValue, scanTomlLabel)));
    }

    /**
     * Warns about each suppression setting of the Scan.toml file that a command-line option overrides.
     */
    private void warnSuppressionOverrides(ScanTomlFile scanTomlFile, String scanTomlLabel) {
        if (ignoreSuppressions) {
            warnOverride(scanTomlFile, ScanTomlFile.SUPPRESSION_ENABLED_SETTING, "--ignore-suppressions", "false",
                    scanTomlLabel);
        }
        if (requireSuppressionReason) {
            warnOverride(scanTomlFile, ScanTomlFile.SUPPRESSION_REQUIRE_REASON_SETTING,
                    "--require-suppression-reason", "true", scanTomlLabel);
        }
        if (requireSuppressionRuleId) {
            warnOverride(scanTomlFile, ScanTomlFile.SUPPRESSION_REQUIRE_RULE_ID_SETTING,
                    "--require-suppression-rule-id", "true", scanTomlLabel);
        }
    }

    /**
     * Selects the rules of a package. Each rule option replaces its own setting, so an option and the other setting of
     * the file can conflict.
     */
    private RuleSelection resolveRules(ScanTomlFile scanTomlFile, String scanTomlLabel) {
        List<String> tomlIncludeRules = scanTomlFile.getRulesToInclude().stream()
                .map(ScanTomlFile.RuleToFilter::id).toList();
        List<String> tomlExcludeRules = scanTomlFile.getRulesToExclude().stream()
                .map(ScanTomlFile.RuleToFilter::id).toList();
        List<String> include = includeRules.isEmpty() ? tomlIncludeRules : includeRules;
        List<String> exclude = excludeRules.isEmpty() ? tomlExcludeRules : excludeRules;
        if (listRules) {
            return new RuleSelection(new ArrayList<>(include), new ArrayList<>(exclude));
        }
        String includeOption = "--include-rules=" + String.join(",", includeRules);
        String excludeOption = "--exclude-rules=" + String.join(",", excludeRules);
        if (!includeRules.isEmpty() && !new HashSet<>(includeRules).equals(new HashSet<>(tomlIncludeRules))) {
            warnOverride(scanTomlFile, ScanTomlFile.RULE_INCLUDE_SETTING, includeOption,
                    ScanTomlFile.quotedList(includeRules), scanTomlLabel);
        }
        if (!excludeRules.isEmpty() && !new HashSet<>(excludeRules).equals(new HashSet<>(tomlExcludeRules))) {
            warnOverride(scanTomlFile, ScanTomlFile.RULE_EXCLUDE_SETTING, excludeOption,
                    ScanTomlFile.quotedList(excludeRules), scanTomlLabel);
        }
        // Both from the command line is caught earlier, and both from the file is a configuration error
        if (!include.isEmpty() && !exclude.isEmpty()) {
            String conflict = includeRules.isEmpty()
                    ? DiagnosticLog.error(DiagnosticCode.RULE_OPTION_CONFLICT, excludeOption,
                    ScanTomlFile.RULE_INCLUDE_SETTING + " = " + ScanTomlFile.quotedList(tomlIncludeRules),
                    scanTomlLabel)
                    : DiagnosticLog.error(DiagnosticCode.RULE_OPTION_CONFLICT, includeOption,
                    ScanTomlFile.RULE_EXCLUDE_SETTING + " = " + ScanTomlFile.quotedList(tomlExcludeRules),
                    scanTomlLabel);
            throw ScanFailure.usage(conflict);
        }
        return new RuleSelection(new ArrayList<>(include), new ArrayList<>(exclude));
    }

    /**
     * Warns about the rules of the workspace [rule] table that a package does not provide, since they may come from an
     * analyzer that only some packages use.
     */
    private void warnUnavailableWorkspaceRules(Project project, ScanTomlFile workspaceFile, List<Rule> packageRules) {
        Set<String> packageRuleIds = new HashSet<>();
        packageRules.forEach(rule -> packageRuleIds.add(rule.id()));
        Stream<ScanTomlFile.RuleToFilter> includes = includeRules.isEmpty()
                ? workspaceFile.getRulesToInclude().stream() : Stream.empty();
        Stream<ScanTomlFile.RuleToFilter> excludes = excludeRules.isEmpty()
                ? workspaceFile.getRulesToExclude().stream() : Stream.empty();
        List<String> unavailableRules = Stream.concat(includes, excludes)
                .map(ScanTomlFile.RuleToFilter::id)
                .filter(ruleId -> !packageRuleIds.contains(ruleId))
                .distinct()
                .map(ruleId -> "'" + ruleId + "'")
                .toList();
        if (unavailableRules.isEmpty()) {
            return;
        }
        WorkspaceProject workspaceProject = project.workspaceProject().orElseThrow();
        warn(DiagnosticLog.warning(DiagnosticCode.WORKSPACE_RULE_NOT_AVAILABLE, joinWithAnd(unavailableRules),
                workspaceFile.getDisplayPath(workspaceProject.sourceRoot()).orElse(Constants.SCAN_FILE),
                unavailableRules.size(), packageLabel(workspaceProject, project)));
    }

    /**
     * Finds the plugins of the platforms named in {@code --platforms} that the package's Scan.toml declares. A
     * platform is never reported to unless it is named on the command line.
     */
    private List<SelectedPlatform> selectPlatforms(Project project, ScanTomlFile scanTomlFile) {
        Map<String, ScanTomlFile.Platform> declared = new LinkedHashMap<>();
        scanTomlFile.getPlatforms().forEach(platform -> declared.putIfAbsent(platform.name(), platform));
        // A platform that triggers the scan reports to the platforms the package declares, unless it names some
        List<String> requested = platforms.isEmpty() && platformTriggered ? List.copyOf(declared.keySet())
                : platforms.stream().distinct().toList();
        if (requested.isEmpty()) {
            return List.of();
        }
        List<String> undeclared = requested.stream().filter(name -> !declared.containsKey(name))
                .map(name -> "'" + name + "'").toList();
        if (!undeclared.isEmpty()) {
            if (!scanningWorkspace) {
                throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_NOT_AVAILABLE,
                        undeclared.size(), joinWithAnd(undeclared)));
            }
            WorkspaceProject workspaceProject = project.workspaceProject().orElseThrow();
            warn(DiagnosticLog.warning(DiagnosticCode.PLATFORM_NOT_DECLARED,
                    packageLabel(workspaceProject, project), undeclared.size(), joinWithAnd(undeclared)));
        }
        List<ScanTomlFile.Platform> selected = requested.stream().filter(declared::containsKey)
                .map(declared::get).toList();
        if (selected.isEmpty()) {
            return List.of();
        }
        declaredPlatforms.addAll(selected.stream().map(ScanTomlFile.Platform::name).toList());

        // Each JAR is loaded on its own, so that a broken one is blamed on the platform that names it
        Map<Path, Map<String, StaticCodeAnalysisPlatformPlugin>> pluginsByJar = new HashMap<>();
        List<SelectedPlatform> selectedPlatforms = new ArrayList<>();
        for (ScanTomlFile.Platform platform : selected) {
            Path jarPath = platformJars.get(platform.path());
            if (jarPath == null) {
                jarPath = ScanUtils.resolvePlatformJar(platform, project.targetDir(), errorStream);
                platformJars.put(platform.path(), jarPath);
            }
            Map<String, StaticCodeAnalysisPlatformPlugin> plugins = pluginsByJar.get(jarPath);
            if (plugins == null) {
                plugins = loadPlatformPlugins(platform.name(), jarPath);
                pluginsByJar.put(jarPath, plugins);
            }
            StaticCodeAnalysisPlatformPlugin plugin = plugins.get(platform.name());
            if (plugin == null) {
                throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_NOT_PROVIDED, platform.name(),
                        jarPath));
            }
            Map<String, String> platformArgs = new HashMap<>();
            platform.arguments().forEach((key, value) -> platformArgs.put(key, value.toString()));
            selectedPlatforms.add(new SelectedPlatform(platform.name(), plugin,
                    new PlatformPluginContextImpl(platformArgs, platformTriggered)));
        }
        return selectedPlatforms;
    }

    /**
     * Fails the scan when no package declares a platform named in --platforms. A package may leave out a named
     * platform, but at least one package must declare it.
     */
    private void failOnUndeclaredWorkspacePlatforms() {
        List<String> missing = platforms.stream().distinct().filter(name -> !declaredPlatforms.contains(name))
                .map(name -> "'" + name + "'").toList();
        if (!missing.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_NOT_AVAILABLE, missing.size(),
                    joinWithAnd(missing)));
        }
    }

    /**
     * Reports the issues to every selected platform, one after another. Each failure is
     * collected, so that one failing platform does not hide another.
     */
    private void reportToPlatforms(List<PlatformRun> runs) {
        if (runs.isEmpty()) {
            return;
        }
        errorStream.println();
        runs.forEach(run -> errorStream.println("Reporting issues to: " + run.label()));
        List<Optional<String>> outcomes = PlatformRunner.runAll(runs.stream()
                .map(run -> new PlatformRunner.Task(run.platform().plugin(), run.platform().context(), run.issues()))
                .toList());
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < runs.size(); i++) {
            PlatformRun run = runs.get(i);
            outcomes.get(i).ifPresent(reason -> errors.add(run.error(reason)));
        }
        if (!errors.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORMS_FAILED, errors.size()), errors);
        }
    }

    private PlatformRun platformRun(Project project, SelectedPlatform platform, List<Issue> issues) {
        String label = project.workspaceProject()
                .map(workspaceProject -> packageLabel(workspaceProject, project))
                .orElse(null);
        return new PlatformRun(platform, issues, label);
    }

    /**
     * Fails the scan when a rule named on the command line is not a rule of any scanned package. With coreRulesOnly,
     * only the core rule IDs are checked, as the rules of a package that failed to prepare are not known.
     */
    private void failOnUnknownRuleOptions(boolean coreRulesOnly) {
        if (listRules) {
            return;
        }
        List<String> unknownRules = Stream.concat(includeRules.stream(), excludeRules.stream())
                .filter(ruleId -> !allRuleIds.contains(ruleId))
                .filter(ruleId -> !coreRulesOnly || ruleId.startsWith(BALLERINA_RULE_PREFIX))
                .distinct()
                .map(ruleId -> "'" + ruleId + "'")
                .toList();
        if (!unknownRules.isEmpty()) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.UNKNOWN_RULE_OPTION,
                    joinWithAnd(unknownRules), unknownRules.size()), LIST_RULES_HINT);
        }
    }

    /**
     * Returns the gate of a package. Each gate option replaces its own setting, and the gate is off when a platform
     * triggers the scan, since the platform applies its own quality gate.
     */
    private QualityGate resolveGate(ScanTomlFile scanTomlFile, String scanTomlLabel) {
        if (platformTriggered || !gateEnabled) {
            return QualityGate.OFF;
        }
        if (gateSeverity != null) {
            warnOverride(scanTomlFile, ScanTomlFile.GATE_FAIL_ON_SEVERITY_SETTING,
                    FAIL_ON_SEVERITY_OPTION + "=" + gateSeverity, "\"" + gateSeverity + "\"", scanTomlLabel);
        }
        if (gateCount != null) {
            warnOverride(scanTomlFile, ScanTomlFile.GATE_FAIL_ON_COUNT_SETTING, FAIL_ON_COUNT_OPTION + "=" + gateCount,
                    String.valueOf(gateCount), scanTomlLabel);
        }
        GateSeverity severity = gateSeverity != null ? gateSeverity : scanTomlFile.getFailOnSeverity().orElse(null);
        Integer count = gateCount != null ? gateCount : scanTomlFile.getFailOnCount().orElse(null);
        if (gateCount != null && severity == GateSeverity.NONE) {
            String turnedOffBy = gateSeverity != null ? FAIL_ON_SEVERITY_OPTION + "=" + gateSeverity
                    : ScanTomlFile.GATE_FAIL_ON_SEVERITY_SETTING + " = \"" + severity + "\" in " + scanTomlLabel;
            warnOnce(DiagnosticLog.warning(DiagnosticCode.GATE_COUNT_IGNORED, FAIL_ON_COUNT_OPTION + "=" + gateCount,
                    turnedOffBy));
        }
        return QualityGate.of(severity, count);
    }

    private void printRunningScans(Project project) {
        if (!scanningWorkspace) {
            errorStream.println();
            errorStream.println(RUNNING_SCANS_LOG);
            return;
        }
        // The packages' lines form one block, so that it reads as one list
        if (!packageScansStarted) {
            packageScansStarted = true;
            errorStream.println();
        }
        errorStream.println(RUNNING_PACKAGE_SCANS_LOG
                + packageLabel(project.workspaceProject().orElseThrow(), project));
    }

    /**
     * Runs the analyzers on a prepared package, and applies its rule selection and suppressions.
     */
    private PackageResult scanPackage(PackageScan packageScan, PackageRules rules) {
        Project project = packageScan.project();
        ProjectAnalyzer projectAnalyzer = rules.analyzer();
        List<Issue> externalIssues = projectAnalyzer.runExternalAnalyzers(rules.externalAnalyzers());
        failOnCompilationErrors(projectAnalyzer);

        List<Issue> issues = projectAnalyzer.analyze(rules.coreRules());
        issues.addAll(externalIssues);

        PackageConfig config = packageScan.config();
        RuleSelection ruleSelection = config.ruleSelection();
        issues.removeIf(issue -> !ruleSelection.selects(issue.rule().id()));

        SuppressionOutcome suppressionOutcome = SuppressionFilter.apply(project, issues,
                ProjectAnalyzer.rules(rules.coreRules(), rules.externalAnalyzers()), ruleSelection,
                config.suppressionOptions());
        printSuppressionWarnings(suppressionOutcome.diagnostics());
        return new PackageResult(project, issues, suppressionOutcome.activeIssues(),
                suppressionOutcome.suppressionsByIssue(), packageScan.platforms(), config.gate(),
                suppressionFailure(suppressionOutcome));
    }

    /**
     * Prints the active issues of a package and saves its results and HTML report, unless a platform triggered the
     * scan.
     */
    private void report(PackageResult result) {
        Project project = result.project();
        List<Issue> activeIssues = result.activeIssues();
        Map<Issue, SuppressedIssue> suppressions = result.suppressions();
        allIssues.addAll(result.issues());
        allSuppressions.putAll(suppressions);

        // The results are always printed and saved, even when they are also reported to platforms
        if (!platformTriggered) {
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

                if (!scanningWorkspace) {
                    errorStream.println();
                    errorStream.println("View scan results at:");
                    errorStream.println("\t" + reportPath.toUri());
                    errorStream.println();
                    if (scanReport) {
                        Path scanReportPath = ScanUtils.generateScanReport(result.issues(), project, targetDir,
                                suppressions, this::analyzedSources);
                        errorStream.println();
                        errorStream.println("View scan report at:");
                        errorStream.println("\t" + scanReportPath.toUri() + System.lineSeparator());
                    }
                }
            }
        }
    }

    /**
     * Fails the scan when the package does not compile, as its issues would not be reliable.
     */
    private static void failOnCompilationErrors(ProjectAnalyzer projectAnalyzer) {
        List<Diagnostic> compilationErrors = projectAnalyzer.getCompilationErrors();
        if (compilationErrors.isEmpty()) {
            return;
        }
        throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.COMPILATION_CONTAINS_ERRORS),
                compilationErrors.stream().map(ScanCmd::formatDiagnostic).toList());
    }

    /**
     * Returns the failure for the suppressions that do not meet the requirements, or null when every suppression meets
     * them.
     */
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
                invalidSuppressionCount), diagnostics);
    }

    /**
     * Formats a compiler diagnostic with its file and position, when it has them.
     */
    private static String formatDiagnostic(Diagnostic diagnostic) {
        Location location = diagnostic.location();
        LineRange lineRange = location == null ? null : location.lineRange();
        if (lineRange == null || lineRange.fileName() == null || lineRange.fileName().isEmpty()) {
            return "error: " + diagnostic.message();
        }
        return DiagnosticLog.fileError(lineRange.fileName(), lineRange, diagnostic.message());
    }

    /**
     * Joins items as a list in prose, such as "a, b and c".
     */
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

    /**
     * Warns about each option that has no effect on a single Ballerina file.
     */
    private void printUnsupportedSingleFileFlagWarnings() {
        if (targetDir != null) {
            warn(DiagnosticLog.warning(DiagnosticCode.REPORT_NOT_SUPPORTED));
        }

        if (scanReport) {
            warn(DiagnosticLog.warning(DiagnosticCode.SCAN_REPORT_NOT_SUPPORTED));
        }

        if (Boolean.TRUE.equals(excludeTests)) {
            warn(DiagnosticLog.warning(DiagnosticCode.EXCLUDE_TESTS_NOT_SUPPORTED));
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
    protected Project getProject() {
        Project project = loadProject();
        ScanUtils.requireLockedDependencies(project);
        return project;
    }

    /**
     * Loads the package, workspace or file to scan, failing as an invalid target when the path is not one, and as a
     * runtime error when it cannot be loaded.
     */
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
            throw ScanFailure.runtime(loadFailure, List.of("error: " + ScanFailure.describe(ex)));
        }
        if (loadResult.diagnostics().hasErrors()) {
            throw ScanFailure.runtime(loadFailure,
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

    /**
     * Creates the analyzer of a package.
     *
     * @param project         the package or single file project
     * @param scanTomlFile    the Scan.toml file the package uses
     * @param analyzedSources the sources to analyze, with the command-line options applied
     * @return the analyzer
     * @since 0.13.0
     */
    protected ProjectAnalyzer getProjectAnalyzer(Project project, ScanTomlFile scanTomlFile,
                                                 AnalyzedSources analyzedSources) {
        return new ProjectAnalyzer(project, scanTomlFile, analyzedSources);
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

    /**
     * Loads the platform plugins a JAR provides, keyed by the platform each one reports to.
     *
     * @param platformName the platform that names the JAR, which a failure is reported for
     * @param jarPath      the local JAR
     * @return the plugins of the JAR
     */
    private Map<String, StaticCodeAnalysisPlatformPlugin> loadPlatformPlugins(String platformName, Path jarPath) {
        Map<String, StaticCodeAnalysisPlatformPlugin> plugins = new HashMap<>();
        try {
            URLClassLoader classLoader = platformLoaders.get(jarPath);
            if (classLoader == null) {
                classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());
                platformLoaders.put(jarPath, classLoader);
            }
            // Each package gets its own plugin instances, so that a plugin can keep state for the package it reports
            ServiceLoader.load(StaticCodeAnalysisPlatformPlugin.class, classLoader)
                    .forEach(plugin -> plugins.putIfAbsent(plugin.platform(), plugin));
        } catch (MalformedURLException | ServiceConfigurationError | LinkageError | RuntimeException ex) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_FAILED, platformName,
                    ScanFailure.describe(ex)));
        }
        return plugins;
    }

    private record PackageScan(Project project, ScanTomlFile scanTomlFile, PackageConfig config,
                               List<SelectedPlatform> platforms) {
    }

    private record PackageRules(ProjectAnalyzer analyzer, List<Rule> coreRules,
                                Map<String, List<Rule>> externalAnalyzers) {
    }

    /**
     * The effective configuration of a package: the Scan.toml file it uses with the command-line options applied.
     *
     * @param analyzedSources    the sources to analyze
     * @param ruleSelection      the rules to report
     * @param suppressionOptions how suppressions are applied
     * @param gate               the gate the package's issues are checked against
     */
    private record PackageConfig(AnalyzedSources analyzedSources, RuleSelection ruleSelection,
                                 SuppressionOptions suppressionOptions, QualityGate gate) {
    }

    // Holds the project, not the PackageScan, so that the analyzer and rules can be released after the scan
    private record PackageResult(Project project, List<Issue> issues, List<Issue> activeIssues,
                                 Map<Issue, SuppressedIssue> suppressions, List<SelectedPlatform> platforms,
                                 QualityGate gate, ScanFailure suppressionFailure) {
    }

    private record SelectedPlatform(String name, StaticCodeAnalysisPlatformPlugin plugin,
                                    PlatformPluginContext context) {
    }

    /**
     * A platform to report the issues of a package to.
     *
     * @param platform     the platform
     * @param issues       the active issues of the package
     * @param packageLabel the package as shown to the user, or null outside a workspace
     */
    private record PlatformRun(SelectedPlatform platform, List<Issue> issues, String packageLabel) {
        String label() {
            return packageLabel == null ? platform.name() : platform.name() + " (" + packageLabel + ")";
        }

        String error(String reason) {
            return (packageLabel == null ? "" : packageLabel + ": ") + "error: "
                    + DiagnosticLog.error(DiagnosticCode.PLATFORM_FAILED, platform.name(), reason);
        }
    }

    /**
     * Reads the value of an option that takes an optional boolean, as picocli does for a boolean option: a separate
     * argument is its value only when it is true or false, so that {@code --exclude-tests main.bal} scans
     * {@code main.bal}. A value attached with {@code =} is kept as given, so that an invalid one is reported by the
     * command instead of by picocli.
     */
    /**
     * Returns whether {@code --exclude-tests} was written without an attached value, in which case picocli 4.0.1
     * takes the next argument as its value even when it is the target.
     */
    private boolean isExcludeTestsWrittenAlone() {
        CommandLine.ParseResult parseResult = spec == null ? null : spec.commandLine().getParseResult();
        return parseResult != null && parseResult.originalArgs().contains(EXCLUDE_TESTS_OPTION);
    }

    private static class OptionalBooleanValue implements CommandLine.IParameterPreprocessor {
        /**
         * Pushes the implicit true when the option is written alone or before an argument that is not a boolean.
         */
        @Override
        public boolean preprocess(Stack<String> args, CommandLine.Model.CommandSpec commandSpec,
                                  CommandLine.Model.ArgSpec argSpec, Map<String, Object> info) {
            boolean attached = !" ".equals(info.get("separator"));
            if (!attached && (args.isEmpty() || !isBoolean(args.peek()))) {
                args.push("true");
            }
            return false;
        }

        /**
         * Returns whether an argument is a boolean value, in any case.
         */
        private static boolean isBoolean(String value) {
            return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
        }
    }

    private static class StringToListConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(",", -1)).map(String::trim).toList();
        }
    }
}
