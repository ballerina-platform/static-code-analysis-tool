/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.scan.Issue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static io.ballerina.scan.utilities.ScanToolConstants.RESULTS_JSON_FILE;
import static io.ballerina.scan.utilities.ScanToolConstants.TARGET_DIR_NAME;

public class ScanUtils {

    private static final PrintStream outputStream = System.out;

    private ScanUtils() {

    }

    public static void printToConsole(ArrayList<Issue> issues) {

        String jsonOutput = convertIssuesToJsonString(issues);

        outputStream.println();
        outputStream.println(jsonOutput);
    }

    public static String convertIssuesToJsonString(ArrayList<Issue> issues) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        return gson.toJson(issuesAsJson);
    }

    public static Target getTargetPath(Project project, String directoryName) {

        Target target;
        try {
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                if (directoryName != null) {
                    Path parentDirectory = project.sourceRoot().toAbsolutePath().getParent();
                    if (parentDirectory != null) {
                        Path targetDirectory = Files.createDirectories(parentDirectory.resolve(directoryName));
                        target = new Target(targetDirectory);
                    } else {
                        target = new Target(project.targetDir());
                    }
                } else {
                    target = new Target(project.targetDir());
                }
            } else {
                Path parentDirectory = project.sourceRoot().toAbsolutePath().getParent();
                if (parentDirectory != null) {
                    Path targetDirectory;
                    if (directoryName != null) {
                        targetDirectory = Files.createDirectories(parentDirectory.resolve(directoryName));
                    } else {
                        targetDirectory = Files.createDirectories(parentDirectory.resolve(TARGET_DIR_NAME));
                    }
                    target = new Target(targetDirectory);
                } else {
                    target = new Target(project.targetDir());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return target;
    }

    public static Path saveToDirectory(ArrayList<Issue> issues, String projectPath, String directoryName) {
        // Create folder to save issues to
        Project project = ProjectLoader.loadProject(Path.of(projectPath));
        Target target = getTargetPath(project, directoryName);

        // Retrieve path where report is saved
        Path jsonFilePath;
        try {
            Path reportPath = target.getReportPath();

            // Convert the issues to a json string
            String jsonOutput = convertIssuesToJsonString(issues);

            // Create the file to save the analysis issues to
            File jsonFile = new File(reportPath.resolve(RESULTS_JSON_FILE).toString());

            // Write results to file and return saved file path
            try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFile)) {
                try (Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                    writer.write(new String(jsonOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                    writer.close();

                    jsonFilePath = jsonFile.toPath();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jsonFilePath;
    }
}
