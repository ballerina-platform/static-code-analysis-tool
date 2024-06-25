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

package io.ballerina.exampleplatformplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ballerina.scan.Issue;
import io.ballerina.scan.PlatformPluginContext;
import io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PlatformPlugin implements StaticCodeAnalysisPlatformPlugin {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String PLATFORM_ARGS_FILE = "platform-arguments.json";
    private static final String ANALYSIS_ISSUES_FILES = "analysis-issues.json";
    private Map<String, String> platformArgs;
    private List<Issue> issues;

    @Override
    public String platform() {
        return "examplePlatform";
    }

    @Override
    public void init(PlatformPluginContext platformArgs) {
        this.platformArgs =  platformArgs.platformArgs();
    }

    @Override
    public void onScan(List<Issue> issues) {
        this.issues = Collections.unmodifiableList(issues);
        saveToFile();
    }

    private void saveToFile() {
        Path projectDir = Path.of(System.getProperty("user.dir"));
        try (FileWriter writer = new FileWriter(projectDir.resolve(PLATFORM_ARGS_FILE).toFile(),
                StandardCharsets.UTF_8)) {
            gson.toJson(platformArgs, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while writing platform arguments to file", ex);
        }
        try (FileWriter writer = new FileWriter(projectDir.resolve(ANALYSIS_ISSUES_FILES).toFile(),
                StandardCharsets.UTF_8)) {
            gson.toJson(issues, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while writing analysis issues to file", ex);
        }
    }
}
