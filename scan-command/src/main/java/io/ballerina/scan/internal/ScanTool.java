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
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.scan.ExcludedIssue;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScanResult;
import io.ballerina.scan.utils.Constants;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanTomlWriter;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.scan.utils.SymbolResolver;
import io.ballerina.tools.text.LineRange;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@code ScanTool} LANGUAGE SERVER ENTRY POINT
 * The core tool that executes and manages the security analysis.
 *
 * @since 0.11.1
 */
public class ScanTool {
    private static final Gson GSON = new Gson();

    // ===================================================================================
    // PUBLIC LS ENTRY POINTS
    // ===================================================================================

    /**
     * ENTRY POINT TO RUN A SCAN
     * Returns a JSON string of issues.
     */
    public static String runScan(String projectPathStr,
                                 Map<String, Boolean> buildOptionsMap) {
        try {
            Path projectPath = Paths.get(projectPathStr);
            Project project = loadProjectForLs(projectPath, buildOptionsMap);

            // Run the scanner
            ScanResult result = runScan(project);

            // Convert to JSON
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("activeIssues", issuesToJsonArray(result.activeIssues()));
            jsonObject.add("excludedIssues", excludedIssuesToJsonArray(result.excludedIssues()));
            return GSON.toJson(jsonObject);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("success", false);
            errorObject.addProperty("error", e.getMessage());
            errorObject.add("activeIssues", new JsonArray());
            errorObject.add("excludedIssues", new JsonArray());
            return GSON.toJson(errorObject);
        }
    }

    /**
     * ENTRY POINT FOR ADDING EXCLUSIONS
    * Resolves the AST symbol at the given line and writes a symbol-based exclusion
    * entry to the Constants.SCAN_FILE file.
     */
    public static String addExclusion(String projectPathStr, String filePath, int lineNumber,
                                      String ruleId,
                                      Map<String, Boolean> buildOptionsMap) {
        JsonObject result = new JsonObject();
        try {
            Path projectPath = Paths.get(projectPathStr);
            Project project = loadProjectForLs(projectPath, buildOptionsMap);

            // Resolve the enclosing symbol and line hash
            String symbol = SymbolResolver.resolveSymbol(project, filePath, lineNumber);
            String lineHash = SymbolResolver.resolveLineHash(project, filePath, lineNumber);

            // Write the exclusion to Constants.SCAN_FILE
            Path scanTomlPath = projectPath.resolve(Constants.SCAN_FILE);
            String relativeFilePath = ScanUtils.getRelativePath(filePath, project.sourceRoot().toString());
            ScanTomlWriter.addExclusion(scanTomlPath, relativeFilePath, ruleId, symbol, lineHash);

            result.addProperty("success", true);
            result.addProperty("filePath", relativeFilePath);
            result.addProperty("ruleId", ruleId);
            result.addProperty("symbol", symbol);
            result.addProperty("lineHash", lineHash);
            result.addProperty("message", "Exclusion added successfully for symbol '" + symbol
                    + "' in file '" + filePath + "' for rule '" + ruleId + "'");
        } catch (IOException e) {
            result.addProperty("success", false);
                result.addProperty("error", "Failed to write exclusion to " + Constants.SCAN_FILE + ": "
                    + e.getMessage());
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("error", "Failed to add exclusion: " + e.getMessage());
        }
        return GSON.toJson(result);
    }

    /**
    * ENTRY POINT FOR ADDING GLOBAL EXCLUSIONS
    * Adds a rule ID to the global `[rule]` section in Constants.SCAN_FILE.
     */
    public static String addGlobalExclusion(String projectPathStr, String ruleId) {
        JsonObject result = new JsonObject();
        try {
            Path projectPath = Paths.get(projectPathStr);
            Path scanTomlPath = projectPath.resolve(Constants.SCAN_FILE);
            ScanTomlWriter.addGlobalExclusion(scanTomlPath, ruleId);

            result.addProperty("success", true);
            result.addProperty("ruleId", ruleId);
            result.addProperty("message", "Global exclusion added successfully for rule '" + ruleId + "'");
        } catch (IOException e) {
            result.addProperty("success", false);
                result.addProperty("error", "Failed to write global exclusion to " + Constants.SCAN_FILE + ": "
                    + e.getMessage());
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("error", "Failed to add global exclusion: " + e.getMessage());
        }
        return GSON.toJson(result);
    }

    /**
    * ENTRY POINT FOR REMOVING EXCLUSIONS
    * Removes a symbol-based exclusion entry from the Constants.SCAN_FILE file.
     */
    public static String removeExclusion(String projectPathStr, String filePath,
                                         String ruleId, String symbol, String lineHash) {
        JsonObject result = new JsonObject();
        try {
            Path projectPath = Paths.get(projectPathStr);
            Project project = BuildProject.load(projectPath, BuildOptions.builder().build());
            
            String relativeFilePath = ScanUtils.getRelativePath(filePath, project.sourceRoot().toString());
            Path scanTomlPath = projectPath.resolve(Constants.SCAN_FILE);
            ScanTomlWriter.removeExclusion(scanTomlPath, relativeFilePath, ruleId, symbol, lineHash);

            result.addProperty("success", true);
            result.addProperty("filePath", relativeFilePath);
            result.addProperty("ruleId", ruleId);
            result.addProperty("message", "Exclusion removed successfully.");
        } catch (IOException e) {
            result.addProperty("success", false);
            result.addProperty("error", "Failed to remove exclusion: " + e.getMessage());
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("error", "Failed to remove exclusion due to unexpected error: " + e.getMessage());
        }
        return GSON.toJson(result);
    }

    /**
    * ENTRY POINT FOR REMOVING GLOBAL EXCLUSIONS
    * Removes a rule ID from the global `[rule]` section in Constants.SCAN_FILE.
     */
    public static String removeGlobalExclusion(String projectPathStr, String ruleId) {
        JsonObject result = new JsonObject();
        try {
            Path scanTomlPath = Paths.get(projectPathStr).resolve(Constants.SCAN_FILE);
            ScanTomlWriter.removeGlobalExclusion(scanTomlPath, ruleId);

            result.addProperty("success", true);
            result.addProperty("ruleId", ruleId);
            result.addProperty("message", "Global exclusion removed successfully.");
        } catch (IOException e) {
            result.addProperty("success", false);
            result.addProperty("error", "Failed to remove global exclusion: " + e.getMessage());
        } catch (Exception e) {
            result.addProperty("success", false);
            result.addProperty("error", "Failed to remove global exclusion due to unexpected error: " + e.getMessage());
        }
        return GSON.toJson(result);
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

        return BuildProject.load(projectPath, buildOptions);
    }

    private static ScanResult runScan(Project project) {
        java.io.ByteArrayOutputStream diagnosticBuffer = new java.io.ByteArrayOutputStream();
        PrintStream silentStream = new PrintStream(
                diagnosticBuffer,
                true,
                StandardCharsets.UTF_8);

        // Load Constants.SCAN_FILE configurations
        Optional<ScanTomlFile> scanToml = ScanUtils.loadScanTomlConfigurations(project, silentStream);
        
        // If Scan.toml is malformed or missing, return a failing ScanResult
        if (scanToml.isEmpty()) {
            return new ScanResult(new ArrayList<>(), new ArrayList<>());
        }
        
        ProjectAnalyzer analyzer = new ProjectAnalyzer(project, scanToml.get());

        // Execute
        return execute(analyzer, scanToml.get(), project);
    }

    /**
    * Executes scanning with include/exclude filters resolved from Constants.SCAN_FILE.
     *
     * @throws IllegalArgumentException when both include and exclude rule filters are configured,
     *                                  since they are mutually exclusive.
     */
    private static ScanResult execute(ProjectAnalyzer projectAnalyzer,
                                      ScanTomlFile scanToml, Project project) {
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
            boolean isGlobalExclusion;
            String issueFileName = issue.location() != null && issue.location().lineRange() != null
                    ? issue.location().lineRange().fileName() : "";
            String ruleId = issue.rule() != null ? issue.rule().id() : "";

            String issueSymbol = "";
            String issueLineHash = "";
            boolean isExcludedByLocal = false;
            boolean isExcludedByGlobal = false;

            if (!includeRules.isEmpty() && !includeRules.contains(ruleId)) {
                isExcludedByGlobal = true;
            }

            if (!excludeRules.isEmpty() && excludeRules.contains(ruleId)) {
                isExcludedByGlobal = true;
            }

            // Resolve context data for local exclusions whenever possible.
            Set<ScanTomlFile.Exclusion> exclusions = scanToml.getExclusions();
            if (!exclusions.isEmpty()) {
                if (issue.location() != null && issue.location().lineRange() != null
                        && issue.location().lineRange().fileName() != null
                        && issue.location().lineRange().startLine() != null) {
                    int issueLine = issue.location().lineRange().startLine().line();
                    issueSymbol = SymbolResolver.resolveSymbol(project, issueFileName, issueLine);
                    issueLineHash = SymbolResolver.resolveLineHash(project, issueFileName, issueLine);

                    String finalExSymbol = issueSymbol;
                    String finalExLineHash = issueLineHash;

                    isExcludedByLocal = exclusions.stream().anyMatch(exclusion ->
                            ScanUtils.matchesExclusion(issueFileName, ruleId,
                                    finalExSymbol, finalExLineHash, exclusion));
                }
            }

            isExcluded = isExcludedByLocal || isExcludedByGlobal;
            isGlobalExclusion = isExcludedByGlobal && !isExcludedByLocal;

            if (isExcluded) {
                if (issueSymbol.isEmpty() && issue.location() != null
                        && issue.location().lineRange() != null
                        && issue.location().lineRange().startLine() != null) {
                    int issueLine = issue.location().lineRange().startLine().line();
                    issueSymbol = SymbolResolver.resolveSymbol(project, issueFileName, issueLine);
                    issueLineHash = SymbolResolver.resolveLineHash(project, issueFileName, issueLine);
                }
                excludedIssues.add(new ExcludedIssue(issue, ruleId, issueFileName, 
                        issueSymbol, issueLineHash, isGlobalExclusion));
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
            obj.addProperty("lineHash", ex.lineHash());
            obj.addProperty("ruleId", ex.ruleId());
            obj.addProperty("symbol", ex.symbol());
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
            obj.addProperty("severity", issue.rule().kind() != null ? issue.rule().kind().toString() : "WARNING");
            obj.addProperty("ruleKind", issue.rule().kind() != null ? issue.rule().kind().name() : "UNKNOWN");
        } else {
            obj.addProperty("ruleId", "UNKNOWN");
            obj.addProperty("message", "Unknown Issue Rule");
            obj.addProperty("severity", "WARNING");
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
}
