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
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.projects.util.ProjectConstants.USER_DIR_PROPERTY;
import static io.ballerina.scan.utils.Constants.ANALYSIS_EXCLUDE_TESTS;
import static io.ballerina.scan.utils.Constants.ANALYSIS_TABLE;
import static io.ballerina.scan.utils.Constants.ANALYZER_NAME;
import static io.ballerina.scan.utils.Constants.ANALYZER_ORG;
import static io.ballerina.scan.utils.Constants.ANALYZER_REPOSITORY;
import static io.ballerina.scan.utils.Constants.ANALYZER_TABLE;
import static io.ballerina.scan.utils.Constants.ANALYZER_VERSION;
import static io.ballerina.scan.utils.Constants.CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN;
import static io.ballerina.scan.utils.Constants.EXCLUDE_KEY;
import static io.ballerina.scan.utils.Constants.GATE_FAIL_ON_COUNT;
import static io.ballerina.scan.utils.Constants.GATE_FAIL_ON_SEVERITY;
import static io.ballerina.scan.utils.Constants.GATE_TABLE;
import static io.ballerina.scan.utils.Constants.INCLUDE_KEY;
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
import static io.ballerina.scan.utils.Constants.RULES_TABLE;
import static io.ballerina.scan.utils.Constants.SARIF_SCHEMA;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_NAME;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_ORGANIZATION;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_URI;
import static io.ballerina.scan.utils.Constants.SARIF_TOOL_VERSION;
import static io.ballerina.scan.utils.Constants.SARIF_VERSION;
import static io.ballerina.scan.utils.Constants.SCAN_FILE;
import static io.ballerina.scan.utils.Constants.SCAN_FILE_FIELD;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_CONTENT;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_PACKAGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_PATH;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_RELATIVE_PATH;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_KIND;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_MESSAGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_RULE_ID;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_SEVERITY;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_SUPPRESSION;
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
import static java.util.Locale.ROOT;

/**
 * {@code ScanUtils} contains all the utility functions used by the scan tool.
 *
 * @since 0.1.0
 */
public final class ScanUtils {
    /**
     * The values a gate count can take, as described in messages.
     *
     * @since 0.11.1
     */
    public static final String GATE_COUNT_VALUES = "a whole number of 1 or more";

    private static final String RULE_LIST_VALUES = "an array of rule IDs, such as [\"ballerina:1\"]";

    private ScanUtils() {
    }

    /**
     * Parses a gate count.
     *
     * @param value the count as given on the command line
     * @return the count, or empty if the value is not a whole number of 1 or more
     * @since 0.11.1
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
     * Prints issues generated via static code analyzers to the console.
     *
     * @param issues       generated issues
     * @param outputStream print stream
     * @deprecated the leading blank line it prints does not fit the console layout of the scan; print
     * {@link #convertIssuesToJsonString(List)} instead
     */
    @Deprecated(since = "0.11.1")
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
     * @deprecated the leading blank line it prints does not fit the console layout of the scan; print
     * {@link #convertIssuesToJsonString(List)} or {@link #convertIssuesToSarifString(List, Project)} instead
     */
    @Deprecated(since = "0.11.1")
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
     * Fails when the dependencies of a package are not locked in its Dependencies.toml, as {@code bal build} locks
     * them. A single file has no dependencies to lock.
     *
     * @param project the project to scan
     * @throws ScanToolException if the project is a package without a Dependencies.toml
     * @since 0.11.1
     */
    public static void requireLockedDependencies(Project project) {
        if (project.kind() != ProjectKind.BUILD_PROJECT) {
            return;
        }
        Package currentPackage = project.currentPackage();
        // An empty package has no dependencies to lock, and is reported as empty instead
        if (!ProjectUtils.isProjectEmpty(project) && currentPackage.dependenciesToml().isEmpty()) {
            String packageName = currentPackage.packageOrg().value() + "/" + currentPackage.packageName().value();
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.MISSING_DEPENDENCIES_TOML, packageName));
        }
    }

    /**
     * Returns the json {@link String} array of generated issues, where each suppressed issue carries a
     * {@code suppressions} entry mirroring the SARIF suppression object.
     *
     * @param issues       generated issues, including suppressed ones
     * @param suppressions the suppressed issues, keyed by the issue they suppress
     * @return json string array of generated issues
     */
    private static String convertIssuesToJsonString(List<Issue> issues, Map<Issue, SuppressedIssue> suppressions) {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        Map<String, String> fileContentCache = new HashMap<>();
        for (int i = 0; i < issues.size(); i++) {
            Issue issue = issues.get(i);
            JsonObject issueAsJson = issuesAsJson.get(i).getAsJsonObject();
            if (issue instanceof IssueImpl issueImpl) {
                String snippetText = SnippetExtractor.extract(issueImpl.filePath(),
                        issueImpl.location().textRange(), fileContentCache);
                if (snippetText != null) {
                    issueAsJson.getAsJsonObject("location").addProperty("snippet", snippetText);
                }
            }
            SuppressedIssue suppressedIssue = suppressions.get(issue);
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
     */
    private static String convertIssuesToSarifString(List<Issue> issues, Project project,
                                                     Map<Issue, SuppressedIssue> suppressions) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
        driver.addProperty("informationUri", SARIF_TOOL_URI + SARIF_TOOL_VERSION);

        // Create rules array for the tool
        JsonArray rules = new JsonArray();
        Map<String, JsonObject> ruleMap = new HashMap<>();
        Set<String> addedRuleIds = new HashSet<>();

        // Collect unique rules from issues
        for (Issue issue : issues) {
            IssueImpl issueImpl = (IssueImpl) issue;
            String ruleId = issueImpl.rule().id();

            JsonObject ruleObject = ruleMap.computeIfAbsent(ruleId, id -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", id);

                // Construct helpUri based on rule ID and description
                String helpUri = constructHelpUri(id, issueImpl.rule().description());
                obj.addProperty("helpUri", helpUri);

                JsonObject shortDescription = new JsonObject();
                shortDescription.addProperty("text", issueImpl.rule().description());
                obj.add("shortDescription", shortDescription);

                String level = mapRuleKindToSarifLevel(issueImpl.rule().kind());
                JsonObject defaultConfiguration = new JsonObject();
                defaultConfiguration.addProperty("level", level);
                obj.add("defaultConfiguration", defaultConfiguration);

                return obj;
            });
            if (addedRuleIds.add(ruleId)) {
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
            JsonObject result = new JsonObject();

            result.addProperty("ruleId", issueImpl.rule().id());
            result.addProperty("level", mapRuleKindToSarifLevel(issueImpl.rule().kind()));

            JsonObject message = new JsonObject();
            message.addProperty("text", issueImpl.rule().description());
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
     * Constructs the helpUri based on rule ID and description.
     *
     * @param ruleId      the rule ID
     * @param description the rule description
     * @return the constructed helpUri
     */
    private static String constructHelpUri(String ruleId, String description) {
        String baseUri = SARIF_TOOL_URI + SARIF_TOOL_VERSION;
        String anchor;
        String idPart = ruleId.replace(":", "").replace("/", "");
        String descPart = description.toLowerCase(ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-$", "")
                .replaceAll("^-", "");
        anchor = "#" + idPart + "---" + descPart;
        return baseUri + anchor;
    }

    /**
     * Maps RuleKind to SARIF level.
     *
     * @param ruleKind the rule kind
     * @return corresponding SARIF level
     */
    private static String mapRuleKindToSarifLevel(RuleKind ruleKind) {
        return switch (ruleKind) {
            case BUG -> "error";
            case CODE_SMELL -> "note";
            case VULNERABILITY -> "warning";
        };
    }

    /**
     * Returns the relative path from the source root.
     *
     * @param filePath   the absolute file path
     * @param sourceRoot the source root path
     * @return relative path
     */
    private static String getRelativePath(String filePath, String sourceRoot) {
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
     * @since 0.11.1
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
     * @since 0.11.1
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
    private static Target getTargetPath(Project project, String directoryName) {
        Path targetDirectory = project.targetDir();
        try {
            if (directoryName == null) {
                return new Target(project.targetDir());
            }
            Path parentDirectory = project.sourceRoot();
            if (parentDirectory == null) {
                return new Target(project.targetDir());
            }
            targetDirectory = parentDirectory.resolve(directoryName);
            return new Target(Files.createDirectories(targetDirectory));
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.FAILED_TO_CREATE_TARGET_DIR,
                    targetDirectory, ex.getMessage()));
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
     * @since 0.11.1
     */
    public static Path generateScanReport(List<Issue> issues, Project project, String directoryName,
                                          Map<Issue, SuppressedIssue> suppressions) {
        return generateScanReport(issues, project, directoryName, suppressions, AnalyzedSources.of(false));
    }

    /**
     * Generates the HTML report, listing every analyzed source of the package, with or without issues.
     *
     * @param analyzedSources the sources of the package that were scanned
     */
    public static Path generateScanReport(List<Issue> issues, Project project, String directoryName,
                                          Map<Issue, SuppressedIssue> suppressions,
                                          AnalyzedSources analyzedSources) {
        JsonObject scannedProject = new JsonObject();
        Package currentPackage = project.currentPackage();
        String packageName = currentPackage.packageName().toString();
        scannedProject.addProperty(SCAN_REPORT_PROJECT_NAME, packageName);
        scannedProject.addProperty(SCAN_REPORT_PROJECT_KIND, project.kind().name());
        if (project.kind() == ProjectKind.BUILD_PROJECT) {
            scannedProject.addProperty(SCAN_REPORT_PROJECT_VERSION, currentPackage.packageVersion().toString());
        }

        Path reportRoot = project.sourceRoot().toAbsolutePath().normalize();
        JsonObject scannedPackage = new JsonObject();
        scannedPackage.addProperty(SCAN_REPORT_PACKAGE_ORG, currentPackage.packageOrg().toString());
        scannedPackage.addProperty(SCAN_REPORT_PACKAGE_NAME, packageName);
        scannedPackage.addProperty(SCAN_REPORT_PACKAGE_VERSION, currentPackage.packageVersion().toString());
        scannedPackage.addProperty(SCAN_REPORT_PACKAGE_PATH, toRelativePath(reportRoot, project.sourceRoot()));
        JsonArray scannedPackages = new JsonArray();
        scannedPackages.add(scannedPackage);
        scannedProject.add(SCAN_REPORT_PACKAGES, scannedPackages);

        Map<String, JsonObject> scanReportPathAndFile = new LinkedHashMap<>();
        for (Document document : getAnalyzedDocuments(currentPackage, analyzedSources)) {
            // Documents that exist only in memory have no file to show; issues in them still get an entry below
            Optional<Path> documentPath = project.documentPath(document.documentId());
            if (documentPath.isEmpty()) {
                continue;
            }
            JsonObject scanReportFile = createScanReportFile(document.name(), documentPath.get().toString());
            String relativePath = toRelativePath(reportRoot, documentPath.get());
            if (relativePath != null) {
                scanReportFile.addProperty(SCAN_REPORT_FILE_RELATIVE_PATH, relativePath);
            }
            scanReportFile.addProperty(SCAN_REPORT_FILE_PACKAGE, packageName);
            scanReportPathAndFile.put(toReportFileKey(documentPath.get().toString()), scanReportFile);
        }
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
     * Returns the {@link JsonObject} representation of a scanned source file, including its content.
     *
     * @param fileName name of the source file
     * @param filePath path of the source file
     * @return json object representation of the scanned source file
     */
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

    private static List<Document> getAnalyzedDocuments(Package analyzedPackage, AnalyzedSources analyzedSources) {
        List<Document> documents = new ArrayList<>();
        for (ModuleId moduleId : analyzedPackage.moduleIds()) {
            Module module = analyzedPackage.module(moduleId);
            analyzedSources.documentIds(module)
                    .map(module::document)
                    // Skip the in-memory document the analyzer adds to import external analyzer plugins
                    .filter(document -> !document.name().startsWith(Constants.IMPORT_GENERATOR_FILE))
                    .forEach(documents::add);
        }
        return documents;
    }

    /**
     * Returns the path relative to the root with forward slashes, so the report builds the same folder tree on any
     * OS, or null when the path is outside the root.
     */
    private static String toRelativePath(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(normalizedRoot)) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        normalizedRoot.relativize(normalizedPath).forEach(segment -> segments.add(segment.toString()));
        return String.join("/", segments);
    }

    private static String toReportFileKey(String filePath) {
        return Path.of(filePath).toAbsolutePath().normalize().toString();
    }

    /**
     * Returns the severity the report shows for a rule. Rules have no severity of their own on this line, so it is
     * the severity the quality gate counts the rule's kind as.
     */
    private static String reportSeverity(RuleKind kind) {
        return switch (kind) {
            case BUG -> "HIGH";
            case VULNERABILITY -> "MEDIUM";
            case CODE_SMELL -> "LOW";
        };
    }

    private static JsonObject getJsonIssue(IssueImpl issueImpl, SuppressedIssue suppressedIssue) {
        JsonObject scanReportIssue = new JsonObject();
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_RULE_ID, issueImpl.rule().id());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_KIND, issueImpl.rule().kind().toString());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_SEVERITY, reportSeverity(issueImpl.rule().kind()));
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_TYPE, issueImpl.source().toString());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_MESSAGE, issueImpl.rule().description());

        JsonObject scanReportIssueTextRange = new JsonObject();
        LineRange lineRange = issueImpl.location().lineRange();
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE, lineRange.startLine().line());
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE_OFFSET, lineRange.startLine()
                .offset());
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE, lineRange.endLine().line());
        scanReportIssueTextRange.addProperty(SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE_OFFSET, lineRange.endLine()
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
     * Returns the {@link Optional<ScanTomlFile>} representation of a local/remote Scan.toml file. TOML syntax errors
     * and invalid settings are ignored, as only {@code bal scan} rejects them.
     *
     * @param project      Ballerina project
     * @param outputStream print stream
     * @return the optional in-memory representation of the Scan.toml file, which is empty after printing the error
     * when the configurations cannot be loaded
     */
    public static Optional<ScanTomlFile> loadScanTomlConfigurations(Project project, PrintStream outputStream) {
        try {
            return Optional.of(readScanTomlConfigurations(project, outputStream, Validation.NONE));
        } catch (ScanToolException ex) {
            outputStream.println(ex.getMessage());
            ex.diagnostics().forEach(outputStream::println);
            return Optional.empty();
        }
    }

    /**
     * Returns the {@link ScanTomlFile} representation of a local/remote Scan.toml file.
     *
     * @param project      Ballerina project
     * @param outputStream print stream for progress messages
     * @return the in-memory representation of the Scan.toml file, which is empty when the project has none
     * @throws ScanToolException if the configurations cannot be loaded, with a diagnostic for each error found in
     *                           the file
     * @since 0.11.1
     */
    public static ScanTomlFile readScanTomlConfigurations(Project project, PrintStream outputStream) {
        return readScanTomlConfigurations(project, outputStream, false);
    }

    /**
     * Returns the {@link ScanTomlFile} representation of a local/remote Scan.toml file.
     *
     * @param project      Ballerina project
     * @param outputStream print stream for progress messages
     * @param rulesOnly    whether only the errors that change the rules of the package fail the load: TOML syntax
     *                     errors and an invalid {@code [[analyzer]]} table
     * @return the in-memory representation of the Scan.toml file, which is empty when the project has none
     * @throws ScanToolException if the configurations cannot be loaded, with a diagnostic for each error found in
     *                           the file
     * @since 0.11.1
     */
    public static ScanTomlFile readScanTomlConfigurations(Project project, PrintStream outputStream,
                                                          boolean rulesOnly) {
        return readScanTomlConfigurations(project, outputStream, Validation.of(rulesOnly));
    }

    /**
     * Reads the Scan.toml file of a package, failing the load on the errors the validation covers.
     */
    private static ScanTomlFile readScanTomlConfigurations(Project project, PrintStream outputStream,
                                                           Validation validation) {
        if (!project.kind().equals(ProjectKind.BUILD_PROJECT) || project.currentPackage().ballerinaToml().isEmpty()) {
            return new ScanTomlFile();
        }

        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().get();
        TomlDocument ballerinaTomlDocument = ballerinaToml.tomlDocument();
        Toml ballerinaTomlDocumentContent = ballerinaTomlDocument.toml();

        Path targetDir = project.targetDir();
        if (ballerinaTomlDocumentContent.getTable(SCAN_TABLE).isEmpty()) {
            Path userDir = Path.of(System.getProperty(USER_DIR_PROPERTY));
            Path scanTomlFilePath = userDir.resolve(SCAN_FILE);
            if (Files.isRegularFile(scanTomlFilePath)) {
                outputStream.println("Loading scan tool configurations from " + scanTomlFilePath);
                return loadScanFile(targetDir, scanTomlFilePath, displayPath(userDir, scanTomlFilePath),
                        userDir.toAbsolutePath(), validation);
            }
            return new ScanTomlFile();
        }

        Toml scanTable = ballerinaTomlDocumentContent.getTable(SCAN_TABLE).get();
        if (scanTable.get(SCAN_FILE_FIELD).isEmpty()) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.MISSING_CONFIG_FIELD));
        }

        TomlValueNode configPath = scanTable.get(SCAN_FILE_FIELD).get();
        String scanTomlPath = configPath.toNativeValue().toString();
        Optional<Path> path = convertConfigurationPath(scanTomlPath);
        Optional<Path> absPath = path.isEmpty() || path.get().isAbsolute() ? path
                : Optional.of(project.sourceRoot().resolve(scanTomlPath));

        if (absPath.isPresent() && absPath.get().toFile().isFile()) {
            outputStream.println("Loading scan tool configurations from " + absPath.get());
            return loadScanFile(targetDir, absPath.get(), displayPath(project.sourceRoot(), absPath.get()),
                    absPath.get().toAbsolutePath().getParent(), validation);
        }
        // A remote file has no folder of its own, so its relative paths are resolved against the package root
        return loadRemoteScanFile(targetDir, scanTomlPath, project.sourceRoot().toAbsolutePath(), outputStream,
                validation);
    }

    /**
     * Returns the path of a file as shown to the user: relative to a root, with forward slashes on every OS, or as
     * given when the file is outside the root.
     *
     * @param root the directory the path is shown relative to
     * @param file the file
     * @return the path as shown to the user
     * @since 0.11.1
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
     * @param validation   the errors that fail the load
     * @return the in-memory representation of the remote Scan.toml file
     * @throws ScanToolException if the file cannot be downloaded or has errors
     */
    private static ScanTomlFile loadRemoteScanFile(Path targetDir, String scanTomlPath, Path baseDir,
                                                   PrintStream outputStream, Validation validation) {
        // Keyed by URL, so that a file cached for another configPath is not reused
        Path cachePath = targetDir.resolve(Integer.toHexString(scanTomlPath.hashCode()) + "-" + SCAN_FILE);

        if (Files.exists(cachePath)) {
            outputStream.println("Loading scan tool configurations from cache");
            ScanTomlFile scanTomlFile = loadScanFile(targetDir, cachePath, scanTomlPath, baseDir, validation);
            scanTomlFile.setRemoteLocation(scanTomlPath);
            return scanTomlFile;
        }

        URL remoteScanTomlFilePath;
        try {
            remoteScanTomlFilePath = toUrl(scanTomlPath);
            FileUtils.copyURLToFile(remoteScanTomlFilePath, cachePath.toFile());
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_CONFIG_FILE,
                    ex.getMessage()));
        }
        outputStream.println("Loading scan tool configurations from " + remoteScanTomlFilePath);
        ScanTomlFile scanTomlFile = loadScanFile(targetDir, cachePath, scanTomlPath, baseDir, validation);
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
     * @param validation       the errors that fail the load
     * @return the in-memory representation of the Scan.toml file
     * @throws ScanToolException if the file cannot be read or has errors the validation covers
     */
    private static ScanTomlFile loadScanFile(Path targetDir, Path scanTomlFilePath, String displayPath, Path baseDir,
                                             Validation validation) {
        Toml scanTomlDocumentContent = readTomlFile(scanTomlFilePath, displayPath, validation);
        ScanTomlFile scanTomlFile = new ScanTomlFile();
        scanTomlFile.setPath(scanTomlFilePath);
        Map<String, TopLevelNode> entries = scanTomlDocumentContent.rootNode().entries();
        List<String> errors = new ArrayList<>();
        List<String> analyzerErrors = new ArrayList<>();

        for (Toml platformTable : scanTomlDocumentContent.getTables(PLATFORM_TABLE)) {
            loadPlatform(platformTable, scanTomlFile, baseDir);
        }
        TopLevelNode analyzerNode = entries.get(ANALYZER_TABLE);
        if (analyzerNode instanceof TomlTableArrayNode) {
            scanTomlDocumentContent.getTables(ANALYZER_TABLE).forEach(loadAnalyzer(scanTomlFile));
        } else if (analyzerNode != null && !(analyzerNode instanceof TomlKeyValueNode keyValue
                && keyValue.value() instanceof TomlArrayValueNode array && array.elements().isEmpty())) {
            String error = DiagnosticLog.fileError(displayPath, analyzerNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_ANALYZER_LIST));
            errors.add(error);
            analyzerErrors.add(error);
        }
        TopLevelNode ruleNode = entries.get(RULES_TABLE);
        if (ruleNode != null && ruleNode.kind() != TomlType.TABLE) {
            errors.add(DiagnosticLog.fileError(displayPath, ruleNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_CONFIG_TABLE, RULES_TABLE)));
        } else {
            scanTomlDocumentContent.getTable(RULES_TABLE).ifPresent(ruleTable -> {
                loadRuleTable(ruleTable, scanTomlFile, displayPath, errors);
                if (!scanTomlFile.getRulesToInclude().isEmpty() && !scanTomlFile.getRulesToExclude().isEmpty()) {
                    errors.add(DiagnosticLog.fileError(displayPath, ruleNode.location().lineRange(),
                            DiagnosticLog.error(DiagnosticCode.ATTEMPT_TO_INCLUDE_AND_EXCLUDE)));
                }
            });
        }
        // Written in any other form, such as [[suppression]], the settings would not be read at all
        TopLevelNode suppressionNode = entries.get(SUPPRESSION_TABLE);
        if (suppressionNode != null && suppressionNode.kind() != TomlType.TABLE) {
            errors.add(DiagnosticLog.fileError(displayPath, suppressionNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_SUPPRESSION_TABLE)));
        } else {
            scanTomlDocumentContent.getTable(SUPPRESSION_TABLE)
                    .ifPresent(table -> loadSuppression(table, scanTomlFile, displayPath, errors));
        }

        TopLevelNode gateNode = entries.get(GATE_TABLE);
        if (gateNode != null && gateNode.kind() != TomlType.TABLE) {
            errors.add(DiagnosticLog.fileError(displayPath, gateNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_CONFIG_TABLE, GATE_TABLE)));
        } else {
            scanTomlDocumentContent.getTable(GATE_TABLE)
                    .ifPresent(gateTable -> loadGate(gateTable, scanTomlFile, displayPath, errors));
        }
        TopLevelNode analysisNode = entries.get(ANALYSIS_TABLE);
        if (analysisNode != null && analysisNode.kind() != TomlType.TABLE) {
            errors.add(DiagnosticLog.fileError(displayPath, analysisNode.location().lineRange(),
                    DiagnosticLog.error(DiagnosticCode.INVALID_CONFIG_TABLE, ANALYSIS_TABLE)));
        } else if (analysisNode != null) {
            scanTomlDocumentContent.getTable(ANALYSIS_TABLE)
                    .ifPresent(analysisTable -> loadAnalysis(analysisTable, scanTomlFile, displayPath, errors));
        }
        switch (validation) {
            case ALL -> failOnConfigErrors(displayPath, errors);
            case RULES -> failOnConfigErrors(displayPath, analyzerErrors);
            case NONE -> { }
        }
        return scanTomlFile;
    }

    /**
     * Parses a Scan.toml file. Unless the validation is NONE, a syntax error fails the load with its position.
     */
    private static Toml readTomlFile(Path scanTomlFilePath, String displayPath, Validation validation) {
        Toml scanTomlDocumentContent;
        try {
            scanTomlDocumentContent = Toml.read(scanTomlFilePath);
        } catch (IOException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.READING_CONFIG_FILE, ex.getMessage()));
        }
        if (validation == Validation.NONE) {
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

    /**
     * Fails the load with every error found in the file, so that all of them are reported at once.
     */
    private static void failOnConfigErrors(String displayPath, List<String> errors) {
        if (!errors.isEmpty()) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.CONFIG_FILE_HAS_ERRORS, displayPath,
                    errors.size()), errors);
        }
    }

    /**
     * Loads the [gate] table, adding an error for an unknown key or an invalid value.
     */
    private static void loadGate(Toml gateTable, ScanTomlFile scanTomlFile, String displayPath, List<String> errors) {
        gateTable.rootNode().entries().forEach((key, node) -> {
            if (!key.equals(GATE_FAIL_ON_SEVERITY) && !key.equals(GATE_FAIL_ON_COUNT)) {
                errors.add(DiagnosticLog.fileError(displayPath, node.location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.UNKNOWN_CONFIG_KEY, key, GATE_TABLE,
                                GATE_FAIL_ON_SEVERITY + " or " + GATE_FAIL_ON_COUNT)));
            }
        });

        gateTable.get(GATE_FAIL_ON_SEVERITY).ifPresent(value -> {
            Optional<GateSeverity> severity = value.kind() == TomlType.STRING
                    ? GateSeverity.parse(value.toNativeValue().toString())
                    : Optional.empty();
            if (severity.isPresent()) {
                scanTomlFile.setFailOnSeverity(severity.get());
            } else {
                errors.add(invalidConfigValue(displayPath, value, GATE_FAIL_ON_SEVERITY, GateSeverity.VALUES));
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

    /**
     * Loads the [analysis] table, adding an error for an unknown key or an invalid value.
     */
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
                scanTomlFile.writeExcludeTests((Boolean) value.toNativeValue());
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

    /**
     * Loads the [suppression] table, adding an error for an unknown key or a value that is not a boolean, since either
     * would otherwise read as unset and silently turn a requirement off.
     */
    private static void loadSuppression(Toml suppressionTable, ScanTomlFile scanTomlFile, String displayPath,
                                        List<String> errors) {
        List<String> keys = List.of(SUPPRESSION_ENABLED, SUPPRESSION_REQUIRE_REASON, SUPPRESSION_REQUIRE_RULE_ID);
        suppressionTable.rootNode().entries().forEach((key, node) -> {
            if (!keys.contains(key)) {
                errors.add(DiagnosticLog.fileError(displayPath, node.location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.UNKNOWN_SUPPRESSION_SETTING, key)));
            }
        });
        Map<String, Consumer<Boolean>> setters = Map.of(
                SUPPRESSION_ENABLED, scanTomlFile::setSuppressionEnabled,
                SUPPRESSION_REQUIRE_REASON, scanTomlFile::setSuppressionReasonRequired,
                SUPPRESSION_REQUIRE_RULE_ID, scanTomlFile::setSuppressionRuleIdRequired);
        for (String key : keys) {
            suppressionTable.get(key).ifPresent(value -> {
                if (value.kind() == TomlType.BOOLEAN) {
                    setters.get(key).accept((Boolean) value.toNativeValue());
                } else {
                    errors.add(DiagnosticLog.fileError(displayPath, value.location().lineRange(),
                            DiagnosticLog.error(DiagnosticCode.INVALID_SUPPRESSION_SETTING, key)));
                }
            });
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
     * Loads a platform to the {@link ScanTomlFile} representation of the Scan.toml file. A platform without a name or
     * a path is skipped.
     *
     * @param scanTomlFile the in-memory representation of the Scan.toml file
     * @param baseDir      the directory relative paths are resolved against
     */
    private static void loadPlatform(Toml platformTable, ScanTomlFile scanTomlFile, Path baseDir) {
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

        // The JAR is checked, or downloaded, only when the platform is selected, so that a platform JAR that exists
        // only on CI does not fail other scans
        String jarPath = resolveLocalPath(baseDir, path.get())
                .or(() -> isUrl(path.get()) ? Optional.empty()
                        : convertConfigurationPath(path.get()).map(file -> baseDir.resolve(file).normalize()))
                .map(Path::toString)
                .orElse(path.get());

        // An argument named <name>Path that names an existing file next to the Scan.toml file is passed as an absolute
        // path. Other values, such as URLs or files the plugin creates, belong to the plugin and are passed unchanged
        for (Map.Entry<String, Object> argument : properties.entrySet()) {
            if (argument.getKey().endsWith(PATH_ARGUMENT_SUFFIX) && argument.getValue() instanceof String value) {
                resolveLocalPath(baseDir, value).ifPresent(file -> argument.setValue(file.toString()));
            }
        }

        ScanTomlFile.Platform platform = new ScanTomlFile.Platform(name.get(), jarPath, properties);
        scanTomlFile.setPlatform(platform);
    }

    /**
     * Returns whether a path is written as a URL.
     */
    private static boolean isUrl(String path) {
        try {
            toUrl(path);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    // Reports relative or unparsable paths as malformed, matching the deprecated URL(String) constructor
    private static URL toUrl(String path) throws MalformedURLException {
        try {
            URI uri = new URI(path);
            if (!uri.isAbsolute()) {
                throw new MalformedURLException("no protocol: " + path);
            }
            return uri.toURL();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            MalformedURLException malformed = new MalformedURLException(ex.getMessage());
            malformed.initCause(ex);
            throw malformed;
        }
    }

    // Returns the absolute path of an existing file, resolving a relative path against the base directory
    private static Optional<Path> resolveLocalPath(Path baseDir, String path) {
        return convertConfigurationPath(path)
                .map(file -> baseDir.resolve(file).toAbsolutePath().normalize())
                .filter(Files::isRegularFile);
    }

    /**
     * Loads the rules of the [rule] table to the {@link ScanTomlFile} representation of the Scan.toml file, adding an
     * error for an unknown key or a value that is not an array of rule IDs.
     *
     * @param ruleTable    the [rule] table
     * @param scanTomlFile the in-memory representation of the Scan.toml file
     * @param displayPath  the path of the file as shown to the user
     * @param errors       the errors found in the file
     */
    private static void loadRuleTable(Toml ruleTable, ScanTomlFile scanTomlFile, String displayPath,
                                      List<String> errors) {
        ruleTable.rootNode().entries().forEach((key, node) -> {
            if (!key.equals(INCLUDE_KEY) && !key.equals(EXCLUDE_KEY)) {
                errors.add(DiagnosticLog.fileError(displayPath, node.location().lineRange(),
                        DiagnosticLog.error(DiagnosticCode.UNKNOWN_CONFIG_KEY, key, RULES_TABLE,
                                INCLUDE_KEY + " or " + EXCLUDE_KEY)));
            }
        });
        ruleTable.get(INCLUDE_KEY).ifPresent(rules -> loadRules(rules, scanTomlFile, true, displayPath, errors));
        ruleTable.get(EXCLUDE_KEY).ifPresent(rules -> loadRules(rules, scanTomlFile, false, displayPath, errors));
    }

    /**
     * Loads the rule IDs of include or exclude, adding an error when the value is not an array of strings.
     */
    private static void loadRules(TomlValueNode rules, ScanTomlFile scanTomlFile, boolean includeRules,
                                  String displayPath, List<String> errors) {
        String key = includeRules ? INCLUDE_KEY : EXCLUDE_KEY;
        if (!(rules instanceof TomlArrayValueNode rulesArray)
                || !rulesArray.elements().stream().allMatch(rule -> rule.kind() == TomlType.STRING)) {
            errors.add(invalidConfigValue(displayPath, rules, key, RULE_LIST_VALUES));
            return;
        }
        List<String> ruleIds = rulesArray.toNativeValue().stream().map(Object::toString).toList();
        ruleIds.forEach(rule -> {
            ScanTomlFile.RuleToFilter ruleToFilter = new ScanTomlFile.RuleToFilter(rule);
            if (includeRules) {
                scanTomlFile.setRuleToInclude(ruleToFilter);
            } else {
                scanTomlFile.setRuleToExclude(ruleToFilter);
            }
        });
        scanTomlFile.writeRules(includeRules, ruleIds);
    }

    /**
     * Returns the local JAR of a platform, downloading it into the target directory first when its path is a URL. A
     * downloaded JAR is cached and reused.
     *
     * @param platform     the platform
     * @param targetDir    the target directory the remote JAR is cached in
     * @param outputStream the print stream for progress messages
     * @return the absolute path of the local JAR
     * @throws ScanToolException if the path is neither a file nor a URL, or the JAR cannot be downloaded
     * @since 0.11.1
     */
    public static Path resolvePlatformJar(ScanTomlFile.Platform platform, Path targetDir, PrintStream outputStream) {
        Optional<Path> localJar = convertConfigurationPath(platform.path()).filter(Files::isRegularFile);
        if (localJar.isPresent()) {
            return localJar.get().toAbsolutePath();
        }
        try {
            return Path.of(loadRemoteJAR(targetDir, platform.name(), toUrl(platform.path()), outputStream));
        } catch (MalformedURLException ex) {
            throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.PLATFORM_FILE_NOT_FOUND, platform.name(),
                    PLATFORM_PATH, platform.path()));
        }
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
        int maxSeverityLength = RULE_KIND_COLUMN.length();
        int maxDescriptionLength = RULE_DESCRIPTION_COLUMN.length();

        for (Rule rule : rules) {
            maxRuleIDLength = Math.max(maxRuleIDLength, rule.id().length());
            maxSeverityLength = Math.max(maxSeverityLength, rule.kind().toString().length());
            maxDescriptionLength = Math.max(maxDescriptionLength, rule.description().length());
        }

        String format = "\t%-" + maxRuleIDLength + "s | %-" + maxSeverityLength + "s | %-" + maxDescriptionLength
                + "s%n";

        outputStream.printf(format, RULE_ID_COLUMN, RULE_KIND_COLUMN, RULE_DESCRIPTION_COLUMN);
        outputStream.printf("\t%s--%s--%s%n",
                "-".repeat(maxRuleIDLength + 1),
                "-".repeat(maxSeverityLength + 1),
                "-".repeat(maxDescriptionLength + 1));

        sortRules(rules);
        for (Rule rule : rules) {
            String formattedLine = String.format(format, rule.id(), rule.kind().toString(), rule.description());
            outputStream.println(formattedLine.stripTrailing());
        }
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

    /**
     * The errors in a Scan.toml file that fail its load.
     */
    private enum Validation {
        ALL,
        // TOML syntax errors and the errors that change the rules of a package, for listing the rules
        RULES,
        NONE;

        static Validation of(boolean rulesOnly) {
            return rulesOnly ? RULES : ALL;
        }
    }
}
