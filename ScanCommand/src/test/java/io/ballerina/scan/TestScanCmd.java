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

package io.ballerina.scan;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestScanCmd {
    @Test
    void testScanCommand() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> arguments = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
            arguments.add("cd bal-scan-tool-tester & bal scan");
        } else {
            arguments.add("sh");
            arguments.add("-c");
            arguments.add("cd bal-scan-tool-tester ; bal scan");
        }

        processBuilder.command(arguments);

        // Redirect IO
        StringBuilder output = new StringBuilder();
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);

        // Start process
        int exitCode;
        try {
            Process scanProcess = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(scanProcess.getInputStream(),
                    StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");

                // Check the first message
                if (output.length() > 1) {
                    break;
                }
            }
            reader.close();
            exitCode = scanProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (exitCode == 0) {
            // Assert the first line
            String firstLine = output.toString().trim().split("\n")[0];
            Assertions.assertEquals("Running Scans...", firstLine);
        }
    }
}
