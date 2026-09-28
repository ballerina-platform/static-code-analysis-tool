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
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.scan.Issue;
import io.ballerina.scan.OwaspCoverage;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Severity;
import io.ballerina.scan.Standards;
import io.ballerina.scan.internal.IssueImpl;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.TomlType;
import io.ballerina.toml.semantic.ast.TomlArrayValueNode;
import io.ballerina.toml.semantic.ast.TomlValueNode;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.scan.utils.Constants.ANALYZER_NAME;
import static io.ballerina.scan.utils.Constants.ANALYZER_ORG;
import static io.ballerina.scan.utils.Constants.ANALYZER_REPOSITORY;
import static io.ballerina.scan.utils.Constants.ANALYZER_TABLE;
import static io.ballerina.scan.utils.Constants.ANALYZER_VERSION;
import static io.ballerina.scan.utils.Constants.CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN;
import static io.ballerina.scan.utils.Constants.JAR_PREDICATE;
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
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_PATH;
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
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TAGS;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE_OFFSET;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE_OFFSET;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TYPE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PROJECT_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_SCANNED_FILES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ZIP_FILE;
import static io.ballerina.scan.utils.Constants.SCAN_TABLE;

/**
 * {@code ScanUtils} contains all the utility functions used by the scan tool.
 *
 * @since 0.1.0
 */
public final class ScanUtils {
    private ScanUtils() {
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
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();

        Map<String, String> fileContentCache = new HashMap<>();
        for (int i = 0; i < issues.size(); i++) {
            IssueImpl issueImpl = (IssueImpl) issues.get(i);
            String snippetText = SnippetExtractor.extract(issueImpl.filePath(), issueImpl.location().textRange(),
                    fileContentCache);
            if (snippetText != null) {
                issuesAsJson.get(i).getAsJsonObject().getAsJsonObject("location")
                        .addProperty("snippet", snippetText);
            }
        }

        String json = gson.toJson(issuesAsJson);
        if (File.separator.equals("\\")) {
            // Gson JSON-escapes backslashes in fileName (\ -> \\), but fileName is a display field
            // that should preserve the OS-native single backslash separator as-is.
            Pattern p = Pattern.compile("(\"fileName\"\\s*:\\s*\")([^\"]*)(\")", Pattern.MULTILINE);
            Matcher m = p.matcher(json);
            json = m.replaceAll(mr -> Matcher.quoteReplacement(
                    mr.group(1) + mr.group(2).replace("\\\\", "\\") + mr.group(3)));
        }
        return json;
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
            message.addProperty("text", rule.name());
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
        Severity severity = rule.severity();
        if (severity != null) {
            return switch (severity) {
                case BLOCKER, HIGH -> "error";
                case MEDIUM -> "warning";
                case LOW -> "note";
                case INFO -> "none";
            };
        }
        return switch (rule.kind()) {
            case BUG -> "error";
            case VULNERABILITY -> "warning";
            case CODE_SMELL -> "note";
        };
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
        Target target = getTargetPath(project, directoryName);

        Path jsonFilePath;
        try {
            Path reportPath = target.getReportPath();

            // Save JSON report
            String jsonOutput = convertIssuesToJsonString(issues);
            File jsonFile = new File(reportPath.resolve(RESULTS_JSON_FILE).toString());
            try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFile);
                 Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                writer.write(new String(jsonOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                jsonFilePath = jsonFile.toPath();
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
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
        Target target = getTargetPath(project, directoryName);
        try {
            Path reportPath = target.getReportPath();
            String sarifOutput = convertIssuesToSarifString(issues, project);
            File sarifFile = new File(reportPath.resolve(RESULTS_SARIF_FILE).toString());
            try (FileOutputStream fos = new FileOutputStream(sarifFile);
                 Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(new String(sarifOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            }
            return sarifFile.toPath();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
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
        JsonObject scannedProject = new JsonObject();
        String name;
        if (project.kind() == ProjectKind.WORKSPACE_PROJECT) {
            name = Optional.of(project.sourceRoot().getFileName()).get().toString();
        } else {
            name = project.currentPackage().packageName().toString();
        }
        scannedProject.addProperty(SCAN_REPORT_PROJECT_NAME, name);
        Map<String, JsonObject> scanReportPathAndFile = new HashMap<>();

        for (Issue issue : issues) {
            IssueImpl issueImpl = (IssueImpl) issue;
            String filePath = issueImpl.filePath();

            if (scanReportPathAndFile.containsKey(filePath)) {
                JsonObject scanReportFile = scanReportPathAndFile.get(filePath);
                JsonArray issuesArray = scanReportFile.getAsJsonArray(SCAN_REPORT_ISSUES);
                JsonObject issueObject = getJsonIssue(issueImpl);
                issuesArray.add(issueObject);
                scanReportFile.add(SCAN_REPORT_ISSUES, issuesArray);
                scanReportPathAndFile.put(filePath, scanReportFile);
            } else {
                JsonObject scanReportFile = new JsonObject();
                scanReportFile.addProperty(SCAN_REPORT_FILE_NAME, issueImpl.fileName());
                scanReportFile.addProperty(SCAN_REPORT_FILE_PATH, filePath);
                String fileContent;
                try {
                    fileContent = Files.readString(Path.of(filePath));
                } catch (IOException ex) {
                    throw new ScanToolException(DiagnosticLog.error(DiagnosticCode.FAILED_TO_READ_BALLERINA_FILE,
                            ex.getMessage()));
                }
                scanReportFile.addProperty(SCAN_REPORT_FILE_CONTENT, fileContent);
                JsonArray issuesArray = new JsonArray();
                JsonObject issueObject = getJsonIssue(issueImpl);
                issuesArray.add(issueObject);
                scanReportFile.add(SCAN_REPORT_ISSUES, issuesArray);
                scanReportPathAndFile.put(filePath, scanReportFile);
            }
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
     * Returns the {@link JsonObject} representation of the static code analysis
     * issue.
     *
     * @param issueImpl the {@link IssueImpl} representing the issue, to be
     *                  converted to a JSON object
     * @return json object representation of the static code analysis issue
     */
    private static JsonObject getJsonIssue(IssueImpl issueImpl) {
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
        return scanReportIssue;
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
     * Scan.toml file.
     *
     * @param project      Ballerina project
     * @param outputStream print stream
     * @return the optional in-memory representation of the Scan.toml file
     */
    public static Optional<ScanTomlFile> loadScanTomlConfigurations(Project project, PrintStream outputStream) {
        if (!project.kind().equals(ProjectKind.BUILD_PROJECT) || project.currentPackage().ballerinaToml().isEmpty()) {
            return Optional.of(new ScanTomlFile());
        }

        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().get();
        TomlDocument ballerinaTomlDocument = ballerinaToml.tomlDocument();
        Toml ballerinaTomlDocumentContent = ballerinaTomlDocument.toml();

        Path targetDir = project.targetDir();
        if (ballerinaTomlDocumentContent.getTable(SCAN_TABLE).isEmpty()) {
            Path scanTomlFilePath = project.sourceRoot().resolve(SCAN_FILE);
            if (Files.exists(scanTomlFilePath)) {
                outputStream.println("Loading scan tool configurations from " + scanTomlFilePath);
                return loadScanFile(targetDir, scanTomlFilePath, outputStream);
            }
            return Optional.of(new ScanTomlFile());
        }

        Toml scanTable = ballerinaTomlDocumentContent.getTable(SCAN_TABLE).get();
        if (scanTable.get(SCAN_FILE_FIELD).isEmpty()) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.MISSING_CONFIG_FIELD));
            return Optional.empty();
        }

        TomlValueNode configPath = scanTable.get(SCAN_FILE_FIELD).get();
        String scanTomlPath = configPath.toNativeValue().toString();
        Optional<Path> path = convertConfigurationPath(scanTomlPath);
        Optional<Path> absPath = path.isEmpty() || path.get().isAbsolute() ? path
                : Optional.of(project.sourceRoot().resolve(scanTomlPath));

        if (absPath.isPresent() && absPath.get().toFile().isFile()) {
            return loadScanFile(targetDir, absPath.get(), outputStream);
        }
        return loadRemoteScanFile(targetDir, scanTomlPath, outputStream);
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
     * Returns the {@link Optional<ScanTomlFile>} representation a remote Scan.toml
     * file from the provided URL.
     *
     * @param targetDir    the target directory
     * @param scanTomlPath the remote Scan.toml file path
     * @param outputStream the print stream
     * @return the optional in-memory representation of the remote Scan.toml file
     */
    private static Optional<ScanTomlFile> loadRemoteScanFile(Path targetDir, String scanTomlPath,
                                                             PrintStream outputStream) {
        Path cachePath = targetDir.resolve(SCAN_FILE);

        if (Files.exists(cachePath)) {
            outputStream.println("Loading scan tool configurations from cache");
            return loadScanFile(targetDir, cachePath, outputStream);
        }

        URL remoteScanTomlFilePath;
        try {
            remoteScanTomlFilePath = new URL(scanTomlPath);
            FileUtils.copyURLToFile(remoteScanTomlFilePath, cachePath.toFile());
        } catch (IOException ex) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_CONFIG_FILE, ex.getMessage()));
            return Optional.empty();
        }
        outputStream.println("Loading scan tool configurations from " + remoteScanTomlFilePath);
        return loadScanFile(targetDir, cachePath, outputStream);
    }

    /**
     * Returns the {@link Optional<ScanTomlFile>} representation of the Scan.toml
     * file.
     *
     * @param targetDir        the target directory
     * @param scanTomlFilePath the local Scan.toml file path
     * @param outputStream     the print stream
     * @return the optional in-memory representation of the Scan.toml file
     */
    private static Optional<ScanTomlFile> loadScanFile(Path targetDir, Path scanTomlFilePath,
                                                       PrintStream outputStream) {
        Toml scanTomlDocumentContent;
        try {
            scanTomlDocumentContent = Toml.read(scanTomlFilePath);
        } catch (IOException ex) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.READING_CONFIG_FILE, ex.getMessage()));
            return Optional.empty();
        }
        ScanTomlFile scanTomlFile = new ScanTomlFile();

        for (Toml platformTable : scanTomlDocumentContent.getTables(PLATFORM_TABLE)) {
            if (!loadPlatform(platformTable, scanTomlFile, targetDir, outputStream)) {
                return Optional.empty();
            }
        }
        scanTomlDocumentContent.getTables(ANALYZER_TABLE).forEach(loadAnalyzer(scanTomlFile));
        scanTomlDocumentContent.getTable(RULES_TABLE).ifPresent(ruleTable -> {
            ruleTable.get("include").ifPresent(loadRules(scanTomlFile, true));
            ruleTable.get("exclude").ifPresent(loadRules(scanTomlFile, false));
        });
        return Optional.of(scanTomlFile);
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
     * Returns the boolean value indicating whether the scan process should be
     * continued.
     *
     * @param scanTomlFile the in-memory representation of the Scan.toml file
     * @param targetDir    the target directory
     * @param outputStream the print stream
     * @return the boolean value indicating whether the scan process should be
     * continued
     */
    private static boolean loadPlatform(Toml platformTable, ScanTomlFile scanTomlFile, Path targetDir,
                                        PrintStream outputStream) {
        Map<String, Object> properties = platformTable.toMap();
        Object platformName = properties.remove(PLATFORM_NAME);
        Optional<String> name = (platformName instanceof String) ? Optional.of(platformName.toString())
                : Optional.empty();
        Object platformPath = properties.remove(PLATFORM_PATH);
        Optional<String> path = (platformPath instanceof String) ? Optional.of(platformPath.toString())
                : Optional.empty();

        if (name.isEmpty() || name.get().isEmpty() || path.isEmpty()) {
            return true;
        }

        // Download remote JAR if the path is a URL and set the path to the downloaded
        // JAR
        if (!(new File(path.get()).exists())) {
            try {
                URL url = new URL(path.get());
                path = loadRemoteJAR(targetDir, name.get(), url, outputStream);
            } catch (MalformedURLException ex) {
                outputStream.println(DiagnosticLog.error(DiagnosticCode.LOADING_REMOTE_PLATFORM_FILE,
                        ex.getMessage()));
                return false;
            }
        }

        if (path.isEmpty() || !Files.exists(Path.of(path.get()))) {
            return false;
        }

        ScanTomlFile.Platform platform = new ScanTomlFile.Platform(name.get(), path.get(), properties);
        scanTomlFile.setPlatform(platform);
        return true;
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


    /**
     * Returns the {@link Optional<String>} path of the downloaded remote JAR file.
     *
     * @param targetDir     the target directory
     * @param fileName      the remote file name
     * @param remoteJarFile the remote JAR file path
     * @param outputStream  the print stream
     * @return the optional path of the downloaded remote JAR file
     */
    private static Optional<String> loadRemoteJAR(Path targetDir, String fileName, URL remoteJarFile,
                                                  PrintStream outputStream) {
        String platformJarFileName = fileName + JAR_PREDICATE;
        Path cachedJarPath = targetDir.resolve(platformJarFileName);
        if (Files.exists(cachedJarPath)) {
            outputStream.println("Loading " + platformJarFileName + " from cache");
            return Optional.of(cachedJarPath.toAbsolutePath().toString());
        }
        try {
            FileUtils.copyURLToFile(remoteJarFile, cachedJarPath.toFile());
            outputStream.println("Downloading remote JAR: " + remoteJarFile);
            return Optional.of(cachedJarPath.toAbsolutePath().toString());
        } catch (IOException ex) {
            outputStream.println(DiagnosticLog.error(DiagnosticCode.DOWNLOADING_REMOTE_JAR_FILE, platformJarFileName));
            return Optional.empty();
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
