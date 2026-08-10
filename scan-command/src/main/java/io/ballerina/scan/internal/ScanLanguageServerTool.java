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
import io.ballerina.scan.utils.Constants;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanTomlWriter;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.text.LineRange;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
            ScanResult result = runScan(project);

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
                errorObject.add("activeIssues", new JsonArray());
                errorObject.add("excludedIssues", new JsonArray());
                return GSON.toJson(errorObject);
            }

            // Convert to JSON
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("success", true);
            jsonObject.add("activeIssues", issuesToJsonArray(result.activeIssues()));
            jsonObject.add("excludedIssues", excludedIssuesToJsonArray(result.excludedIssues()));
            jsonObject.addProperty("dependentPackageIssuesFound", dependentPackageIssuesFound);
            return GSON.toJson(jsonObject);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("success", false);
            String errorMsg = e.getMessage();
            errorObject.addProperty("error", errorMsg != null && !errorMsg.trim().isEmpty() ? errorMsg : e.toString());
            errorObject.add("activeIssues", new JsonArray());
            errorObject.add("excludedIssues", new JsonArray());
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

    // ===================================================================================
    // CORE SCANNING LOGIC
    // ===================================================================================

    private static Project loadProjectForLs(Path projectPath, Map<String, Boolean> buildOptionsMap) {
        boolean isOffline = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("offline"));
        boolean isSticky = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("sticky"));
        boolean isSkipTests = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("skipTests"));

        BuildOptions buildOptions = BuildOptions.builder()
                .setOffline(isOffline)
                .setSticky(isSticky)
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
                boolean isSticky = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("sticky"));
                boolean isSkipTests = buildOptionsMap != null && Boolean.TRUE.equals(buildOptionsMap.get("skipTests"));
                BuildOptions wsBuildOptions = BuildOptions.builder()
                        .setOffline(isOffline)
                        .setSticky(isSticky)
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
        return execute(analyzer, scanToml.get());
    }

    /**
    * Executes scanning with include/exclude filters resolved from Constants.SCAN_FILE.
     *
     * @throws IllegalArgumentException when both include and exclude rule filters are configured,
     *                                  since they are mutually exclusive.
     */
    private static ScanResult execute(ProjectAnalyzer projectAnalyzer,
                                      ScanTomlFile scanToml) {
        // Gather all available Rules
        List<Rule> coreRules = CoreRule.rules();
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers();

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

        // Run Analysis
        List<Issue> issues = projectAnalyzer.analyze(coreRules);

        if (!externalAnalyzers.isEmpty()) {
            issues.addAll(projectAnalyzer.runExternalAnalyzers(externalAnalyzers));
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

    private static JsonObject issueToJsonObject(Issue issue) {
        JsonObject obj = new JsonObject();
        if (issue.rule() != null) {
            obj.addProperty("ruleId", issue.rule().id());
            obj.addProperty("message", issue.rule().description());
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
}
