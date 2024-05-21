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
import io.ballerina.projects.Project;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.scan.Issue;
import io.ballerina.scan.internal.IssueImpl;
import io.ballerina.tools.text.LineRange;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.ballerina.scan.utils.Constants.REPORT_DATA_PLACEHOLDER;
import static io.ballerina.scan.utils.Constants.RESULTS_HTML_FILE;
import static io.ballerina.scan.utils.Constants.RESULTS_JSON_FILE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_CONTENT;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_FILE_PATH;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_MESSAGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_RULE_ID;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_SEVERITY;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_END_LINE_OFFSET;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TEXT_RANGE_START_LINE_OFFSET;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ISSUE_TYPE;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_PROJECT_NAME;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_SCANNED_FILES;
import static io.ballerina.scan.utils.Constants.SCAN_REPORT_ZIP_FILE;

/**
 * {@code ScanUtils} contains all the utility functions used by the scan tool.
 *
 * @since 0.1.0
 * */
public final class ScanUtils {
    /**
     * Prints issues generated via static code analyzers to the console.
     *
     * @param issues       generated issues
     * @param outputStream print stream
     * */
    public static void printToConsole(List<Issue> issues, PrintStream outputStream) {
        String jsonOutput = convertIssuesToJsonString(issues);
        outputStream.println();
        outputStream.println(jsonOutput);
    }

    /**
     * Returns the json {@link String} array of generated issues.
     *
     * @param issues generated issues
     * @return json string array of generated issues
     * */
    private static String convertIssuesToJsonString(List<Issue> issues) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        return gson.toJson(issuesAsJson);
    }

    /**
     * Returns the {@link Path} of the json analysis report where generated issues are saved.
     *
     * @param issues        generated issues
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return path of the json analysis report where generated issues are saved
     * */
    public static Path saveToDirectory(List<Issue> issues, Project project, String directoryName) {
        Target target = getTargetPath(project, directoryName);

        Path jsonFilePath;
        try {
            Path reportPath = target.getReportPath();
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
     * Returns the generated {@link Target} directory where analysis reports are saved.
     *
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return generated target directory
     * */
    private static Target getTargetPath(Project project, String directoryName) {
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
     * Returns the {@link Path} of the html analysis report where generated issues are saved.
     *
     * @param issues        generated issues
     * @param project       Ballerina project
     * @param directoryName target directory name
     * @return path of the html analysis report where generated issues are saved
     */
    public static Path generateScanReport(List<Issue> issues, Project project, String directoryName) {
        JsonObject scannedProject = new JsonObject();
        scannedProject.addProperty(SCAN_REPORT_PROJECT_NAME, project.currentPackage().packageName().toString());
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
                continue;
            }

            JsonObject scanReportFile = new JsonObject();
            scanReportFile.addProperty(SCAN_REPORT_FILE_NAME, issueImpl.fileName());
            scanReportFile.addProperty(SCAN_REPORT_FILE_PATH, filePath);
            String fileContent;
            try {
                fileContent = Files.readString(Path.of(filePath));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read the file with exception: " + ex.getMessage());
            }
            scanReportFile.addProperty(SCAN_REPORT_FILE_CONTENT, fileContent);
            JsonArray issuesArray = new JsonArray();
            JsonObject issueObject = getJsonIssue(issueImpl);
            issuesArray.add(issueObject);
            scanReportFile.add(SCAN_REPORT_ISSUES, issuesArray);

            scanReportPathAndFile.put(filePath, scanReportFile);
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
            throw new RuntimeException("Failed to copy the file with exception: " + ex.getMessage());
        }
        return htmlFile.toPath();
    }

    /**
     * Returns the {@link JsonObject} representation of the static code analysis issue.
     *
     * @param issueImpl the {@link IssueImpl} representing the issue, to be converted to a JSON object
     * @return json object representation of the static code analysis issue
     */
    private static JsonObject getJsonIssue(IssueImpl issueImpl) {
        JsonObject scanReportIssue = new JsonObject();
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_RULE_ID, issueImpl.rule().id());
        scanReportIssue.addProperty(SCAN_REPORT_ISSUE_SEVERITY, issueImpl.rule().kind().toString());
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
        return scanReportIssue;
    }

    /**
     * Extracts the HTML report template zip from a provided resource stream to the provided destination.
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

    private ScanUtils() {
    }
}
