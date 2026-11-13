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
import io.ballerina.projects.util.ProjectPaths;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.Issue;
import io.ballerina.scan.PlatformPluginContext;
import io.ballerina.scan.ReportFormat;
import io.ballerina.scan.Rule;
import io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.internal.ConsoleLayout.Section;
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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ballerina.scan.internal.ScanToolConstants.RESOLVING_PACKAGE_SCAN_RULES_LOG;
import static io.ballerina.scan.internal.ScanToolConstants.RULE_ID_PATTERN;
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
    private static final String EXCLUDE_TESTS_OPTION = "--exclude-tests";
    private static final String LIST_RULES_HINT = "  Run 'bal scan --list-rules' to see the available rules.";
    private static final String PACKAGE_SCOPE = "the package";
    // The JSON report has been named both json and ballerina, and --help documents json
    private static final String JSON_FORMAT = "json";

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

    // A String, as the bal launcher's picocli 4.0.1 would let a Boolean swallow a target; resolveOptions validates
    // the value and gives back a target the option took as its value
    @CommandLine.Option(names = EXCLUDE_TESTS_OPTION, arity = "0..1", fallbackValue = "true",
            description = "Leave the test sources out of the scan (true/false). Default is false")
    private String excludeTestsOption;
    private Boolean excludeTests;

    private final List<Rule> allRules = new ArrayList<>();
    private final Set<String> allRuleIds = new HashSet<>();
    private ReportFormat reportFormat = ReportFormat.BALLERINA;
    private GateSeverity gateSeverity;
    private Integer gateCount;
    // Each platform JAR is loaded once, and its loader is closed at the end
    private final Map<Path, URLClassLoader> platformLoaders = new LinkedHashMap<>();
    private final ConsoleLayout layout = new ConsoleLayout();
    // Warnings are held until the next section, so that the configuration lines are printed as one block
    private final List<String> pendingWarnings = new ArrayList<>();

    public ScanCmd() {
        this(System.out, System.err);
        this.exitHandler = failure -> Runtime.getRuntime().exit(failure.exitCode().code());
    }

    ScanCmd(PrintStream outputStream) {
        this(outputStream, outputStream);
    }

    /**
     * Creates a scan command that writes to the given streams. When they are the same stream, they are tracked as one,
     * so that the console layout counts their lines together.
     */
    ScanCmd(PrintStream outputStream, PrintStream errorStream) {
        this.projectPath = Paths.get(System.getProperty(ProjectConstants.USER_DIR));
        this.outputStream = layout.track(outputStream);
        this.errorStream = errorStream == outputStream ? this.outputStream : layout.track(errorStream);
        // Standard output holds only the results, so that it can be redirected to a file
        layout.separateWith(errorStream);
    }

    /**
     * Creates a scan command for test runners. The quality gate is off, so the issues of a fixture can be inspected
     * whatever its {@code [gate]} settings, and a failure is thrown as a {@link ScanToolException}, carrying the
     * problems that caused it in {@link ScanToolException#diagnostics()}, instead of exiting the JVM.
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
        this.outputStream = layout.track(Objects.requireNonNull(outputStream, "outputStream"));
        this.errorStream = this.outputStream;
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

    // Deprecated in BLauncherCmd but still abstract there, so it must be implemented
    @Override
    @SuppressWarnings("deprecation")
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
        // Standard output holds only the results. Anything else printed to System.out, such as by a compiler plugin
        // while the package compiles, is sent to standard error. With one stream for both, as in the test runner,
        // System.out is left to the caller. System.err already goes to standard error.
        PrintStream systemOut = System.out;
        boolean redirectSystemOut = errorStream != outputStream;
        if (redirectSystemOut) {
            System.setOut(errorStream);
        }
        try {
            run();
        } catch (RuntimeException ex) {
            fail(ScanFailure.from(ex));
        } finally {
            // An Error, such as a LinkageError from a plugin, skips the failure report, so the warnings are printed
            // here, as they are often the context for it
            flushWarnings();
            closePlatformLoaders();
            if (redirectSystemOut) {
                errorStream.flush();
                System.setOut(systemOut);
            }
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
        section(Section.FAILURE);
        failure.reportLines().forEach(errorStream::println);
        errorStream.flush();
        exitHandler.accept(failure);
    }

    private void warn(String message) {
        pendingWarnings.add(ScanFailure.warning(message));
    }

    /**
     * Starts a section of the console output. The held warnings are printed first, except while the configuration is
     * loaded, so that those lines stay together.
     */
    private void section(Section section) {
        if (section != Section.LOADING) {
            flushWarnings();
        }
        layout.enter(section);
    }

    /**
     * Prints the held warnings as a section of their own.
     */
    private void flushWarnings() {
        if (pendingWarnings.isEmpty()) {
            return;
        }
        layout.enter(Section.WARNINGS);
        pendingWarnings.forEach(errorStream::println);
        pendingWarnings.clear();
    }

    /**
     * Loads the target and scans it as a package or a single file.
     */
    private void run() {
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        resolveOptions();
        Project project = getProject();
        if (ProjectUtils.isProjectEmpty(project)) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.EMPTY_PACKAGE));
        }
        ScanUtils.requireLockedDependencies(project);
        executeProject(project);
    }

    /**
     * Validates the command-line arguments, so that invalid input exits as a usage error before anything is loaded.
     */
    private void resolveOptions() {
        if (excludeTestsOption != null && !isBoolean(excludeTestsOption) && isExcludeTestsWrittenAlone()) {
            // picocli takes an unknown option after --exclude-tests as its value, so it is reported here instead
            if (excludeTestsOption.startsWith("-")) {
                throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.UNKNOWN_OPTION, excludeTestsOption));
            }
            targets.add(excludeTestsValuePosition(), excludeTestsOption);
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
        if (!platforms.isEmpty() && isStandaloneBalFile()) {
            throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.PLATFORMS_NOT_SUPPORTED));
        }
        if (excludeTestsOption != null) {
            if (!isBoolean(excludeTestsOption)) {
                throw ScanFailure.usage(DiagnosticLog.error(DiagnosticCode.INVALID_OPTION_VALUE, excludeTestsOption,
                        EXCLUDE_TESTS_OPTION, "true or false"));
            }
            excludeTests = Boolean.parseBoolean(excludeTestsOption);
        }
        try {
            reportFormat = JSON_FORMAT.equalsIgnoreCase(format) ? ReportFormat.BALLERINA
                    : ReportFormat.fromString(format);
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
     * Scans a package or a single file, and fails the scan on an error or a tripped gate.
     *
     * @param project the package or single file project
     */
    private void executeProject(Project project) {
        PackageScan packageScan = preparePackage(project);
        if (listRules) {
            if (project.kind() == ProjectKind.BUILD_PROJECT) {
                section(Section.RESOLVING);
                errorStream.println(RESOLVING_PACKAGE_SCAN_RULES_LOG);
            }
            PackageRules rules = loadRules(packageScan);
            rules.analyzer().runExternalAnalyzers(rules.externalAnalyzers());
            failOnCompilationErrors(rules.analyzer());
            section(Section.RULES);
            ScanUtils.printRulesToConsole(allRules, outputStream);
            return;
        }
        PackageRules rules = loadRules(packageScan);
        failOnUnknownRuleOptions();
        checkConfigRules(packageScan, rules);

        printRunningScans();
        PackageResult result = scanPackage(packageScan, rules);
        // Nothing is reported until the package meets the suppression requirements
        if (result.suppressionFailure() != null) {
            throw result.suppressionFailure();
        }
        Optional<Path> resultsPath = report(result);
        printSummary(result.activeIssues().size(), result.suppressions().size());
        resultsPath.ifPresent(this::printResultsLocation);
        if (resultsPath.isPresent() && scanReport) {
            printScanReportLocation(ScanUtils.generateScanReport(result.issues(), project, targetDir,
                    result.suppressions(), packageScan.config().analyzedSources()));
        }
        // The results are saved before the platforms run, and a failed platform wins over the gate
        reportToPlatforms(result.platforms().stream()
                .map(platform -> new PlatformRun(platform, result.activeIssues())).toList());
        result.gate().evaluate(result.activeIssues()).ifPresent(message -> {
            throw ScanFailure.runtime(message);
        });
    }

    /**
     * Prepares a package for scanning: resolves its configuration and selects its platforms.
     *
     * @param project the package or single file project
     * @return the prepared package
     */
    private PackageScan preparePackage(Project project) {
        section(Section.LOADING);
        ScanTomlFile scanTomlFile = ScanUtils.readScanTomlConfigurations(project, errorStream, listRules);
        PackageConfig config = resolveConfig(project, scanTomlFile);

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
        ProjectAnalyzer projectAnalyzer = getProjectAnalyzer(packageScan.project(), packageScan.scanTomlFile(),
                packageScan.config().analyzedSources());
        List<Rule> coreRules = CoreRule.rules();
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();
        failOnUnresolvedAnalyzers(packageScan.project(), packageScan.scanTomlFile(),
                projectAnalyzer.getUnresolvedAnalyzers());
        addRules(coreRules);
        externalAnalyzers.values().forEach(this::addRules);
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
        String scanTomlLabel = scanTomlFile.getDisplayPath(project.sourceRoot()).orElse(Constants.SCAN_FILE);

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

    // An option replaces one setting, and warns only when the file writes that setting differently
    private void warnOverride(ScanTomlFile scanTomlFile, String setting, String option, String value,
                              String scanTomlLabel) {
        scanTomlFile.getWrittenValue(setting)
                .filter(writtenValue -> !writtenValue.equals(value))
                .ifPresent(writtenValue -> warn(DiagnosticLog.warning(DiagnosticCode.SETTING_OVERRIDDEN, option,
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
     * Selects the rules of a package. The rules of each option are added to its own setting of the file, so an option
     * and the other setting of the file can conflict.
     */
    private RuleSelection resolveRules(ScanTomlFile scanTomlFile, String scanTomlLabel) {
        List<String> tomlIncludeRules = scanTomlFile.getRulesToInclude().stream()
                .map(ScanTomlFile.RuleToFilter::id).toList();
        List<String> tomlExcludeRules = scanTomlFile.getRulesToExclude().stream()
                .map(ScanTomlFile.RuleToFilter::id).toList();
        List<String> include = Stream.concat(tomlIncludeRules.stream(), includeRules.stream()).distinct().toList();
        List<String> exclude = Stream.concat(tomlExcludeRules.stream(), excludeRules.stream()).distinct().toList();
        // Both from the command line is caught earlier, and both from the file is a configuration error
        if (!listRules && !include.isEmpty() && !exclude.isEmpty()) {
            String conflict = includeRules.isEmpty()
                    ? DiagnosticLog.error(DiagnosticCode.RULE_OPTION_CONFLICT,
                    "--exclude-rules=" + String.join(",", excludeRules),
                    ScanTomlFile.RULE_INCLUDE_SETTING + " = " + ScanTomlFile.quotedList(tomlIncludeRules),
                    scanTomlLabel)
                    : DiagnosticLog.error(DiagnosticCode.RULE_OPTION_CONFLICT,
                    "--include-rules=" + String.join(",", includeRules),
                    ScanTomlFile.RULE_EXCLUDE_SETTING + " = " + ScanTomlFile.quotedList(tomlExcludeRules),
                    scanTomlLabel);
            throw ScanFailure.usage(conflict);
        }
        return new RuleSelection(new ArrayList<>(include), new ArrayList<>(exclude));
    }

    /**
     * Finds the plugins of the platforms named in {@code --platforms} that the package's Scan.toml declares. A
     * platform is never reported to unless it is named on the command line, or the scan is triggered by a platform.
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
        List<String> withoutPath = requested.stream().filter(name -> !declared.containsKey(name))
                .filter(scanTomlFile.getPlatformsWithoutPath()::contains)
                .map(name -> "'" + name + "'").toList();
        if (!withoutPath.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_PATH_MISSING, withoutPath.size(),
                    joinWithAnd(withoutPath)));
        }
        List<String> undeclared = requested.stream().filter(name -> !declared.containsKey(name))
                .map(name -> "'" + name + "'").toList();
        if (!undeclared.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_NOT_AVAILABLE,
                    undeclared.size(), joinWithAnd(undeclared)));
        }
        List<ScanTomlFile.Platform> selected = requested.stream().map(declared::get).toList();

        // Each JAR is loaded on its own, so that a broken one is blamed on the platform that names it
        Map<Path, Map<String, StaticCodeAnalysisPlatformPlugin>> pluginsByJar = new HashMap<>();
        List<SelectedPlatform> selectedPlatforms = new ArrayList<>();
        for (ScanTomlFile.Platform platform : selected) {
            Path jarPath = ScanUtils.resolvePlatformJar(platform, project.targetDir(), errorStream);
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
     * Reports the issues to every selected platform, one after another. Each failure is collected, so that one
     * failing platform does not hide another.
     */
    private void reportToPlatforms(List<PlatformRun> runs) {
        if (runs.isEmpty()) {
            return;
        }
        section(Section.PLATFORMS);
        runs.forEach(run -> errorStream.println("Reporting issues to: " + run.platform().name()));
        section(Section.PLATFORM_OUTPUT);
        // The plugins print to the system streams, which execute already routes to standard error when it is separate.
        // They are routed here too for a command with one stream, so that the layout still counts their lines.
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;
        List<Optional<String>> outcomes;
        try {
            System.setOut(errorStream);
            System.setErr(errorStream);
            outcomes = PlatformRunner.runAll(runs.stream()
                    .map(run -> new PlatformRunner.Task(run.platform().plugin(), run.platform().context(),
                            run.issues()))
                    .toList());
        } finally {
            outputStream.flush();
            errorStream.flush();
            System.setOut(systemOut);
            System.setErr(systemErr);
        }
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < runs.size(); i++) {
            PlatformRun run = runs.get(i);
            outcomes.get(i).ifPresent(reason -> errors.add(run.error(reason)));
        }
        if (!errors.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORMS_FAILED, errors.size()), errors);
        }
    }

    /**
     * Fails the scan when a rule named on the command line is not a rule of the scanned package.
     */
    private void failOnUnknownRuleOptions() {
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

    /**
     * Checks the rules of the [rule] table of the Scan.toml file against the rules of the package. A mistyped rule
     * fails the scan, and a rule of an analyzer the package does not use is a warning, since a shared Scan.toml can
     * name rules of analyzers that only some packages use.
     */
    private void checkConfigRules(PackageScan packageScan, PackageRules rules) {
        Project project = packageScan.project();
        ScanTomlFile scanTomlFile = packageScan.scanTomlFile();
        Set<String> packageRuleIds = new HashSet<>();
        ProjectAnalyzer.rules(rules.coreRules(), rules.externalAnalyzers())
                .forEach(rule -> packageRuleIds.add(rule.id()));
        String displayPath = scanTomlFile.getDisplayPath(project.sourceRoot()).orElse(Constants.SCAN_FILE);
        Set<String> mistypedRules = mistypedRules(scanTomlFile, packageRuleIds);
        List<String> unavailableRules = configRuleIds(scanTomlFile)
                .filter(ruleId -> !packageRuleIds.contains(ruleId) && !mistypedRules.contains(ruleId))
                .map(ruleId -> "'" + ruleId + "'")
                .toList();
        if (!unavailableRules.isEmpty()) {
            warn(DiagnosticLog.warning(DiagnosticCode.WORKSPACE_RULE_NOT_AVAILABLE, joinWithAnd(unavailableRules),
                    displayPath, unavailableRules.size(), PACKAGE_SCOPE));
        }
        if (!mistypedRules.isEmpty()) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.CONFIG_FILE_HAS_ERRORS, displayPath,
                    mistypedRules.size()), ruleErrors(scanTomlFile, displayPath, mistypedRules));
        }
    }

    /**
     * Returns the rules of the [rule] table that are mistyped: a rule ID that is not well formed, or one that is not
     * among the known rules although its provider, the core rules or an analyzer, is known. A rule of an analyzer
     * that provides no known rules cannot be checked, as it may be used only by other packages.
     */
    private static Set<String> mistypedRules(ScanTomlFile scanTomlFile, Set<String> knownRuleIds) {
        Set<String> providers = knownRuleIds.stream()
                .map(ruleId -> ruleId.substring(0, ruleId.lastIndexOf(':')))
                .collect(Collectors.toSet());
        return configRuleIds(scanTomlFile)
                .filter(ruleId -> !knownRuleIds.contains(ruleId))
                .filter(ruleId -> {
                    Matcher matcher = RULE_ID_PATTERN.matcher(ruleId);
                    return !matcher.matches() || providers.contains(matcher.group(1));
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> ruleErrors(ScanTomlFile scanTomlFile, String displayPath, Set<String> ruleIds) {
        List<String> errors = new ArrayList<>();
        for (String ruleId : ruleIds) {
            errors.add(DiagnosticLog.fileError(displayPath, scanTomlFile.getRuleLocation(ruleId).orElseThrow(),
                    DiagnosticLog.error(DiagnosticCode.UNKNOWN_CONFIG_RULE, ruleId, PACKAGE_SCOPE)));
        }
        errors.add(LIST_RULES_HINT);
        return errors;
    }

    /**
     * Fails the scan with each [[analyzer]] of the Scan.toml file that could not be resolved, as a Scan.toml error at
     * its position, since leaving it out would give a clean-looking scan without its rules.
     */
    private static void failOnUnresolvedAnalyzers(Project project, ScanTomlFile scanTomlFile,
                                                  Map<ScanTomlFile.Analyzer, String> unresolvedAnalyzers) {
        if (unresolvedAnalyzers.isEmpty()) {
            return;
        }
        String displayPath = scanTomlFile.getDisplayPath(project.sourceRoot()).orElse(Constants.SCAN_FILE);
        List<String> errors = new ArrayList<>();
        unresolvedAnalyzers.forEach((analyzer, reason) -> {
            String id = analyzer.org() + "/" + analyzer.name()
                    + (analyzer.version() == null ? "" : ":" + analyzer.version())
                    + (analyzer.repository() == null ? "" : " from the " + analyzer.repository() + " repository");
            errors.add(DiagnosticLog.fileError(displayPath, scanTomlFile.getAnalyzerLocation(analyzer).orElseThrow(),
                    DiagnosticLog.error(DiagnosticCode.ANALYZER_NOT_RESOLVED, id,
                            reason.isEmpty() ? "" : ": " + reason)));
        });
        throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.CONFIG_FILE_HAS_ERRORS, displayPath,
                errors.size()), errors);
    }

    private static Stream<String> configRuleIds(ScanTomlFile scanTomlFile) {
        return Stream.concat(scanTomlFile.getRulesToInclude().stream(), scanTomlFile.getRulesToExclude().stream())
                .map(ScanTomlFile.RuleToFilter::id)
                .distinct();
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
            warn(DiagnosticLog.warning(DiagnosticCode.GATE_COUNT_IGNORED, FAIL_ON_COUNT_OPTION + "=" + gateCount,
                    turnedOffBy));
        }
        return QualityGate.of(severity, count);
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
     * Prints the active issues of a package and saves its results, unless a platform triggered the scan.
     *
     * @return the results file of the package, or empty when none was saved
     */
    private Optional<Path> report(PackageResult result) {
        // A platform-triggered scan neither prints nor saves; otherwise the results are printed and saved even when
        // they are also sent to platforms
        if (platformTriggered) {
            return Optional.empty();
        }
        Project project = result.project();
        Map<Issue, SuppressedIssue> suppressions = result.suppressions();
        printIssues(result.activeIssues(), project);
        if (!project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            return Optional.empty();
        }
        return Optional.of(ReportFormat.SARIF.equals(reportFormat)
                ? ScanUtils.saveSarifToDirectory(result.issues(), project, targetDir, suppressions)
                : ScanUtils.saveToDirectory(result.issues(), project, targetDir, suppressions));
    }

    /**
     * Prints issues to standard output as one JSON or SARIF document.
     */
    private void printIssues(List<Issue> issues, Project project) {
        section(Section.RESULTS);
        outputStream.println(ReportFormat.SARIF.equals(reportFormat)
                ? convertIssuesToSarifString(issues, project) : convertIssuesToJsonString(issues, project, Map.of()));
        outputStream.flush();
    }

    private void printResultsLocation(Path resultsPath) {
        section(Section.RESULTS_FILE);
        errorStream.println("View scan results at:");
        errorStream.println("\t" + resultsPath.toUri());
    }

    private void printScanReportLocation(Path scanReportPath) {
        section(Section.SCAN_REPORT);
        errorStream.println("View scan report at:");
        errorStream.println("\t" + scanReportPath.toUri());
    }

    private void printRunningScans() {
        section(Section.RUNNING);
        errorStream.println(RUNNING_SCANS_LOG);
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
                .forEach(pendingWarnings::add);
    }

    private void printSummary(int activeIssueCount, int suppressedIssueCount) {
        section(Section.SUMMARY);
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
     * @throws RuntimeException if the path is not a valid Ballerina project, which exits as an invalid target
     */
    protected Project getProject() {
        return loadTarget();
    }

    private Project loadTarget() {
        if (Files.isDirectory(projectPath)) {
            if (!Files.isRegularFile(projectPath.resolve(ProjectConstants.BALLERINA_TOML))) {
                throw ScanFailure.invalidTarget(DiagnosticLog.error(DiagnosticCode.INVALID_PROJECT_PATH,
                        projectPath), List.of());
            }
            // A relative path such as '.' has no parent to find the package root from
            Path packageRoot = projectPath.toAbsolutePath().normalize();
            return loadProject(() -> BuildProject.load(packageRoot));
        }
        if (!Files.isRegularFile(projectPath) || !projectPath.toString().endsWith(ProjectConstants.BLANG_SOURCE_EXT)) {
            throw ScanFailure.invalidTarget(DiagnosticLog.error(DiagnosticCode.INVALID_PROJECT_PATH, projectPath),
                    List.of());
        }
        Optional<Path> packageRoot = getEnclosingPackageRoot();
        if (packageRoot.isPresent()) {
            throw ScanFailure.invalidTarget(DiagnosticLog.error(DiagnosticCode.FILE_IN_PACKAGE, projectPath,
                    packageRoot.get()), List.of());
        }
        Path balFile = projectPath.toAbsolutePath().normalize();
        return loadProject(() -> SingleFileProject.load(balFile));
    }

    /**
     * Loads the package or file to scan, failing as a runtime error when it cannot be loaded.
     */
    private Project loadProject(Supplier<Project> loader) {
        try {
            return loader.get();
        } catch (RuntimeException ex) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PROJECT_LOAD_FAILED, projectPath),
                    List.of("error: " + ScanFailure.describe(ex)));
        }
    }

    private Optional<Path> getEnclosingPackageRoot() {
        try {
            Path absolutePath = projectPath.toAbsolutePath().normalize();
            if (ProjectPaths.isStandaloneBalFile(absolutePath)) {
                return Optional.empty();
            }
            return Optional.of(ProjectPaths.packageRoot(absolutePath));
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
     * @since 0.11.1
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
            ServiceLoader.load(StaticCodeAnalysisPlatformPlugin.class, classLoader)
                    .forEach(plugin -> plugins.putIfAbsent(plugin.platform(), plugin));
        } catch (MalformedURLException | ServiceConfigurationError | LinkageError | RuntimeException ex) {
            throw ScanFailure.runtime(DiagnosticLog.error(DiagnosticCode.PLATFORM_FAILED, platformName,
                    ScanFailure.describe(ex)));
        }
        return plugins;
    }

    /**
     * Returns whether {@code --exclude-tests} was written without an attached value, in which case picocli takes the
     * next argument as its value even when it is the target, so that {@code --exclude-tests main.bal} must still scan
     * {@code main.bal}.
     */
    private boolean isExcludeTestsWrittenAlone() {
        return originalArgs().contains(EXCLUDE_TESTS_OPTION);
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
     * @param platform the platform
     * @param issues   the active issues of the package
     */
    private record PlatformRun(SelectedPlatform platform, List<Issue> issues) {
        String error(String reason) {
            return "error: " + DiagnosticLog.error(DiagnosticCode.PLATFORM_FAILED, platform.name(), reason);
        }
    }

    /**
     * Returns where the value that {@code --exclude-tests} took belongs among the targets, so that they keep the order
     * they were written in.
     */
    private int excludeTestsValuePosition() {
        List<String> args = originalArgs();
        int optionIndex = args.indexOf(EXCLUDE_TESTS_OPTION);
        int position = 0;
        for (int i = 0; i < optionIndex && position < targets.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("-")) {
                // An option value written as a separate argument is not a target
                if (!arg.contains("=") && takesValue(arg)) {
                    i++;
                }
            } else if (arg.equals(targets.get(position))) {
                position++;
            }
        }
        return position;
    }

    private boolean takesValue(String option) {
        CommandLine.Model.OptionSpec optionSpec = spec.findOption(option);
        return optionSpec != null && optionSpec.arity().max > 0;
    }

    private List<String> originalArgs() {
        CommandLine.ParseResult parseResult = spec == null ? null : spec.commandLine().getParseResult();
        return parseResult == null ? List.of() : parseResult.originalArgs();
    }

    private boolean isStandaloneBalFile() {
        try {
            return ProjectPaths.isStandaloneBalFile(projectPath);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static class StringToListConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(",", -1)).map(String::trim).toList();
        }
    }
}
