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

package io.ballerina.scan.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.cli.launcher.LauncherUtils;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.Issue;
import io.ballerina.scan.OwaspCoverage;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Severity;
import io.ballerina.scan.Standards;
import io.ballerina.scan.SuppressedIssue;
import io.ballerina.scan.internal.IssueImpl;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.TomlType;
import io.ballerina.toml.semantic.ast.TomlArrayValueNode;
import io.ballerina.toml.semantic.ast.TomlKeyValueNode;
import io.ballerina.toml.semantic.ast.TomlTableArrayNode;
import io.ballerina.toml.semantic.ast.TomlValueNode;
import io.ballerina.toml.semantic.ast.TopLevelNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.scan.utils.Constants.ANALYSIS_EXCLUDE_TESTS;
import static io.ballerina.scan.utils.Constants.ANALYSIS_TABLE;
import static io.ballerina.scan.utils.Constants.ANALYZER_NAME;
import static io.ballerina.scan.utils.Constants.ANALYZER_ORG;
import static io.ballerina.scan.utils.Constants.ANALYZER_REPOSITORY;
import static io.ballerina.scan.utils.Constants.ANALYZER_TABLE;
import static io.ballerina.scan.utils.Constants.ANALYZER_VERSION;
import static io.ballerina.scan.utils.Constants.CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN;
import static io.ballerina.scan.utils.Constants.GATE_FAIL_ON_COUNT;
import static io.ballerina.scan.utils.Constants.GATE_FAIL_ON_SEVERITY;
import static io.ballerina.scan.utils.Constants.GATE_TABLE;
import static io.ballerina.scan.utils.Constants.IMPORT_GENERATOR_FILE;
import static io.ballerina.scan.utils.Constants.JAR_PREDICATE;
import static io.ballerina.scan.utils.Constants.PATH_ARGUMENT_SUFFIX;
import static io.ballerina.scan.utils.Constants.PLATFORM_NAME;
import static io.ballerina.scan.utils.Constants.PLATFORM_PATH;
import static io.ballerina.scan.utils.Constants.PLATFORM_TABLE;
import static io.ballerina.scan.utils.Constants.REPORT_DATA_PLACEHOLDER;
import static io.ballerina.scan.utils.Constants.RESULTS_HTML_FILE;
import static io.ballerina.scan.utils.Constants.RESULTS_JSON_FILE;
import static io.ballerina.scan.utils.Constants.RESULTS_SARIF_FILE;
import static io.ballerina.scan.utils.Constants.RULE_DESCRIPTION_COLUMN;
import static io.ballerina.scan.utils.Constants.RULE_ID_COLUMN;
import static io.ballerina.scan.utils.Constants.RULE_KIND_COLUMN;
import static io.ballerina.scan.utils.Constants.RULE_PRIORITY_LIST;
import static io.ballerina.scan.utils.Constants.RULE_SEVERITY_COLUMN;
import static io.ballerina.scan.utils.Constants.RULES_TABLE;
import static io.ballerina.scan.utils.Constants.SARIF_SCHEMA;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_NAME;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_ORGANIZATION;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_VERSION;
import static io.ballerina.scan.utils.Constants.SARIF_VERSION;
import static io.ballerina.scan.utils.Constants.SCAN_FILE;
import static io.ballerina.scan.utils.Constants.SCAN_FILE_FIELD;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_CONTENT;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_PACKAGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_PATH;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_RELATIVE_PATH;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_CWE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_DETAILS;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_HELP_URI;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_KIND;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_MESSAGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_OWASP;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_OWASP_CATEGORIES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_OWASP_YEAR;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_RULE_ID;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_SEVERITY;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_SUPPRESSION;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TAGS;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE_OFFSET;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE_OFFSET;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TYPE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PACKAGES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PACKAGE_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PACKAGE_ORG;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PACKAGE_PATH;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PACKAGE_VERSION;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PROJECT_KIND;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PROJECT_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PROJECT_VERSION;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_SCANNED_FILES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_SUPPRESSION_RULES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ZIP_FILE;
import static io.ballerina.scan.utils.Constants.SCAN_TABLE;
import static io.ballerina.scan.utils.Constants.SUPPRESSIONS;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_ENABLED;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_JUSTIFICATION;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_KIND;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_KIND_IN_SOURCE;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_REQUIRE_REASON;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_REQUIRE_RULE_ID;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_STATUS;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_STATUS_ACCEPTED;
import static io.ballerina.scan.utils.Constants.SUPPRESSION_TABLE;

/**
 * {@code ScanUtils} contains all the utility functions used by the scan tool.
 *
 * @since 0.1.0
 */
public final class ScanUtils {
    /**
     * The gate severity that turns the gate off.
     *
     * @since 0.13.0
     */
    public static final String GATE_SEVERITY_NONE = "NONE";

    /**
     * The values a gate severity can take, as listed in messages.
     *
     * @since 0.13.0
     */
    public static final String GATE_SEVERITY_VALUES = "BLOCKER, HIGH, MEDIUM, LOW, INFO or NONE";

    /**
     * The values a gate count can take, as described in messages.
     *
     * @since 0.13.0
     */
    public static final String GATE_COUNT_VALUES = "a whole number of 1 or more";

    private ScanUtils() {
    }

    /**
     * Fails unless each package of the project has its dependencies locked in {@code Dependencies.toml}. The scan
     * uses the locked versions and does not resolve the dependencies of a package itself; {@code bal build} does.
     *
     * @param project the project to scan
     */
    public static void requireLockedDependencies(Project project) {
        Collection<BuildProject> packages = switch (project) {
            case BuildProject buildProject -> List.of(buildProject);
            case WorkspaceProject workspaceProject -> workspaceProject.projects();
            default -> List.of();
        };
        for (BuildProject buildProject : packages) {
            Package currentPackage = buildProject.currentPackage();
            // An empty package has no dependencies to lock, and is reported as empty instead
            if (!ProjectUtils.isProjectEmpty(buildProject) && currentPackage.dependenciesToml().isEmpty()) {
                String packageName = currentPackage.packageOrg().value() + "/" + currentPackage.packageName().value();
                throw LauncherUtils.createLauncherException(
                        DiagnosticLog.error(DiagnosticCode.MISSING_DEPENDENCIES_TOML, packageName));
            }
        }
    }

    /**
     * Prints issues generated via static code analyzers to the console.
     *
     * @param issues       generated issues
     * @param outputStream print stream
     */
    public static void printToConsole(List<Issue> issues, PrintStream outputStream) {
        printToConsole(issues, outputStream, false, null);
    }

    /**
     * Prints issues generated via static code analyzers to the console.
     *
     * @param issues       generated issues
     * @param outputStream print stream
     * @param printSarif   whether to print SARIF format instead of default BALLERINA format
     * @param project      Ballerina project (required for SARIF format)
     */
    public static void printToConsole(List<Issue> issues, PrintStream outputStream, boolean printSarif,
                                      Project project) {
        String output;
        if (printSarif && project != null) {
            output = convertIssuesToSarifString(issues, project);
        } else {
            output = convertIssuesToJsonString(issues);
        }
        outputStream.println();
        outputStream.println(output);
    }

    /**
     * Returns the json {@link String} array of generated issues.
     *
     * @param issues generated issues
     * @return json string array of generated issues
     */
    public static String convertIssuesToJsonString(List<Issue> issues) {
        return convertIssuesToJsonString(issues, Map.of());
    }

    /**
     * Returns the json {@link String} array of generated issues, where each suppressed issue carries a
     * {@code suppressions} entry mirroring the SARIF suppression object.
     *
     * @param issues       generated issues, including suppressed ones
     * @param suppressions the suppressed issues, keyed by the issue they suppress
     * @return json string array of generated issues
     * @since 0.13.0
     */
    public static String convertIssuesToJsonString(List<Issue> issues, Map<Issue, SuppressedIssue> suppressions) {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();

        Map<String, String> fileContentCache = new HashMap<>();
        for (int i = 0; i < issues.size(); i++) {
            IssueImpl issueImpl = (IssueImpl) issues.get(i);
            JsonObject issueAsJson = issuesAsJson.get(i).getAsJsonObject();
            String snippetText = SnippetExtractor.extract(issueImpl.filePath(), issueImpl.location().textRange(),
                    fileContentCache);
            if (snippetText != null) {
                issueAsJson.getAsJsonObject("location").addProperty("snippet", snippetText);
            }
            SuppressedIssue suppressedIssue = suppressions.get(issueImpl);
            if (suppressedIssue != null) {
                issueAsJson.add(SUPPRESSIONS, toSuppressionsJson(suppressedIssue));
            }
        }

        return gson.toJson(issuesAsJson);
    }

    private static JsonArray toSuppressionsJson(SuppressedIssue suppressedIssue) {
        JsonObject suppression = new JsonObject();
        suppression.addProperty(SUPPRESSION_KIND, SUPPRESSION_KIND_IN_SOURCE);
        suppression.addProperty(SUPPRESSION_STATUS, SUPPRESSION_STATUS_ACCEPTED);
        suppressedIssue.justification().ifPresent(reason -> suppression.addProperty(SUPPRESSION_JUSTIFICATION,
                reason));
        JsonArray suppressionsAsJson = new JsonArray();
        suppressionsAsJson.add(suppression);
        return suppressionsAsJson;
    }

    /**
     * Returns the SARIF (Static Analysis Results Interchange Format) {@link String}
     * representation of generated issues.
     *
     * @param issues  generated issues
     * @param project Ballerina project
     * @return SARIF string representation of generated issues
     */
    public static String convertIssuesToSarifString(List<Issue> issues, Project project) {
        return convertIssuesToSarifString(issues, project, Map.of());
    }

    /**
     * Returns the SARIF (Static Analysis Results Interchange Format) {@link String} representation of generated
     * issues, where each suppressed issue carries an {@code inSource} suppression with its justification.
     *
     * @param issues       generated issues, including suppressed ones
     * @param project      Ballerina project
     * @param suppressions the suppressed issues, keyed by the issue they suppress
     * @return SARIF string representation of generated issues
     * @since 0.13.0
     */
    public static String convertIssuesToSarifString(List<Issue> issues, Project project,
                                                    Map<Issue, SuppressedIssue> suppressions) {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        // Create SARIF root object
        JsonObject sarif = new JsonObject();
        sarif.addProperty("version", SARIF_VERSION);
        sarif.addProperty("$schema", SARIF_SCHEMA);

        // Create runs array
        JsonArray runs = new JsonArray();
        JsonObject run = new JsonObject();

        // Create tool information
        JsonObject tool = new JsonObject();
        JsonObject driver = new JsonObject();
        driver.addProperty("name", SARIF_TOOL_NAME);
        driver.addProperty("organization", SARIF_TOOL_ORGANIZATION);
        driver.addProperty("semanticVersion", SARIF_TOOL_VERSION);

        // Create rules array for the tool
        JsonArray rules = new JsonArray();
        Map<String, JsonObject> ruleMap = new HashMap<>();
        Map<String, Integer> ruleIndexMap = new HashMap<>();
        Set<String> addedRuleIds = new HashSet<>();

        // Collect unique rules from issues
        for (Issue issue : issues) {
            IssueImpl issueImpl = (IssueImpl) issue;
            Rule rule = issueImpl.rule();
            String ruleId = rule.id();

            JsonObject ruleObject = ruleMap.computeIfAbsent(ruleId, id -> buildSarifRuleObject(rule));
            if (addedRuleIds.add(ruleId)) {
                ruleIndexMap.put(ruleId, rules.size());
                rules.add(ruleObject);
            }
        }

        driver.add("rules", rules);
        tool.add("driver", driver);
        run.add("tool", tool);

        // Create results array
        JsonArray results = new JsonArray();
        Map<String, String> fileContentCache = new HashMap<>();

        for (Issue issue : issues) {
            IssueImpl issueImpl = (IssueImpl) issue;
            Rule rule = issueImpl.rule();
            JsonObject result = new JsonObject();

            result.addProperty("ruleId", rule.id());
            result.addProperty("ruleIndex", ruleIndexMap.get(rule.id()));
            result.addProperty("level", resolveSarifLevel(rule));

            JsonObject message = new JsonObject();
            message.addProperty("text", rule.details() != null ? rule.details() : rule.name());
            result.add("message", message);

            // Create locations array
            JsonArray locations = new JsonArray();
            JsonObject location = new JsonObject();
            JsonObject physicalLocation = new JsonObject();

            JsonObject artifactLocation = new JsonObject();
            String relativePath = getRelativePath(issueImpl.filePath(), project.sourceRoot().toString());
            artifactLocation.addProperty("uri", relativePath.replace("\\", "/"));
            physicalLocation.add("artifactLocation", artifactLocation);

            JsonObject region = new JsonObject();
            LineRange lineRange = issueImpl.location().lineRange();
            TextRange textRange = issueImpl.location().textRange();
            region.addProperty("startLine", lineRange.startLine().line() + 1);
            region.addProperty("startColumn", lineRange.startLine().offset() + 1);
            region.addProperty("endLine", lineRange.endLine().line() + 1);
            region.addProperty("endColumn", lineRange.endLine().offset() + 1);
            region.addProperty("charOffset", textRange.startOffset());
            region.addProperty("charLength", textRange.length());

            String snippetText = SnippetExtractor.extract(issueImpl.filePath(), textRange, fileContentCache);
            if (snippetText != null) {
                JsonObject snippet = new JsonObject();
                snippet.addProperty("text", snippetText);
                region.add("snippet", snippet);
            }

            physicalLocation.add("region", region);

            location.add("physicalLocation", physicalLocation);
            locations.add(location);
            result.add("locations", locations);

            // SARIF requires the suppressions of a run's results to be all present or all absent; an empty array
            // marks a result as not suppressed
            SuppressedIssue suppressedIssue = suppressions.get(issueImpl);
            if (suppressedIssue != null) {
                result.add(SUPPRESSIONS, toSuppressionsJson(suppressedIssue));
            } else if (!suppressions.isEmpty()) {
                result.add(SUPPRESSIONS, new JsonArray());
            }

            results.add(result);
        }

        run.add("results", results);
        runs.add(run);
        sarif.add("runs", runs);

        return gson.toJson(sarif);
    }

    /**
     * Builds the SARIF rule object for a single {@link Rule}, including the enriched metadata
     * (full description, tags, CWE/OWASP coverage, ruleKind) when the rule carries it.
     *
     * @param rule the rule to build a SARIF rule object for
     * @return the SARIF rule object
     */
    private static JsonObject buildSarifRuleObject(Rule rule) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", rule.id());

        // Only rules whose metadata authors a helpUri get one; older plugin rules.json files omit it.
        if (rule.helpUri() != null) {
            obj.addProperty("helpUri", rule.helpUri());
        }

        JsonObject shortDescription = new JsonObject();
        shortDescription.addProperty("text", rule.description());
        obj.add("shortDescription", shortDescription);

        // details() falls back to description(), so only emit it when it's actually distinct.
        if (rule.details() != null && !rule.details().equals(rule.description())) {
            JsonObject fullDescription = new JsonObject();
            fullDescription.addProperty("text", rule.details());
            obj.add("fullDescription", fullDescription);
        }

        JsonObject defaultConfiguration = new JsonObject();
        defaultConfiguration.addProperty("level", resolveSarifLevel(rule));
        obj.add("defaultConfiguration", defaultConfiguration);

        JsonObject properties = new JsonObject();
        properties.addProperty("ruleKind", rule.kind().toString());
        List<String> sarifTags = buildSarifTags(rule);
        if (!sarifTags.isEmpty()) {
            JsonArray tags = new JsonArray();
            sarifTags.forEach(tags::add);
            properties.add("tags", tags);
        }
        obj.add("properties", properties);

        return obj;
    }

    /**
     * Resolves the SARIF reporting level for a rule. When the rule has an explicit
     * {@link Severity} (i.e. its entry in {@code core-rules/rules.json} specifies one), that takes
     * priority: {@code BLOCKER}/{@code HIGH} to {@code error}, {@code MEDIUM} to {@code warning},
     * {@code LOW} to {@code note}, {@code INFO} to {@code none}. Otherwise - for any rule with no
     * {@link Severity} at all, whether an external/plugin rule or a core rule whose JSON simply
     * omits {@code severity} - this falls back to the original {@link RuleKind}-based mapping
     * ({@code BUG} to {@code error}, {@code CODE_SMELL} to {@code note}, {@code VULNERABILITY} to
     * {@code warning}), matching the tool's pre-existing/upstream behavior.
     *
     * @param rule the rule to resolve a SARIF level for
     * @return the resolved SARIF level
     */
    private static String resolveSarifLevel(Rule rule) {
        return switch (effectiveSeverity(rule)) {
            case BLOCKER, HIGH -> "error";
            case MEDIUM -> "warning";
            case LOW -> "note";
            case INFO -> "none";
        };
    }

    /**
     * Returns the severity a rule is reported and gated with: its own {@link Severity}, or for a rule without one, a
     * severity from its {@link RuleKind} ({@code BUG} to {@code HIGH}, {@code VULNERABILITY} to {@code MEDIUM} and
     * {@code CODE_SMELL} to {@code LOW}), so that SARIF levels and the gate agree.
     *
     * @param rule the rule to resolve a severity for
     * @return the severity of the rule
     * @since 0.13.0
     */
    public static Severity effectiveSeverity(Rule rule) {
        if (rule.severity() != null) {
            return rule.severity();
        }
        return switch (rule.kind()) {
            case BUG -> Severity.HIGH;
            case VULNERABILITY -> Severity.MEDIUM;
            case CODE_SMELL -> Severity.LOW;
        };
    }

    /**
     * Parses a gate severity, ignoring case.
     *
     * @param value the severity as given on the command line or in Scan.toml
     * @return the severity name or {@code NONE} in upper case, or empty if the value is not a gate severity
     * @since 0.13.0
     */
    public static Optional<String> parseGateSeverity(String value) {
        String severity = value.trim().toUpperCase(Locale.ROOT);
        if (GATE_SEVERITY_NONE.equals(severity)
                || Arrays.stream(Severity.values()).anyMatch(known -> known.name().equals(severity))) {
            return Optional.of(severity);
        }
        return Optional.empty();
    }

    /**
     * Parses a gate count.
     *
     * @param value the count as given on the command line
     * @return the count, or empty if the value is not a whole number of 1 or more
     * @since 0.13.0
     */
    public static Optional<Integer> parseGateCount(String value) {
        try {
            int count = Integer.parseInt(value.trim());
            return count >= 1 ? Optional.of(count) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * Builds the full SARIF {@code properties.tags} list for a rule: its general {@link Rule#tags()}
     * plus {@code external/cwe/cwe-*} and {@code external/owasp/owasp-a*-*} entries generated from
     * {@link Rule#standards()}.
     *
     * @param rule the rule to build SARIF tags for
     * @return the combined tag list, or an empty list when the rule has neither
     */
    private static List<String> buildSarifTags(Rule rule) {
        List<String> sarifTags = new ArrayList<>();
        if (rule.tags() != null) {
            sarifTags.addAll(rule.tags());
        }
        Standards standards = rule.standards();
        if (standards != null) {
            sarifTags.addAll(standards.toSarifTags());
        }
        return sarifTags;
    }

    /**
     * Returns the relative path from the source root.
     *
     * @param filePath   the absolute file path
     * @param sourceRoot the source root path
     * @return relative path
     */
    public static String getRelativePath(String filePath, String sourceRoot) {
        Path path = Paths.get(filePath);
        try {
            Path source = Paths.get(sourceRoot);
            return source.relativize(path).toString();
        } catch (IllegalArgumentException e) {
            Path fileName = path.getFileName();
            return fileName != null ? fileName.toString() : filePath;
        }
    }

    /**
     * Returns the {@link Path} of the json analysis report where generated issues
     * are saved.
     * Optionally generates a SARIF format report in the same directory.
     *
     * @param issues        generated issues
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return path of the json analysis report where generated issues are saved
     */
    public static Path saveToDirectory(List<Issue> issues, Project project, String directoryName) {
        return saveToDirectory(issues, project, directoryName, Map.of());
    }

    /**
     * Returns the {@link Path} of the json analysis report where generated issues, including suppressed ones, are
     * saved.
     *
     * @param issues        generated issues, including suppressed ones
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @param suppressions  the suppressed issues, keyed by the issue they suppress
     * @return path of the json analysis report where generated issues are saved
     * @since 0.13.0
     */
    public static Path saveToDirectory(List<Issue> issues, Project project, String directoryName,
                                       Map<Issue, SuppressedIssue> suppressions) {
        Target target = getTargetPath(project, directoryName);

        Path jsonFilePath;
        try {
            Path reportPath = target.getReportPath();

            // Save JSON report
            String jsonOutput = convertIssuesToJsonString(issues, suppressions);
            File jsonFile = new File(reportPath.resolve(RESULTS_JSON_FILE).toString());
            try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFile);
                 Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                writer.write(new String(jsonOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                jsonFilePath = jsonFile.toPath();
            }
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.FAILED_TO_WRITE_REPORT, RESULTS_JSON_FILE,
                    ex.getMessage()));
        }
        return jsonFilePath;
    }

    /**
     * Saves only the SARIF analysis report to the target directory.
     *
     * @param issues        generated issues
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return path of the SARIF analysis report
     */
    public static Path saveSarifToDirectory(List<Issue> issues, Project project, String directoryName) {
        return saveSarifToDirectory(issues, project, directoryName, Map.of());
    }

    /**
     * Saves only the SARIF analysis report, including suppressed issues, to the target directory.
     *
     * @param issues        generated issues, including suppressed ones
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @param suppressions  the suppressed issues, keyed by the issue they suppress
     * @return path of the SARIF analysis report
     * @since 0.13.0
     */
    public static Path saveSarifToDirectory(List<Issue> issues, Project project, String directoryName,
                                            Map<Issue, SuppressedIssue> suppressions) {
        Target target = getTargetPath(project, directoryName);
        try {
            Path reportPath = target.getReportPath();
            String sarifOutput = convertIssuesToSarifString(issues, project, suppressions);
            File sarifFile = new File(reportPath.resolve(RESULTS_SARIF_FILE).toString());
            try (FileOutputStream fos = new FileOutputStream(sarifFile);
                 Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(new String(sarifOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            }
            return sarifFile.toPath();
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.FAILED_TO_WRITE_REPORT, RESULTS_SARIF_FILE,
                    ex.getMessage()));
        }
    }

    /**
     * Returns the generated {@link Target} directory where analysis reports are
     * saved.
     *
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return generated target directory
     */
    public static Target getTargetPath(Project project, String directoryName) {
        try {
            if (directoryName == null) {
                return new Target(project.targetDir());
            }
            Path parentDirectory = project.sourceRoot();
            if (parentDirectory == null) {
                return new Target(project.targetDir());
            }
            Path targetDirectory = Files.createDirectories(parentDirectory.resolve(directoryName));
            return new Target(targetDirectory);
        } catch (IOException ex) {
            throw new IllegalCharsetNameException(ex.toString());
        }
    }

    /**
     * Returns the {@link Path} of the html analysis report where generated issues
     * are saved.
     *
     * @param issues        generated issues
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return path of the html analysis report where generated issues are saved
     */
    public static Path generateScanReport(List<Issue> issues, Project project, String directoryName) {
        return generateScanReport(issues, project, directoryName, Map.of());
    }

    /**
     * Returns the {@link Path} of the html analysis report where generated issues are saved. Each suppressed
     * issue is listed with its suppression, so that the report can show the accepted issues apart from the
     * active ones.
     *
     * @param issues        generated issues, including suppressed ones
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @param suppressions  the suppressed issues, keyed by the issue they suppress
     * @return path of the html analysis report where generated issues are saved
     * @since 0.13.0
     */
    public static Path generateScanReport(List<Issue> issues, Project project, String directoryName,
                                          Map<Issue, SuppressedIssue> suppressions) {
        return generateScanReport(issues, project, directoryName, suppressions, analyzedPackage -> false);
    }

    /**
     * Returns the {@link Path} of the html analysis report where generated issues are saved, leaving out the test
     * sources of the packages whose tests were not scanned.
     *
     * @param issues        generated issues, including suppressed ones
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @param suppressions  the suppressed issues, keyed by the issue they suppress
     * @param testsExcluded whether the test sources of a package were left out of the scan
     * @return path of the html analysis report where generated issues are saved
     * @since 0.13.0
     */
    public static Path generateScanReport(List<Issue> issues, Project project, String directoryName,
                                          Map<Issue, SuppressedIssue> suppressions,
                                          Predicate<Package> testsExcluded) {
        JsonObject scannedProject = new JsonObject();
        String name;
        if (project.kind() == ProjectKind.WORKSPACE_PROJECT) {
            name = Optional.of(project.sourceRoot().getFileName()).get().toString();
        } else {
            name = project.currentPackage().packageName().toString();
        }
        scannedProject.addProperty(SCAN_REPORT_PROJECT_NAME, name);
        scannedProject.addProperty(SCAN_REPORT_PROJECT_KIND, project.kind().name());
        if (project.kind() == ProjectKind.BUILD_PROJECT) {
            scannedProject.addProperty(SCAN_REPORT_PROJECT_VERSION,
                    project.currentPackage().packageVersion().toString());
        }

        Path reportRoot = project.sourceRoot().toAbsolutePath().normalize();
        JsonArray scannedPackages = new JsonArray();
        Map<String, JsonObject> scanReportPathAndFile = new LinkedHashMap<>();
        for (Package analyzedPackage : getAnalyzedPackages(project)) {
            String packageName = analyzedPackage.packageName().toString();
            JsonObject scannedPackage = new JsonObject();
            scannedPackage.addProperty(SCAN_REPORT_PACKAGE_ORG, analyzedPackage.packageOrg().toString());
            scannedPackage.addProperty(SCAN_REPORT_PACKAGE_NAME, packageName);
            scannedPackage.addProperty(SCAN_REPORT_PACKAGE_VERSION, analyzedPackage.packageVersion().toString());
            scannedPackage.addProperty(SCAN_REPORT_PACKAGE_PATH,
                    toRelativePath(reportRoot, analyzedPackage.project().sourceRoot()));
            scannedPackages.add(scannedPackage);

            for (Document document : getAnalyzedDocuments(analyzedPackage, testsExcluded.test(analyzedPackage))) {
                // Documents that exist only in memory have no file to show; issues in them still get an entry below.
                Optional<Path> optionalDocumentPath = document.module().project().documentPath(document.documentId());
                if (optionalDocumentPath.isEmpty()) {
                    continue;
                }
                Path documentPath = optionalDocumentPath.get();
                JsonObject scanReportFile = createScanReportFile(getReportFileName(document),
                        documentPath.toString());
                String relativePath = toRelativePath(reportRoot, documentPath);
                if (relativePath != null) {
                    scanReportFile.addProperty(SCAN_REPORT_FILE_RELATIVE_PATH, relativePath);
                }
                scanReportFile.addProperty(SCAN_REPORT_FILE_PACKAGE, packageName);
                scanReportPathAndFile.put(toReportFileKey(documentPath.toString()), scanReportFile);
            }
        }
        scannedProject.add(SCAN_REPORT_PACKAGES, scannedPackages);
        for (Issue issue : issues) {
            IssueImpl issueImpl = (IssueImpl) issue;
            JsonObject scanReportFile = scanReportPathAndFile.computeIfAbsent(toReportFileKey(issueImpl.filePath()),
                    key -> createScanReportFile(issueImpl.fileName(), issueImpl.filePath()));
            scanReportFile.getAsJsonArray(SCAN_REPORT_ISSUES).add(getJsonIssue(issueImpl, suppressions.get(issue)));
        }

        JsonArray scannedFiles = new JsonArray();
        scanReportPathAndFile.values().forEach(scannedFiles::add);
        scannedProject.add(SCAN_REPORT_SCANNED_FILES, scannedFiles);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scannedProject);
        Target target = getTargetPath(project, directoryName);
        InputStream innerJarStream = ScanUtils.class.getClassLoader().getResourceAsStream(SCAN_REPORT_ZIP_FILE);

        File htmlFile;
        try {
            Path htmlReportPath = target.getReportPath();
            unzipReportResources(innerJarStream, htmlReportPath.toFile());
            String content = Files.readString(htmlReportPath.resolve(RESULTS_HTML_FILE));
            content = content.replace(REPORT_DATA_PLACEHOLDER, jsonOutput);
            htmlFile = new File(htmlReportPath.resolve(RESULTS_HTML_FILE).toString());
            FileOutputStream fileOutputStream = new FileOutputStream(htmlFile);

            try (Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                writer.write(new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.FAILED_TO_COPY_SCAN_REPORT,
                    ex.getMessage()));
        }
        return htmlFile.toPath();
    }

    /**
     * Returns the path of a document as recorded in the issues reported for it.
     *
     * @param document the document
     * @return the path of the document, or its name when it has no path
     */
    public static String getReportFilePath(Document document) {
        return document.module().project().documentPath(document.documentId())
                .orElse(Path.of(document.name())).toString();
    }

    /**
     * Returns the name used to identify a document in scan results. Documents of workspace
     * packages are prefixed with their module name so that files across packages stay distinct.
     *
     * @param document Ballerina document
     * @return name of the document as shown in scan results
     */
    public static String getReportFileName(Document document) {
        Module module = document.module();
        return module.project().workspaceProject().isPresent()
                ? module.moduleName().toString() + "/" + document.name()
                : document.name();
    }

    private static List<Package> getAnalyzedPackages(Project project) {
        if (project instanceof WorkspaceProject workspaceProject) {
            return workspaceProject.getResolution().dependencyGraph().toTopologicallySortedList().stream()
                    .filter(buildProject -> !ProjectUtils.isProjectEmpty(buildProject))
                    .map(Project::currentPackage)
                    .toList();
        }
        return List.of(project.currentPackage());
    }

    private static List<Document> getAnalyzedDocuments(Package analyzedPackage, boolean testsExcluded) {
        List<Document> documents = new ArrayList<>();
        for (ModuleId moduleId : analyzedPackage.moduleIds()) {
            Module module = analyzedPackage.module(moduleId);
            Stream.concat(module.documentIds().stream(),
                            testsExcluded ? Stream.empty() : module.testDocumentIds().stream())
                    .map(module::document)
                    // Skip the in-memory document the analyzer adds to import external analyzer plugins
                    .filter(document -> !document.name().startsWith(IMPORT_GENERATOR_FILE))
                    .forEach(documents::add);
        }
        return documents;
    }

    // Uses forward slashes regardless of OS so the report can split paths into folders; null when outside root.
    private static String toRelativePath(Path root, Path path) {
        Path relativePath = root.relativize(path.toAbsolutePath().normalize());
        if (relativePath.startsWith("..")) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        relativePath.forEach(segment -> segments.add(segment.toString()));
        return String.join("/", segments);
    }

    private static String toReportFileKey(String filePath) {
        return Path.of(filePath).toAbsolutePath().normalize().toString();
    }

    private static JsonObject createScanReportFile(String fileName, String filePath) {
        JsonObject scanReportFile = new JsonObject();
        scanReportFile.addProperty(SCAN_REPORT_FILE_NAME, fileName);
        scanReportFile.addProperty(SCAN_REPORT_FILE_PATH, filePath);
        String fileContent;
        try {
            fileContent = Files.readString(Path.of(filePath));
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.FAILED_TO_READ_BALLERINA_FILE,
                    ex.getMessage()));
        }
        scanReportFile.addProperty(SCAN_REPORT_FILE_CONTENT, fileContent);
        scanReportFile.add(SCAN_REPORT_ISSUES, new JsonArray());
        return scanReportFile;
    }

    /**
     * Returns the {@link JsonObject} representation of the static code analysis
     * issue.
     *
     * @param issueImpl the {@link IssueImpl} representing the issue, to be
     *                  converted to a JSON object
     * @return json object representation of the static code analysis issue
     */
    private static JsonObject getJsonIssue(IssueImpl issueImpl, SuppressedIssue suppressedIssue) {
        Rule rule = issueImpl.rule();
        JsonObject scanReportIssue = new JsonObject();
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_RULE_ID, rule.id());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_NAME, rule.name());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_KIND, rule.kind().toString());
        if (rule.severity() != null) {
            scanReportIssue.addProperty(SCAN_REPORT_ISSUE_SEVERITY, rule.severity().toString());
        }
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_TYPE, issueImpl.source().toString());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_MESSAGE, rule.description());
        // details() falls back to description(), so only emit it when it's actually distinct.
        if (rule.details() != null && !rule.details().equals(rule.description())) {
            scanReportIssue.addProperty(SCAN_REPORT_ISSUE_DETAILS, rule.details());
        }
        if (rule.helpUri() != null) {
            scanReportIssue.addProperty(SCAN_REPORT_ISSUE_HELP_URI, rule.helpUri());
        }
        if (rule.tags() != null && !rule.tags().isEmpty()) {
            JsonArray tags = new JsonArray();
            rule.tags().forEach(tags::add);
            scanReportIssue.add(SCAN_REPORT_ISSUE_TAGS, tags);
        }
        addStandards(scanReportIssue, rule.standards());

        JsonObject scanReportIssueTextRange = new JsonObject();
        LineRange lineRange = issueImpl.location().lineRange();
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE, lineRange.startLine().line());
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE_OFFSET, lineRange.startLine()
                .offset());
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE, (int) lineRange.endLine().line());
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE_OFFSET, (int) lineRange.endLine()
                .offset());

        scanReportIssue.add(SCAN_REPORT_ISSUE_TEXT_RANGE, scanReportIssueTextRange);
        if (suppressedIssue != null) {
            scanReportIssue.add(SCAN_REPORT_ISSUE_SUPPRESSION, getJsonSuppression(suppressedIssue));
        }
        return scanReportIssue;
    }

    // An empty rule list means the suppression applies to every rule.
    private static JsonObject getJsonSuppression(SuppressedIssue suppressedIssue) {
        JsonObject suppression = new JsonObject();
        suppressedIssue.justification().ifPresent(reason -> suppression.addProperty(SUPPRESSION_JUSTIFICATION,
                reason));
        JsonArray rules = new JsonArray();
        suppressedIssue.rules().forEach(rules::add);
        suppression.add(SCAN_REPORT_SUPPRESSION_RULES, rules);
        return suppression;
    }

    /**
     * Adds the CWE and OWASP coverage of a rule to its scan report issue, omitting whichever is empty.
     *
     * @param scanReportIssue the scan report issue to add the coverage to
     * @param standards       the standards coverage of the rule, or {@code null}
     */
    private static void addStandards(JsonObject scanReportIssue, Standards standards) {
        if (standards == null) {
            return;
        }
        if (!standards.cwe().isEmpty()) {
            JsonArray cwe = new JsonArray();
            standards.cwe().forEach(cwe::add);
            scanReportIssue.add(SCAN_REPORT_ISSUE_CWE, cwe);
        }
        if (!standards.owasp().isEmpty()) {
            JsonArray owasp = new JsonArray();
            for (OwaspCoverage coverage : standards.owasp()) {
                JsonObject coverageObject = new JsonObject();
                coverageObject.addProperty(SCAN_REPORT_ISSUE_OWASP_YEAR, coverage.year());
                JsonArray categories = new JsonArray();
                coverage.categories().forEach(categories::add);
                coverageObject.add(SCAN_REPORT_ISSUE_OWASP_CATEGORIES, categories);
                owasp.add(coverageObject);
            }
            scanReportIssue.add(SCAN_REPORT_ISSUE_OWASP, owasp);
        }
    }

    /**
     * Extracts the HTML report template zip from a provided resource stream to the
     * provided destination.
     *
     * @param source resource stream that contains the zip
     * @param target destination to extract contents of the zip
     */
    private static void unzipReportResources(InputStream source, File target) throws IOException {
        final ZipInputStream zipStream = new ZipInputStream(source);
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            if (nextEntry.isDirectory()) {
                continue;
            }

            final File nextFile = new File(target, nextEntry.getName());
            final File parent = nextFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }

            try (OutputStream targetStream = new FileOutputStream(nextFile)) {
                final int bufferSize = 4 * 1024;
                final byte[] buffer = new byte[bufferSize];
                int nextCount = zipStream.read(buffer);
                while (nextCount >= 0) {
                    targetStream.write(buffer, 0, nextCount);
                    nextCount = zipStream.read(buffer);
                }
            }
        }
    }

    /**
     * Returns the {@link Optional<ScanTomlFile>} representation of a local/remote
     * Scan.toml file, with the Scan.toml of the enclosing workspace as the default. TOML syntax errors and invalid
     * settings are ignored, as only {@code bal scan} rejects them.
     *
     * @param project      Ballerina project
     * @param outputStream print stream
     * @return the optional in-memory representation of the Scan.toml file, which is empty after printing the error
     * when the configurations cannot be loaded
     */
    public static Optional<ScanTomlFile> loadScanTomlConfigurations(Project project, PrintStream outputStream) {
        try {
            ScanTomlFile scanTomlFile = readScanTomlConfigurations(project, outputStream, false);
            Optional<WorkspaceProject> workspaceProject = project.workspaceProject();
            if (workspaceProject.isPresent()) {
                Optional<ScanTomlFile> workspaceScanToml = readWorkspaceScanToml(workspaceProject.get(),
                        outputStream, false);
                if (workspaceScanToml.isPresent()) {
                    scanTomlFile = scanTomlFile.inherit(workspaceScanToml.get());
                }
            }
            return Optional.of(scanTomlFile);
        } catch (ScanToolException ex) {
            outputStream.println(ex.getMessage());
            ex.diagnostics().forEach(outputStream::println);
            return Optional.empty();
        }
    }

    /**
     * Returns the in-memory representation of the local or remote Scan.toml file of a package, without the defaults
     * of an enclosing workspace.
     *
     * @param project      Ballerina project
     * @param outputStream print stream for progress messages
     * @return the in-memory representation of the Scan.toml file, which is empty when the package has none
     * @throws ScanToolException if the configurations cannot be loaded, with a diagnostic for each error found in
     *                           the file
     * @since 0.13.0
     */
    public static ScanTomlFile readScanTomlConfigurations(Project project, PrintStream outputStream) {
        return readScanTomlConfigurations(project, outputStream, true);
    }

    private static ScanTomlFile readScanTomlConfigurations(Project project, PrintStream outputStream,
                                                           boolean strict) {
        if (!project.kind().equals(ProjectKind.BUILD_PROJECT) || project.currentPackage().ballerinaToml().isEmpty()) {
            return new ScanTomlFile();
        }

        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().get();
        TomlDocument ballerinaTomlDocument = ballerinaToml.tomlDocument();
        return readScanFile(ballerinaTomlDocument.toml(), project.sourceRoot(), project.targetDir(), outputStream,
                strict, false).orElseGet(ScanTomlFile::new);
    }

    /**
     * Returns the in-memory representation of the Scan.toml file of a workspace, which is found the same way as the
     * Scan.toml file of a package: at the workspace root, or at the {@code [scan] configPath} of the workspace
     * Ballerina.toml file. It can set every table a package Scan.toml can, except {@code [[platform]]}.
     *
     * @param workspaceProject the workspace
     * @param outputStream     print stream for progress messages
     * @return the in-memory representation of the file, or empty when the workspace has none
     * @throws ScanToolException if the file cannot be loaded or has errors
     * @since 0.13.0
     */
    public static Optional<ScanTomlFile> readWorkspaceScanToml(WorkspaceProject workspaceProject,
                                                               PrintStream outputStream) {
        return readWorkspaceScanToml(workspaceProject, outputStream, true);
    }

    private static Optional<ScanTomlFile> readWorkspaceScanToml(WorkspaceProject workspaceProject,
                                                                PrintStream outputStream, boolean strict) {
        Path sourceRoot = workspaceProject.sourceRoot();
        Path ballerinaTomlPath = sourceRoot.resolve(ProjectConstants.BALLERINA_TOML);
        Toml ballerinaToml;
        try {
            ballerinaToml = Toml.read(ballerinaTomlPath);
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.READING_CONFIG_FILE, ex.getMessage()));
        }
        return readScanFile(ballerinaToml, sourceRoot, workspaceProject.targetDir(), outputStream, strict, true);
    }

    // Finds the Scan.toml file the [scan] table of a Ballerina.toml file points to, or the one at the root
    private static Optional<ScanTomlFile> readScanFile(Toml ballerinaToml, Path sourceRoot, Path targetDir,
                                                       PrintStream outputStream, boolean strict, boolean workspace) {
        if (ballerinaToml.getTable(SCAN_TABLE).isEmpty()) {
            Path scanTomlFilePath = sourceRoot.resolve(SCAN_FILE);
            if (Files.isRegularFile(scanTomlFilePath)) {
                outputStream.println("Loading scan tool configurations from " + scanTomlFilePath);
                return Optional.of(loadScanFile(targetDir, scanTomlFilePath, displayPath(sourceRoot, scanTomlFilePath),
                        sourceRoot.toAbsolutePath(), outputStream, strict, workspace));
            }
            return Optional.empty();
        }

        Toml scanTable = ballerinaToml.getTable(SCAN_TABLE).get();
        if (scanTable.get(SCAN_FILE_FIELD).isEmpty()) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.MISSING_CONFIG_FIELD));
        }

        TomlValueNode configPath = scanTable.get(SCAN_FILE_FIELD).get();
        String scanTomlPath = configPath.toNativeValue().toString();
        Optional<Path> path = convertConfigurationPath(scanTomlPath);
        Optional<Path> absPath = path.isEmpty() || path.get().isAbsolute() ? path
                : Optional.of(sourceRoot.resolve(scanTomlPath));

        if (absPath.isPresent() && absPath.get().toFile().isFile()) {
            return Optional.of(loadScanFile(targetDir, absPath.get(), displayPath(sourceRoot, absPath.get()),
                    absPath.get().toAbsolutePath().getParent(), outputStream, strict, workspace));
        }
        // A remote file has no folder of its own, so its relative paths are resolved against the root
        return Optional.of(loadRemoteScanFile(targetDir, scanTomlPath, sourceRoot.toAbsolutePath(), outputStream,
                strict, workspace));
    }

    /**
     * Returns the path of a file as shown to the user: relative to a root, with forward slashes on every OS, or as
     * given when the file is outside the root.
     *
     * @param root the directory the path is shown relative to
     * @param file the file
     * @return the path as shown to the user
     * @since 0.13.0
     */
    public static String displayPath(Path root, Path file) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();
        if (!normalizedFile.startsWith(normalizedRoot)) {
            return file.toString();
        }
        List<String> segments = new ArrayList<>();
        normalizedRoot.relativize(normalizedFile).forEach(segment -> segments.add(segment.toString()));
        return String.join("/", segments);
    }

    /**
     * Returns the {@link Optional<Path> } if the path is valid.
     *
     * @param path the path to check
     * @return the optional path of the configuration file
     */
    private static Optional<Path> convertConfigurationPath(String path) {
        try {
            return Optional.of(Paths.get(path));
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns the {@link ScanTomlFile} representation a remote Scan.toml
     * file from the provided URL.
     *
     * @param targetDir    the target directory
     * @param scanTomlPath the remote Scan.toml file path
     * @param baseDir      the directory relative paths in the file are resolved against
     * @param outputStream the print stream
     * @param strict       whether TOML syntax errors and invalid settings fail the load
     * @param workspace    whether the file configures a workspace
     * @return the in-memory representation of the remote Scan.toml file
     */
    private static ScanTomlFile loadRemoteScanFile(Path targetDir, String scanTomlPath, Path baseDir,
                                                   PrintStream outputStream, boolean strict, boolean workspace) {
        // Keyed by URL, so that a workspace and its packages sharing a target directory do not share a cache
        Path cachePath = targetDir.resolve(Integer.toHexString(scanTomlPath.hashCode()) + "-" + SCAN_FILE);

        if (Files.exists(cachePath)) {
            outputStream.println("Loading scan tool configurations from cache");
            ScanTomlFile scanTomlFile = loadScanFile(targetDir, cachePath, scanTomlPath, baseDir, outputStream,
                    strict, workspace);
            scanTomlFile.setRemoteLocation(scanTomlPath);
            return scanTomlFile;
        }

        URL remoteScanTomlFilePath;
        try {
            remoteScanTomlFilePath = new URL(scanTomlPath);
            FileUtils.copyURLToFile(remoteScanTomlFilePath, cachePath.toFile());
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_CONFIG_FILE,
                    ex.getMessage()));
        }
        outputStream.println("Loading scan tool configurations from " + remoteScanTomlFilePath);
        ScanTomlFile scanTomlFile = loadScanFile(targetDir, cachePath, scanTomlPath, baseDir, outputStream, strict,
                workspace);
        scanTomlFile.setRemoteLocation(scanTomlPath);
        return scanTomlFile;
    }

    /**
     * Returns the {@link ScanTomlFile} representation of the Scan.toml
     * file.
     *
     * @param targetDir        the target directory
     * @param scanTomlFilePath the local Scan.toml file path
     * @param displayPath      the path of the file as shown to the user
     * @param baseDir          the directory relative paths in the file are resolved against
     * @param outputStream     the print stream
     * @param strict           whether TOML syntax errors and invalid settings fail the load
     * @param workspace        whether the file configures a workspace, which cannot set platforms
     * @return the in-memory representation of the Scan.toml file
     */
    private static ScanTomlFile loadScanFile(Path targetDir, Path scanTomlFilePath, String displayPath, Path baseDir,
                                             PrintStream outputStream, boolean strict, boolean workspace) {
        Toml scanTomlDocumentContent = readTomlFile(scanTomlFilePath, displayPath, strict);
        ScanTomlFile scanTomlFile = new ScanTomlFile();
        scanTomlFile.setPath(scanTomlFilePath);
        Map<String, TopLevelNode> entries = scanTomlDocumentContent.rootNode().entries();
        List<String> errors = new ArrayList<>();

        TopLevelNode platformNode = entries.get(PLATFORM_TABLE);
        if (workspace) {
            // Each package reports to its own platform project, so a workspace cannot set one for all of them
            if (platformNode != null) {
                errors.add(DiagnosticLog.fileError(displayPath, platformNode.location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.WORKSPACE_PLATFORM_NOT_ALLOWED)));
            }
        } else {
            for (Toml platformTable : scanTomlDocumentContent.getTables(PLATFORM_TABLE)) {
                loadPlatform(platformTable, scanTomlFile, targetDir, baseDir, outputStream, strict);
            }
        }
        TopLevelNode analyzerNode = entries.get(ANALYZER_TABLE);
        if (analyzerNode instanceof TomlTableArrayNode) {
            scanTomlFile.markAnalyzerListSet();
            scanTomlDocumentContent.getTables(ANALYZER_TABLE).forEach(loadAnalyzer(scanTomlFile));
        } else if (analyzerNode instanceof TomlKeyValueNode keyValue
                && keyValue.value() instanceof TomlArrayValueNode array && array.elements().isEmpty()) {
            // Written as analyzer = [], the list clears the analyzers of the workspace
            scanTomlFile.markAnalyzerListSet();
        } else if (analyzerNode != null) {
            errors.add(DiagnosticLog.fileError(displayPath, analyzerNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_ANALYZER_LIST)));
        }
        scanTomlDocumentContent.getTable(RULES_TABLE).ifPresent(ruleTable -> {
            // Written as an empty [rule], the table clears the rules of the workspace
            scanTomlFile.markRuleTableSet();
            ruleTable.get("include").ifPresent(loadRules(scanTomlFile, true));
            ruleTable.get("exclude").ifPresent(loadRules(scanTomlFile, false));
            if (!scanTomlFile.getRulesToInclude().isEmpty() && !scanTomlFile.getRulesToExclude().isEmpty()) {
                errors.add(DiagnosticLog.fileError(displayPath, entries.get(RULES_TABLE).location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.ATTEMPT_TO_INCLUDE_AND_EXCLUDE)));
            }
        });
        // Written in any other form, such as [[suppression]], the settings would not be read at all
        TopLevelNode suppressionNode = scanTomlDocumentContent.rootNode().entries().get(SUPPRESSION_TABLE);
        if (suppressionNode != null && suppressionNode.kind() != TomlType.TABLE) {
            throw LauncherUtils.createLauncherException(DiagnosticLog.error(DiagnosticCode.INVALID_SUPPRESSION_TABLE));
        }
        Optional<Toml> suppressionTable = scanTomlDocumentContent.getTable(SUPPRESSION_TABLE);
        if (suppressionTable.isPresent()) {
            Toml table = suppressionTable.get();
            validateSuppressionSettings(table);
            scanTomlFile.markSuppressionTableSet();
            scanTomlFile.setSuppressionEnabled(booleanSetting(table, SUPPRESSION_ENABLED, true));
            scanTomlFile.setSuppressionReasonRequired(booleanSetting(table, SUPPRESSION_REQUIRE_REASON, false));
            scanTomlFile.setSuppressionRuleIdRequired(booleanSetting(table, SUPPRESSION_REQUIRE_RULE_ID, false));
        }

        scanTomlDocumentContent.getTable(GATE_TABLE).ifPresent(gateTable -> {
            // Written as an empty [gate], the table turns off the gate of the workspace
            scanTomlFile.markGateTableSet();
            loadGate(gateTable, scanTomlFile, displayPath, errors);
        });
        TopLevelNode analysisNode = entries.get(ANALYSIS_TABLE);
        if (analysisNode != null && analysisNode.kind() != TomlType.TABLE) {
            errors.add(DiagnosticLog.fileError(displayPath, analysisNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_CONFIG_TABLE, ANALYSIS_TABLE)));
        } else if (analysisNode != null) {
            scanTomlFile.markAnalysisTableSet();
            scanTomlDocumentContent.getTable(ANALYSIS_TABLE)
                    .ifPresent(analysisTable -> loadAnalysis(analysisTable, scanTomlFile, displayPath, errors));
        }
        if (strict) {
            failOnConfigErrors(displayPath, errors);
        }
        return scanTomlFile;
    }

    private static Toml readTomlFile(Path scanTomlFilePath, String displayPath, boolean strict) {
        Toml scanTomlDocumentContent;
        try {
            scanTomlDocumentContent = Toml.read(scanTomlFilePath);
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.READING_CONFIG_FILE, ex.getMessage()));
        }
        if (!strict) {
            return scanTomlDocumentContent;
        }
        List<String> syntaxErrors = scanTomlDocumentContent.diagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR)
                .map(diagnostic -> DiagnosticLog.fileError(displayPath, diagnostic.location().lineRange(),
                        diagnostic.message()))
                .toList();
        failOnConfigErrors(displayPath, syntaxErrors);
        return scanTomlDocumentContent;
    }

    private static void failOnConfigErrors(String displayPath, List<String> errors) {
        if (!errors.isEmpty()) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.CONFIG_FILE_HAS_ERRORS, displayPath,
                    errors.size(), String.valueOf(errors.size())), errors);
        }
    }

    private static void loadGate(Toml gateTable, ScanTomlFile scanTomlFile, String displayPath, List<String> errors) {
        gateTable.rootNode().entries().forEach((key, node) -> {
            if (!key.equals(GATE_FAIL_ON_SEVERITY) && !key.equals(GATE_FAIL_ON_COUNT)) {
                errors.add(DiagnosticLog.fileError(displayPath, node.location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.UNKNOWN_CONFIG_KEY, key, GATE_TABLE,
                                GATE_FAIL_ON_SEVERITY + " or " + GATE_FAIL_ON_COUNT)));
            }
        });

        gateTable.get(GATE_FAIL_ON_SEVERITY).ifPresent(value -> {
            Optional<String> severity = value.kind() == TomlType.STRING
                    ? parseGateSeverity(value.toNativeValue().toString())
                    : Optional.empty();
            if (severity.isPresent()) {
                scanTomlFile.setFailOnSeverity(severity.get());
            } else {
                errors.add(invalidConfigValue(displayPath, value, GATE_FAIL_ON_SEVERITY, GATE_SEVERITY_VALUES));
            }
        });

        gateTable.get(GATE_FAIL_ON_COUNT).ifPresent(value -> {
            if (value.kind() == TomlType.INTEGER && value.toNativeValue() instanceof Number count
                    && count.longValue() >= 1 && count.longValue() <= Integer.MAX_VALUE) {
                scanTomlFile.setFailOnCount(count.intValue());
            } else {
                errors.add(invalidConfigValue(displayPath, value, GATE_FAIL_ON_COUNT, GATE_COUNT_VALUES));
            }
        });
    }

    private static void loadAnalysis(Toml analysisTable, ScanTomlFile scanTomlFile, String displayPath,
                                     List<String> errors) {
        analysisTable.rootNode().entries().forEach((key, node) -> {
            if (!key.equals(ANALYSIS_EXCLUDE_TESTS)) {
                errors.add(DiagnosticLog.fileError(displayPath, node.location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.UNKNOWN_CONFIG_KEY, key, ANALYSIS_TABLE,
                                ANALYSIS_EXCLUDE_TESTS)));
            }
        });
        analysisTable.get(ANALYSIS_EXCLUDE_TESTS).ifPresent(value -> {
            if (value.kind() == TomlType.BOOLEAN) {
                scanTomlFile.setExcludeTests((Boolean) value.toNativeValue());
            } else {
                errors.add(invalidConfigValue(displayPath, value, ANALYSIS_EXCLUDE_TESTS, "true or false"));
            }
        });
    }

    private static String invalidConfigValue(String displayPath, TomlValueNode value, String key, String expected) {
        return DiagnosticLog.fileError(displayPath, value.location().lineRange(),
                DiagnosticLog.error(DiagnosticCode.INVALID_CONFIG_VALUE, String.valueOf(value.toNativeValue()), key,
                        expected));
    }

    // A misspelled key or a quoted "true" would otherwise read as unset and silently turn a requirement off
    private static void validateSuppressionSettings(Toml suppressionTable) {
        List<String> keys = List.of(SUPPRESSION_ENABLED, SUPPRESSION_REQUIRE_REASON, SUPPRESSION_REQUIRE_RULE_ID);
        for (String key : suppressionTable.toMap().keySet()) {
            if (!keys.contains(key)) {
                throw LauncherUtils.createLauncherException(
                        DiagnosticLog.error(DiagnosticCode.UNKNOWN_SUPPRESSION_SETTING, key));
            }
        }
        for (String key : keys) {
            Optional<TomlValueNode> value = suppressionTable.get(key);
            if (value.isPresent() && value.get().kind() != TomlType.BOOLEAN) {
                throw LauncherUtils.createLauncherException(
                        DiagnosticLog.error(DiagnosticCode.INVALID_SUPPRESSION_SETTING, key));
            }
        }
    }

    /**
     * Loads an analyzer to the {@link ScanTomlFile} representation of the Scan.toml
     * file.
     *
     * @param scanTomlFile the in-memory representation of the Scan.toml file
     */
    private static Consumer<Toml> loadAnalyzer(ScanTomlFile scanTomlFile) {
        return analyzerTable -> {
            Map<String, Object> properties = analyzerTable.toMap();
            Object analyzerOrg = properties.remove(ANALYZER_ORG);
            String org = (analyzerOrg instanceof String) ? analyzerOrg.toString() : null;
            Object analyzerName = properties.remove(ANALYZER_NAME);
            String name = (analyzerName instanceof String) ? analyzerName.toString() : null;
            Pattern versionPattern = Pattern.compile(CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN);
            Object analyzerVersion = properties.remove(ANALYZER_VERSION);
            String version = (analyzerVersion instanceof String) && versionPattern.matcher(analyzerVersion.toString())
                    .matches() ? analyzerVersion.toString() : null;
            Object analyzerRepository = properties.remove(ANALYZER_REPOSITORY);
            String repository = (analyzerRepository instanceof String) ? analyzerRepository.toString() : null;

            if (org == null || org.isEmpty() || name == null || name.isEmpty()) {
                return;
            }
            boolean isLocalRepository = repository != null && repository.equals(LOCAL_REPOSITORY_NAME) &&
                    version != null && !version.isEmpty();
            String repo = isLocalRepository ? repository : null;
            ScanTomlFile.Analyzer analyzer = new ScanTomlFile.Analyzer(org, name, version, repo);
            scanTomlFile.setAnalyzer(analyzer);
        };
    }

    /**
     * Loads a platform to the {@link ScanTomlFile} representation of the Scan.toml file, downloading its JAR when the
     * path is a URL. A platform without a name or a path is skipped.
     *
     * @param scanTomlFile the in-memory representation of the Scan.toml file
     * @param targetDir    the target directory
     * @param baseDir      the directory relative paths are resolved against
     * @param outputStream the print stream
     * @param strict       whether a path argument that does not exist fails the load
     * @throws ScanToolException if the platform JAR cannot be loaded
     */
    private static void loadPlatform(Toml platformTable, ScanTomlFile scanTomlFile, Path targetDir, Path baseDir,
                                     PrintStream outputStream, boolean strict) {
        Map<String, Object> properties = platformTable.toMap();
        Object platformName = properties.remove(PLATFORM_NAME);
        Optional<String> name = (platformName instanceof String) ? Optional.of(platformName.toString())
                : Optional.empty();
        Object platformPath = properties.remove(PLATFORM_PATH);
        Optional<String> path = (platformPath instanceof String) ? Optional.of(platformPath.toString())
                : Optional.empty();

        if (name.isEmpty() || name.get().isEmpty() || path.isEmpty()) {
            return;
        }

        // Download remote JAR if the path is a URL and set the path to the downloaded
        // JAR
        String jarPath;
        Optional<Path> localJar = resolveLocalPath(baseDir, path.get());
        if (localJar.isPresent()) {
            jarPath = localJar.get().toString();
        } else {
            try {
                URL url = new URL(path.get());
                jarPath = loadRemoteJAR(targetDir, name.get(), url, outputStream);
            } catch (MalformedURLException ex) {
                throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.PLATFORM_FILE_NOT_FOUND, name.get(),
                        PLATFORM_PATH, path.get()));
            }
        }

        // Arguments named <name>Path, such as sonarProjectPropertiesPath, are files next to the Scan.toml file
        for (Map.Entry<String, Object> argument : properties.entrySet()) {
            if (!argument.getKey().endsWith(PATH_ARGUMENT_SUFFIX) || !(argument.getValue() instanceof String value)) {
                continue;
            }
            Optional<Path> file = resolveLocalPath(baseDir, value);
            if (file.isPresent()) {
                argument.setValue(file.get().toString());
            } else if (strict) {
                throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.PLATFORM_FILE_NOT_FOUND, name.get(),
                        argument.getKey(), value));
            }
        }

        ScanTomlFile.Platform platform = new ScanTomlFile.Platform(name.get(), jarPath, properties);
        scanTomlFile.setPlatform(platform);
    }

    // Returns the absolute path of an existing file, resolving a relative path against the base directory
    private static Optional<Path> resolveLocalPath(Path baseDir, String path) {
        return convertConfigurationPath(path)
                .map(file -> baseDir.resolve(file).toAbsolutePath().normalize())
                .filter(Files::isRegularFile);
    }

    /**
     * Loads rules to the {@link ScanTomlFile} representation of the Scan.toml file.
     *
     * @param scanTomlFile the in-memory representation of the Scan.toml file
     * @param includeRules whether to include/exclude the rules
     */
    private static Consumer<TomlValueNode> loadRules(ScanTomlFile scanTomlFile, boolean includeRules) {
        return rules -> {
            if (!rules.kind().equals(TomlType.ARRAY)) {
                return;
            }
            TomlArrayValueNode rulesArray = (TomlArrayValueNode) rules;
            rulesArray.toNativeValue().stream().map(Object::toString).forEach(rule -> {
                ScanTomlFile.RuleToFilter ruleToFilter = new ScanTomlFile.RuleToFilter(rule);
                if (includeRules) {
                    scanTomlFile.setRuleToInclude(ruleToFilter);
                } else {
                    scanTomlFile.setRuleToExclude(ruleToFilter);
                }
            });
        };
    }


    // Called after validateSuppressionSettings, so a present value is a boolean
    private static boolean booleanSetting(Toml table, String key, boolean defaultValue) {
        return table.get(key).map(value -> (Boolean) value.toNativeValue()).orElse(defaultValue);
    }

    /**
     * Returns the path of the downloaded remote JAR file.
     *
     * @param targetDir     the target directory
     * @param fileName      the remote file name
     * @param remoteJarFile the remote JAR file path
     * @param outputStream  the print stream
     * @return the path of the downloaded remote JAR file
     * @throws ScanToolException if the JAR cannot be downloaded
     */
    private static String loadRemoteJAR(Path targetDir, String fileName, URL remoteJarFile,
                                        PrintStream outputStream) {
        String platformJarFileName = fileName + JAR_PREDICATE;
        Path cachedJarPath = targetDir.resolve(platformJarFileName);
        if (Files.exists(cachedJarPath)) {
            outputStream.println("Loading " + platformJarFileName + " from cache");
            return cachedJarPath.toAbsolutePath().toString();
        }
        try {
            FileUtils.copyURLToFile(remoteJarFile, cachedJarPath.toFile());
            outputStream.println("Downloading remote JAR: " + remoteJarFile);
            return cachedJarPath.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.DOWNLOADING_REMOTE_JAR_FILE,
                    platformJarFileName));
        }
    }

    /**
     * Prints the {@link List<Rule>} of static analysis rules to the console.
     *
     * @param rules        the list of static analysis rules
     * @param outputStream the print stream
     */
    public static void printRulesToConsole(List<Rule> rules, PrintStream outputStream) {
        int maxRuleIDLength = RULE_ID_COLUMN.length();
        int maxKindLength = RULE_KIND_COLUMN.length();
        int maxSeverityLength = RULE_SEVERITY_COLUMN.length();
        int maxDescriptionLength = RULE_DESCRIPTION_COLUMN.length();

        for (Rule rule : rules) {
            maxRuleIDLength = Math.max(maxRuleIDLength, rule.id().length());
            maxKindLength = Math.max(maxKindLength, rule.kind().toString().length());
            maxSeverityLength = Math.max(maxSeverityLength, severityLabel(rule).length());
            maxDescriptionLength = Math.max(maxDescriptionLength, rule.name().length());
        }

        String format = "\t%-" + maxRuleIDLength + "s | %-" + maxKindLength + "s | %-" + maxSeverityLength
                + "s | %-" + maxDescriptionLength + "s%n";

        outputStream.printf(format, RULE_ID_COLUMN, RULE_KIND_COLUMN, RULE_SEVERITY_COLUMN, RULE_DESCRIPTION_COLUMN);
        outputStream.printf("\t%s--%s--%s--%s%n",
                "-".repeat(maxRuleIDLength + 1),
                "-".repeat(maxKindLength + 1),
                "-".repeat(maxSeverityLength + 1),
                "-".repeat(maxDescriptionLength + 1));

        sortRules(rules);
        for (Rule rule : rules) {
            String formattedLine = String.format(format, rule.id(), rule.kind().toString(), severityLabel(rule),
                    rule.name());
            outputStream.println(formattedLine.stripTrailing());
        }
    }

    /**
     * Returns the severity to show for a rule in the rules table, or {@code -} when the rule's
     * metadata does not declare one (e.g. rules from older compiler plugin {@code rules.json} files).
     */
    private static String severityLabel(Rule rule) {
        return rule.severity() != null ? rule.severity().toString() : "-";
    }

    /**
     * <p>
     * Sorts a list of rules based on their priorities. The priorities are
     * determined by the rule's id and a
     * predefined list of priorities. It achieves this with the following steps:
     * </p>
     * <ol>
     * <li>Get the priority of each rule by comparing the rule's id with the
     * predefined list of priorities.</li>
     * <li>Sort the rules based on their priorities.</li>
     * <li>If both rules have the same priority, the one with an exact match in the
     * priority list comes first</li>
     * <li>Otherwise, they are sorted based on the fully qualified rule id.</li>
     * </ol>
     *
     * @param rules The list of rules to be sorted.
     */
    public static void sortRules(List<Rule> rules) {
        List<String> priorities = List.of(RULE_PRIORITY_LIST);
        Comparator<Rule> ruleComparator = (r1, r2) -> {
            String ruleIdPrefix1 = r1.id().split(":")[0];
            String ruleIdPrefix2 = r2.id().split(":")[0];
            AbstractMap.SimpleEntry<Integer, Boolean> priority1 = getPriority(ruleIdPrefix1, priorities);
            AbstractMap.SimpleEntry<Integer, Boolean> priority2 = getPriority(ruleIdPrefix2, priorities);
            int comparison = Integer.compare(priority1.getKey(), priority2.getKey());
            if (comparison != 0) {
                return comparison;
            }
            comparison = Boolean.compare(priority2.getValue(), priority1.getValue());
            if (comparison != 0) {
                return comparison;
            }
            comparison = ruleIdPrefix1.compareTo(ruleIdPrefix2);
            if (comparison != 0) {
                return comparison;
            }
            return Integer.compare(r1.numericId(), r2.numericId());
        };
        rules.sort(ruleComparator);
    }

    /**
     * Returns the priority of a rule as a pair of the priority index and whether
     * the rule is a direct match.
     *
     * @param ruleId     the rule id
     * @param priorities the list of priorities
     * @return the priority of the rule as a pair of the priority index and whether
     * the rule is a direct match.
     */
    private static AbstractMap.SimpleEntry<Integer, Boolean> getPriority(String ruleId, List<String> priorities) {
        for (int i = 0; i < priorities.size(); i++) {
            if (ruleId.equals(priorities.get(i))) {
                return new AbstractMap.SimpleEntry<>(i, true);
            }
            if (ruleId.split("/")[0].equals(priorities.get(i))) {
                return new AbstractMap.SimpleEntry<>(i, false);
            }
        }
        return new AbstractMap.SimpleEntry<>(priorities.size(), false);
    }

}
