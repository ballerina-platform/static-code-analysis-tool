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

package io.ballerina.scan;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestScanCommand {

    private static final Type listOfIssuesType = new TypeToken<ArrayList<Issue>>() {
    }.getType();

    @AfterEach
    @Test
    void test_scan_command() {
        // Simulating running 'bal scan' in a Ballerina project
        // Set up process
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> arguments = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
            arguments.add("cd bal-scan-tool-tester & bal scan --quiet");
        } else {
            arguments.add("sh");
            arguments.add("-c");
            arguments.add("cd bal-scan-tool-tester ; bal scan --quiet");
        }

        processBuilder.command(arguments);

        // Redirect IO
        processBuilder.inheritIO();

        // Start process
        int exitCode;
        try {
            Process scanProcess = processBuilder.start();
            exitCode = scanProcess.waitFor();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Read results file
        if (exitCode == 0) {
            File resultsFile = new File("./bal-scan-tool-tester/target/report/scan_results.json");
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(resultsFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JsonReader reader = new JsonReader(fileReader);
            Gson gson = new Gson();
            ArrayList<Issue> reportedIssues = gson.fromJson(reader, listOfIssuesType);

            // Assert receiving an empty issues array if the scan is successful
            Assertions.assertTrue(reportedIssues.isEmpty());
        }
    }

}
