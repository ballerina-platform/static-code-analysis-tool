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

import io.ballerina.scan.Issue;
import io.ballerina.scan.PlatformPluginContext;
import io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runs platform plugins one after another, so that plugins written for sequential calls do not race over shared
 * state, working directories or the console. A failing plugin does not stop the others.
 *
 * @since 0.11.1
 */
final class PlatformRunner {

    private PlatformRunner() {
    }

    /**
     * Initializes each plugin and reports the issues to it. Each plugin gets its own copy of the issues, so that a
     * plugin that changes the list does not affect another plugin or the quality gate.
     *
     * @param tasks the plugins to run
     * @return the failure reason of each task, in the order of the tasks, empty for a task that succeeded
     */
    static List<Optional<String>> runAll(List<Task> tasks) {
        List<Optional<String>> outcomes = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            outcomes.add(run(task));
        }
        return outcomes;
    }

    /**
     * Initializes a plugin and reports the issues to it, catching its failure so that the other plugins still run.
     */
    private static Optional<String> run(Task task) {
        try {
            task.plugin().init(task.context());
            task.plugin().onScan(new ArrayList<>(task.issues()));
            return Optional.empty();
        } catch (RuntimeException | LinkageError ex) {
            return Optional.of(ScanFailure.describe(ex));
        }
    }

    record Task(StaticCodeAnalysisPlatformPlugin plugin, PlatformPluginContext context, List<Issue> issues) {
    }
}
