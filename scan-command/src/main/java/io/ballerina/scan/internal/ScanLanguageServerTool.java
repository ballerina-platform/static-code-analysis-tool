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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DependencyGraph;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.ProjectLoadResult;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.util.ProjectPaths;
import io.ballerina.scan.ExcludedIssue;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScanResult;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.utils.Constants;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanTomlWriter;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.text.LineRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code ScanTool} LANGUAGE SERVER ENTRY POINT
 * The core tool that executes and manages the security analysis.
 *
 * @since 0.11.1
 */
public class ScanLanguageServerTool {
    private static final Gson GSON = new Gson();

    /**
     * Version of the {@code scan}, {@code excludeRuleEdit}, {@code includeRuleEdit} and {@code enableSuppressionEdit}
     * entry points. The language server checks it before calling them, so bump it on any incompatible change to their
     * signatures or JSON.
     */
    public static final int API_VERSION = 1;

    /**
     * Returns the version of the language server entry points this tool provides.
     *
     * @return the API version
     */
    public static int apiVersion() {
        return API_VERSION;
    }

    /**
     * Scans a package, or every package of a workspace, and returns the active issues of each package with the same
     * issue fields as the HTML report, plus the absolute path of the file the issue is in and the alternative edits
     * that suppress it with a {@code @scan:Suppress} annotation, innermost construct first. Suppressed issues and
     * diagnostics about the suppressions are returned separately. When Scan.toml disables suppressions, an issue that
     * an annotation would suppress carries that suppression as {@code inactiveSuppression}, and the title of the action
     * that enables suppressions as {@code enableSuppressions}, instead of a fix. With the
     * {@code report} option, it also writes each package's HTML report to its target directory and returns the report
     * path.
     *
     * @param projectPathStr  package or workspace root
     * @param buildOptionsMap build options, plus {@code report}
     * @return scan result as JSON
     */
    public static String scan(String projectPathStr, Map<String, Boolean> buildOptionsMap) {
        JsonObject response = new JsonObject();
        response.addProperty("apiVersion", API_VERSION);
        try {
            Path projectPath = Paths.get(projectPathStr).toAbsolutePath().normalize();
            boolean report = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("report"));
            JsonArray packages = new JsonArray();
            for (Project pkg : loadPackagesForLs(projectPath, buildOptionsMap)) {
                packages.add(scanPackage(pkg, report));
            }
            response.addProperty("success", true);
            response.add("packages", packages);
        } catch (Exception e) {
            response.addProperty("success", false);
            String errorMsg = e.getMessage();
            response.addProperty("error", errorMsg != null && !errorMsg.isBlank() ? errorMsg : e.toString());
        }
        return GSON.toJson(response);
    }

    /**
     * Computes the Scan.toml content that globally excludes a rule, without writing it, so the client can apply it
     * as an undoable edit.
     *
     * @param projectPathStr package root that holds the Scan.toml
     * @param ruleId         rule identifier
     * @return the edit as JSON
     */
    public static String excludeRuleEdit(String projectPathStr, String ruleId) {
        return ruleEdit(projectPathStr, ruleId, true);
    }

    /**
     * Computes the Scan.toml content that stops globally excluding a rule, without writing it.
     *
     * @param projectPathStr package root that holds the Scan.toml
     * @param ruleId         rule identifier
     * @return the edit as JSON
     */
    public static String includeRuleEdit(String projectPathStr, String ruleId) {
        return ruleEdit(projectPathStr, ruleId, false);
    }

    /**
     * Computes the Scan.toml content that applies {@code @scan:Suppress} annotations again, without writing it.
     *
     * @param projectPathStr package root that holds the Scan.toml
     * @return the edit as JSON
     */
    public static String enableSuppressionEdit(String projectPathStr) {
        return GSON.toJson(scanTomlEdit(projectPathStr, ScanTomlWriter::contentWithSuppressionEnabled));
    }

    /**
     * Runs a scan for the given project path.
     *
     * @param projectPathStr project path
     * @param buildOptionsMap build options
     * @return scan result as JSON
     */
    public static String runScan(String projectPathStr,
                                 Map<String, Boolean> buildOptionsMap) {
        try {
            Path projectPath = Paths.get(projectPathStr);
            Project project = loadProjectForLs(projectPath, buildOptionsMap);

            // Run the scanner
            LsScanResult lsResult = runLsScan(project);
            ScanResult result = lsResult.result();

            // Check dependent workspace packages for issues
            boolean dependentPackageIssuesFound;
            try {
                dependentPackageIssuesFound = hasDependentPackageIssues(project, projectPath, buildOptionsMap);
            } catch (Throwable e) {
                String errorMsg;
                if (e.getCause() != null) {
                    errorMsg = e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
                } else {
                    errorMsg = e.getClass().getName() + ": " + e.getMessage();
                }
                JsonObject errorObject = new JsonObject();
                errorObject.addProperty("success", false);
                errorObject.addProperty("error", "Dependent Package Scan Error: " + errorMsg);
                addEmptyIssueArrays(errorObject);
                return GSON.toJson(errorObject);
            }

            // Convert to JSON
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("success", true);
            jsonObject.add("activeIssues", issuesToJsonArray(result.activeIssues()));
            jsonObject.add("excludedIssues", excludedIssuesToJsonArray(result.excludedIssues()));
            jsonObject.add("suppressedIssues", suppressedIssuesToJsonArray(result.suppressedIssues()));
            jsonObject.add("suppressionDiagnostics", suppressionDiagnosticsToJsonArray(lsResult.diagnostics()));
            jsonObject.addProperty("dependentPackageIssuesFound", dependentPackageIssuesFound);
            return GSON.toJson(jsonObject);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("success", false);
            String errorMsg = e.getMessage();
            errorObject.addProperty("error", errorMsg != null && !errorMsg.trim().isEmpty() ? errorMsg : e.toString());
            addEmptyIssueArrays(errorObject);
            return GSON.toJson(errorObject);
        }
    }


    /**
     * Adds a rule ID to the global {@code [rule]} section in {@link Constants#SCAN_FILE}.
     *
     * @param projectPathStr project path
     * @param ruleId rule identifier
     * @return operation result as JSON
     */
    public static String addGlobalExclusion(String projectPathStr, String ruleId) {
        // Validate and normalize ruleId
        String normalizedRuleId = ruleId == null ? "" : ruleId.trim();
        if (normalizedRuleId.isEmpty()) {
            return GSON.toJson(new ScanMutationResponse(false, null,
                    "Invalid ruleId: must be a non-empty string"));
        }

        try {
            Path projectPath = Paths.get(projectPathStr);
            Path scanTomlPath = projectPath.resolve(Constants.SCAN_FILE);
            ScanTomlWriter.addGlobalExclusion(scanTomlPath, normalizedRuleId);

            return GSON.toJson(new ScanMutationResponse(true, normalizedRuleId,
                    "Global exclusion added successfully for rule '" + normalizedRuleId + "'"));
        } catch (IOException e) {
            return GSON.toJson(new ScanMutationResponse(false, null,
                    "Failed to write global exclusion to " + Constants.SCAN_FILE + ": " + e.getMessage()));
        } catch (Exception e) {
            return GSON.toJson(new ScanMutationResponse(false, null,
                    "Failed to add global exclusion: " + e.getMessage()));
        }
    }


    /**
     * Removes a rule ID from the global {@code [rule]} section in {@link Constants#SCAN_FILE}.
     *
     * @param projectPathStr project path
     * @param ruleId rule identifier
     * @return operation result as JSON
     */
    public static String removeGlobalExclusion(String projectPathStr, String ruleId) {
        // Validate and normalize ruleId
        String normalizedRuleId = ruleId == null ? "" : ruleId.trim();
        if (normalizedRuleId.isEmpty()) {
            return GSON.toJson(new ScanMutationResponse(false, null,
                    "Invalid ruleId: must be a non-empty string"));
        }

        try {
            Path scanTomlPath = Paths.get(projectPathStr).resolve(Constants.SCAN_FILE);
            ScanTomlWriter.removeGlobalExclusion(scanTomlPath, normalizedRuleId);

            return GSON.toJson(new ScanMutationResponse(true, normalizedRuleId,
                    "Global exclusion removed successfully."));
        } catch (IOException e) {
            return GSON.toJson(new ScanMutationResponse(false, null,
                    "Failed to remove global exclusion: " + e.getMessage()));
        } catch (Exception e) {
            return GSON.toJson(new ScanMutationResponse(false, null,
                    "Failed to remove global exclusion due to unexpected error: " + e.getMessage()));
        }
    }

    private static String ruleEdit(String projectPathStr, String ruleId, boolean exclude) {
        String normalizedRuleId = ruleId == null ? "" : ruleId.trim();
        if (normalizedRuleId.isEmpty()) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", "Invalid ruleId: must be a non-empty string");
            return GSON.toJson(response);
        }
        JsonObject response = scanTomlEdit(projectPathStr, scanTomlPath -> exclude
                ? ScanTomlWriter.contentWithGlobalExclusion(scanTomlPath, normalizedRuleId)
                : ScanTomlWriter.contentWithoutGlobalExclusion(scanTomlPath, normalizedRuleId));
        if (response.get("success").getAsBoolean()) {
            response.addProperty("ruleId", normalizedRuleId);
        }
        return GSON.toJson(response);
    }

    private interface ScanTomlContent {
        Optional<String> compute(Path scanTomlPath) throws IOException;
    }

    private static JsonObject scanTomlEdit(String projectPathStr, ScanTomlContent contentFunction) {
        JsonObject response = new JsonObject();
        try {
            Path scanTomlPath = Paths.get(projectPathStr).toAbsolutePath().normalize().resolve(Constants.SCAN_FILE);
            Optional<String> content = contentFunction.compute(scanTomlPath);
            response.addProperty("success", true);
            response.addProperty("path", scanTomlPath.toString());
            response.addProperty("exists", Files.exists(scanTomlPath));
            response.addProperty("changed", content.isPresent());
            content.ifPresent(newContent -> response.addProperty("content", newContent));
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("error", "Failed to compute the " + Constants.SCAN_FILE + " edit: "
                    + e.getMessage());
        }
        return response;
    }

    private static List<Project> loadPackagesForLs(Path projectPath, Map<String, Boolean> buildOptionsMap) {
        Project project = loadProjectForLs(projectPath, buildOptionsMap);
        if (project.kind() == ProjectKind.WORKSPACE_PROJECT) {
            return new ArrayList<>(((WorkspaceProject) project).projects());
        }
        return List.of(project);
    }

    private static JsonObject scanPackage(Project project, boolean report) throws IOException {
        JsonObject pkg = new JsonObject();
        pkg.addProperty("path", project.sourceRoot().toAbsolutePath().normalize().toString());
        pkg.addProperty("name", project.currentPackage().packageName().value());
        JsonArray issues = new JsonArray();
        pkg.add("issues", issues);

        Optional<ScanTomlFile> scanToml = ScanUtils.loadScanTomlConfigurations(project, System.err);
        if (scanToml.isEmpty()) {
            throw new IOException("Failed to load " + Constants.SCAN_FILE + " of package '"
                    + project.currentPackage().packageName().value() + "': invalid configuration.");
        }

        // Same order as the CLI: external analyzers add an import document, so compilation errors are checked after.
        ProjectAnalyzer analyzer = new ProjectAnalyzer(project, scanToml.get());
        Map<String, List<Rule>> externalAnalyzers = analyzer.getExternalAnalyzers();
        List<Issue> allIssues = new ArrayList<>(analyzer.runExternalAnalyzers(externalAnalyzers));
        if (!analyzer.getCompilationErrors().isEmpty()) {
            pkg.addProperty("skippedReason", "The package has compilation errors. Fix them and scan again.");
            return pkg;
        }
        List<Rule> coreRules = CoreRule.rules();
        allIssues.addAll(analyzer.analyze(coreRules));

        ScanResult filtered = filterIssues(allIssues, scanToml.get());
        LsScanResult result = applySuppressions(project, filtered, coreRules, externalAnalyzers, scanToml.get());
        SuppressionFixes suppressionFixes = new SuppressionFixes(project);
        for (Issue issue : result.result().activeIssues()) {
            SuppressedIssue inactive = result.inactiveSuppressions().get(issue);
            JsonObject issueJson = toLsIssueJson(issue, inactive);
            if (inactive != null) {
                issueJson.add("inactiveSuppression", issueJson.remove("suppression"));
                JsonObject enableSuppressions = new JsonObject();
                enableSuppressions.addProperty("title", "Enable suppressions in " + Constants.SCAN_FILE);
                enableSuppressions.addProperty("note", "It has a @scan:Suppress annotation, but suppressions are "
                        + "disabled in " + Constants.SCAN_FILE + ".");
                issueJson.add("enableSuppressions", enableSuppressions);
            } else {
                List<SuppressionFixes.Fix> fixes = suppressionFixes.find(issue, ((IssueImpl) issue).filePath());
                if (!fixes.isEmpty()) {
                    JsonArray fixesJson = new JsonArray();
                    fixes.forEach(fix -> fixesJson.add(toSuppressFixJson(fix)));
                    issueJson.add("suppressFixes", fixesJson);
                }
            }
            issues.add(issueJson);
        }
        JsonArray suppressedIssues = new JsonArray();
        Map<Issue, SuppressedIssue> suppressions = new IdentityHashMap<>();
        for (SuppressedIssue suppressed : result.result().suppressedIssues()) {
            suppressedIssues.add(toLsIssueJson(suppressed.issue(), suppressed));
            suppressions.put(suppressed.issue(), suppressed);
        }
        pkg.add("suppressedIssues", suppressedIssues);
        JsonArray suppressionDiagnostics = new JsonArray();
        for (SuppressionDiagnostic diagnostic : result.diagnostics()) {
            JsonObject diagnosticJson = new JsonObject();
            diagnosticJson.addProperty("severity", diagnostic.severity().toString());
            diagnosticJson.addProperty("message", diagnostic.message());
            diagnosticJson.addProperty("filePath", diagnostic.filePath());
            diagnosticJson.add("textRange", toTextRangeJson(diagnostic.location()));
            suppressionDiagnostics.add(diagnosticJson);
        }
        pkg.add("suppressionDiagnostics", suppressionDiagnostics);
        pkg.addProperty("suppressionEnabled", scanToml.get().isSuppressionEnabled());
        if (report) {
            pkg.addProperty("reportPath", ScanUtils.generateScanReport(filtered.activeIssues(), project, null,
                    suppressions).toString());
        }
        return pkg;
    }

    private static JsonObject toLsIssueJson(Issue issue, SuppressedIssue suppressed) {
        IssueImpl issueImpl = (IssueImpl) issue;
        JsonObject issueJson = ScanUtils.getJsonIssue(issueImpl, suppressed);
        issueJson.addProperty("filePath", issueImpl.filePath());
        return issueJson;
    }

    private static JsonObject toSuppressFixJson(SuppressionFixes.Fix fix) {
        JsonObject fixJson = new JsonObject();
        fixJson.addProperty("construct", fix.construct());
        fixJson.addProperty("title", fix.title());
        JsonArray edits = new JsonArray();
        for (SuppressionFixes.Edit edit : fix.edits()) {
            JsonObject editJson = new JsonObject();
            editJson.add("textRange", toTextRangeJson(edit.range()));
            editJson.addProperty("newText", edit.newText());
            editJson.addProperty("snippet", edit.snippet());
            edits.add(editJson);
        }
        fixJson.add("edits", edits);
        return fixJson;
    }

    private static JsonObject toTextRangeJson(LineRange range) {
        JsonObject textRange = new JsonObject();
        textRange.addProperty("startLine", range.startLine().line());
        textRange.addProperty("startLineOffset", range.startLine().offset());
        textRange.addProperty("endLine", range.endLine().line());
        textRange.addProperty("endLineOffset", range.endLine().offset());
        return textRange;
    }

    // ===================================================================================
    // CORE SCANNING LOGIC
    // ===================================================================================

    private static Project loadProjectForLs(Path projectPath, Map<String, Boolean> buildOptionsMap) {
        boolean isOffline = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("offline"));
        boolean isSkipTests = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("skipTests"));

        BuildOptions buildOptions = BuildOptions.builder()
                .setOffline(isOffline)
                .setSkipTests(isSkipTests)
                .build();

        ProjectLoadResult loadResult = ProjectLoader.load(projectPath, buildOptions);
        Project project = loadResult.project();

        if (project.kind() == ProjectKind.WORKSPACE_PROJECT) {
            WorkspaceProject workspaceProject = (WorkspaceProject) project;
            for (BuildProject bp : workspaceProject.projects()) {
                if (bp.sourceRoot().equals(projectPath)) {
                    return bp;
                }
            }
        }

        // If the loaded project is a BuildProject without workspace context, try to find the
        // workspace root and load it to get proper workspace resolution
        Optional<Path> wsRoot = ProjectPaths.workspaceRoot(projectPath);
        if (wsRoot.isPresent()) {
            ProjectLoadResult wsResult = ProjectLoader.load(wsRoot.get(), buildOptions);
            if (wsResult.project().kind() == ProjectKind.WORKSPACE_PROJECT) {
                WorkspaceProject workspaceProject = (WorkspaceProject) wsResult.project();
                for (BuildProject bp : workspaceProject.projects()) {
                    if (bp.sourceRoot().equals(projectPath)) {
                        return bp;
                    }
                }
            }
        }
        return project;
    }

    private static boolean hasDependentPackageIssues(Project project, Path projectPath,
                                                     Map<String, Boolean> buildOptionsMap) throws IOException {
        // Try to get workspace from the project first
        Optional<WorkspaceProject> wsOpt = project.workspaceProject();

        // If not available, try to find workspace from the path
        if (wsOpt.isEmpty()) {
            Optional<Path> wsRoot = ProjectPaths.workspaceRoot(projectPath);
            if (wsRoot.isPresent()) {
                boolean isOffline = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("offline"));
                boolean isSkipTests = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("skipTests"));
                BuildOptions wsBuildOptions = BuildOptions.builder()
                        .setOffline(isOffline)
                        .setSkipTests(isSkipTests)
                        .build();
                ProjectLoadResult wsResult = ProjectLoader.load(wsRoot.get(), wsBuildOptions);
                if (wsResult.project().kind() == ProjectKind.WORKSPACE_PROJECT) {
                    WorkspaceProject wsProject = (WorkspaceProject) wsResult.project();
                    for (BuildProject bp : wsProject.projects()) {
                        if (bp.sourceRoot().equals(projectPath)) {
                            project = bp;
                            wsOpt = Optional.of(wsProject);
                            break;
                        }
                    }
                }
            }
        }

        if (wsOpt.isEmpty()) {
            return false;
        }
        WorkspaceProject workspaceProject = wsOpt.get();
        if (workspaceProject.projects().size() <= 1) {
            return false;
        }
        DependencyGraph<BuildProject> depGraph = workspaceProject.getResolution().dependencyGraph();
        Collection<BuildProject> dependencies = depGraph.getAllDependencies((BuildProject) project);
        for (BuildProject bp : dependencies) {
            ScanResult depResult = runScan(bp);
            if (!depResult.activeIssues().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static ScanResult runScan(Project project) throws IOException {
        return runLsScan(project).result();
    }

    private static LsScanResult runLsScan(Project project) throws IOException {
        // Load Constants.SCAN_FILE configurations
        Optional<ScanTomlFile> scanToml = ScanUtils.loadScanTomlConfigurations(project, System.err);

        // An empty result indicates an invalid Scan.toml configuration, not a missing file.
        if (scanToml.isEmpty()) {
            throw new IOException("Failed to load Scan.toml: invalid configuration. " +
                    "Check for malformed content, missing required 'scan.configPath', " +
                    "or failures while loading referenced configuration.");
        }

        ProjectAnalyzer analyzer = new ProjectAnalyzer(project, scanToml.get());

        // Execute
        return execute(project, analyzer, scanToml.get());
    }

    /**
    * Executes scanning with include/exclude filters resolved from Constants.SCAN_FILE.
     *
     * @throws IllegalArgumentException when both include and exclude rule filters are configured,
     *                                  since they are mutually exclusive.
     */
    private static LsScanResult execute(Project project, ProjectAnalyzer projectAnalyzer,
                                        ScanTomlFile scanToml) {
        // Gather all available Rules
        List<Rule> coreRules = CoreRule.rules();
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();

        // Run Analysis
        List<Issue> issues = projectAnalyzer.analyze(coreRules);

        if (!externalAnalyzers.isEmpty()) {
            issues.addAll(projectAnalyzer.runExternalAnalyzers(externalAnalyzers));
        }

        return applySuppressions(project, filterIssues(issues, scanToml), coreRules, externalAnalyzers, scanToml);
    }

    // The same suppression filter as the command line, applied after rule selection as it is there
    private static LsScanResult applySuppressions(Project project, ScanResult filtered, List<Rule> coreRules,
                                                  Map<String, List<Rule>> externalAnalyzers, ScanTomlFile scanToml) {
        List<Rule> projectRules = new ArrayList<>(coreRules);
        externalAnalyzers.values().forEach(projectRules::addAll);
        RuleSelection selection = new RuleSelection(
                scanToml.getRulesToInclude().stream().map(ScanTomlFile.RuleToFilter::id).toList(),
                scanToml.getRulesToExclude().stream().map(ScanTomlFile.RuleToFilter::id).toList());
        SuppressionOptions options = SuppressionOptions.of(scanToml, false, false, false);
        SuppressionOutcome suppressionOutcome = SuppressionFilter.apply(project, filtered.activeIssues(),
                projectRules, selection, options);
        // With suppressions disabled every issue stays active, but the editor must not offer to annotate an issue
        // that an annotation already covers, so the annotations are matched without applying them.
        Map<Issue, SuppressedIssue> inactiveSuppressions = options.ignoreSuppressions()
                ? SuppressionFilter.apply(project, filtered.activeIssues(), projectRules, selection,
                        new SuppressionOptions(false, false, false)).suppressionsByIssue()
                : Map.of();
        return new LsScanResult(new ScanResult(suppressionOutcome.activeIssues(), filtered.excludedIssues(),
                suppressionOutcome.suppressedIssues()), suppressionOutcome.diagnostics(), inactiveSuppressions);
    }

    private static ScanResult filterIssues(List<Issue> issues, ScanTomlFile scanToml) {
        // Prepare Filter Lists
        List<String> includeRules = new ArrayList<>();
        List<String> excludeRules = new ArrayList<>();

        if (scanToml != null) {
            scanToml.getRulesToInclude().stream()
                    .map(ScanTomlFile.RuleToFilter::id)
                    .forEach(includeRules::add);

            scanToml.getRulesToExclude().stream()
                    .map(ScanTomlFile.RuleToFilter::id)
                    .forEach(excludeRules::add);
        }

        if (!includeRules.isEmpty() && !excludeRules.isEmpty()) {
            throw new IllegalArgumentException("Invalid " + Constants.SCAN_FILE
                    + " configuration: both include and exclude rule "
                    + "filters are set. Configure only one of [rule.include] or [rule.exclude].");
        }

        List<Issue> activeIssues = new ArrayList<>();
        List<ExcludedIssue> excludedIssues = new ArrayList<>();

        for (Issue issue : issues) {
            boolean isExcluded;
            String issueFileName = issue.location() != null && issue.location().lineRange() != null
                    ? issue.location().lineRange().fileName() : "";
            String ruleId = issue.rule() != null ? issue.rule().id() : "";

            boolean isExcludedByGlobal = false;

            if (!includeRules.isEmpty() && !includeRules.contains(ruleId)) {
                isExcludedByGlobal = true;
            }

            if (!excludeRules.isEmpty() && excludeRules.contains(ruleId)) {
                isExcludedByGlobal = true;
            }

            isExcluded = isExcludedByGlobal;

            if (isExcluded) {
                excludedIssues.add(new ExcludedIssue(issue, ruleId, issueFileName, true));
            } else {
                activeIssues.add(issue);
            }
        }

        return new ScanResult(activeIssues, excludedIssues);
    }

    // ===================================================================================
    // HELPER METHODS & SERIALIZATION
    // ===================================================================================

    private static JsonArray issuesToJsonArray(List<Issue> issues) {
        JsonArray jsonArray = new JsonArray();
        for (Issue issue : issues) {
            jsonArray.add(issueToJsonObject(issue));
        }
        return jsonArray;
    }

    private static JsonArray excludedIssuesToJsonArray(List<ExcludedIssue> excludedIssues) {
        JsonArray jsonArray = new JsonArray();
        for (ExcludedIssue ex : excludedIssues) {
            JsonObject obj = new JsonObject();
            obj.addProperty("filePath", ex.filePath());
            obj.addProperty("ruleId", ex.ruleId());
            obj.addProperty("isGlobalExclusion", ex.isGlobalExclusion());
            obj.add("issueContext", issueToJsonObject(ex.issue()));
            jsonArray.add(obj);
        }
        return jsonArray;
    }

    private static void addEmptyIssueArrays(JsonObject errorObject) {
        errorObject.add("activeIssues", new JsonArray());
        errorObject.add("excludedIssues", new JsonArray());
        errorObject.add("suppressedIssues", new JsonArray());
        errorObject.add("suppressionDiagnostics", new JsonArray());
    }

    private static JsonArray suppressedIssuesToJsonArray(List<SuppressedIssue> suppressedIssues) {
        JsonArray jsonArray = new JsonArray();
        for (SuppressedIssue suppressed : suppressedIssues) {
            JsonObject obj = new JsonObject();
            obj.addProperty("ruleId", suppressed.issue().rule().id());
            suppressed.justification().ifPresent(reason -> obj.addProperty("justification", reason));
            JsonArray rules = new JsonArray();
            suppressed.rules().forEach(rules::add);
            obj.add("suppressionRules", rules);
            obj.add("issueContext", issueToJsonObject(suppressed.issue()));
            jsonArray.add(obj);
        }
        return jsonArray;
    }

    private static JsonArray suppressionDiagnosticsToJsonArray(List<SuppressionDiagnostic> diagnostics) {
        JsonArray jsonArray = new JsonArray();
        for (SuppressionDiagnostic diagnostic : diagnostics) {
            JsonObject obj = new JsonObject();
            obj.addProperty("severity", diagnostic.severity().toString());
            obj.addProperty("message", diagnostic.message());
            LineRange range = diagnostic.location();
            // The document name, as issues report it; fileName() carries the module prefix of a workspace
            obj.addProperty("filePath", range.fileName());
            obj.addProperty("startLine", range.startLine().line());
            obj.addProperty("startColumn", range.startLine().offset());
            obj.addProperty("endLine", range.endLine().line());
            obj.addProperty("endColumn", range.endLine().offset());
            jsonArray.add(obj);
        }
        return jsonArray;
    }

    private static JsonObject issueToJsonObject(Issue issue) {
        JsonObject obj = new JsonObject();
        if (issue.rule() != null) {
            obj.addProperty("ruleId", issue.rule().id());
            obj.addProperty("message", issue.rule().name());
            String ruleKind = issue.rule().kind() != null ? issue.rule().kind().name() : "UNKNOWN";
            obj.addProperty("severity", issue.rule().kind() != null ? issue.rule().kind().toString() : "MEDIUM");
            obj.addProperty("ruleKind", ruleKind);
        } else {
            obj.addProperty("ruleId", "UNKNOWN");
            obj.addProperty("message", "Unknown Issue Rule");
            obj.addProperty("severity", "MEDIUM");
            obj.addProperty("ruleKind", "UNKNOWN");
        }

        if (issue.location() != null) {
            LineRange range = issue.location().lineRange();
            if (range != null) {
                if (range.fileName() != null) {
                    obj.addProperty("filePath", range.fileName());
                }
                if (range.startLine() != null) {
                    obj.addProperty("startLine", range.startLine().line());
                    obj.addProperty("startColumn", range.startLine().offset());
                }
                if (range.endLine() != null) {
                    obj.addProperty("endLine", range.endLine().line());
                    obj.addProperty("endColumn", range.endLine().offset());
                }
            }
        }
        return obj;
    }

    private record ScanMutationResponse(boolean success, String ruleId, String message) { }

    private record LsScanResult(ScanResult result, List<SuppressionDiagnostic> diagnostics,
                                Map<Issue, SuppressedIssue> inactiveSuppressions) { }
}
