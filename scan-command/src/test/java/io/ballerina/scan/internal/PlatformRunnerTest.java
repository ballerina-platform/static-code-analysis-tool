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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Tests for running platform plugins.
 *
 * @since 0.11.1
 */
public class PlatformRunnerTest {
    private static final PlatformPluginContext CONTEXT = new PlatformPluginContextImpl(Map.of(), false);

    @Test(description = "test plugins run one after another, in order, on the calling thread")
    void testPluginsRunInOrder() {
        List<String> events = new ArrayList<>();
        Thread caller = Thread.currentThread();
        List<PlatformRunner.Task> tasks = List.of("a", "b", "c").stream()
                .map(name -> task(new RecordingPlugin(name, issues -> {
                    Assert.assertSame(Thread.currentThread(), caller);
                    events.add(name);
                }), issues(2)))
                .toList();

        List<Optional<String>> outcomes = PlatformRunner.runAll(tasks);

        Assert.assertEquals(events, List.of("a", "b", "c"));
        Assert.assertEquals(outcomes, List.of(Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test(description = "test each plugin gets its own copy of the issues")
    void testPluginsGetACopyOfTheIssues() {
        List<Issue> issues = issues(2);
        List<Integer> seenSizes = new ArrayList<>();
        List<PlatformRunner.Task> tasks = List.of(
                task(new RecordingPlugin("clears", List::clear), issues),
                task(new RecordingPlugin("counts", received -> seenSizes.add(received.size())), issues));

        PlatformRunner.runAll(tasks);

        Assert.assertEquals(seenSizes, List.of(2));
        Assert.assertEquals(issues.size(), 2, "the gate still counts every issue");
    }

    @Test(description = "test a failing plugin does not stop the others")
    void testFailingPluginDoesNotStopOthers() {
        List<String> events = new ArrayList<>();
        List<PlatformRunner.Task> tasks = List.of(
                task(new RecordingPlugin("fails", issues -> {
                    throw new IllegalStateException("upload failed");
                }), issues(1)),
                task(new RecordingPlugin("works", issues -> events.add("works")), issues(1)));

        List<Optional<String>> outcomes = PlatformRunner.runAll(tasks);

        Assert.assertEquals(events, List.of("works"));
        Assert.assertTrue(outcomes.get(0).orElseThrow().contains("upload failed"), outcomes.toString());
        Assert.assertTrue(outcomes.get(1).isEmpty());
    }

    private static PlatformRunner.Task task(StaticCodeAnalysisPlatformPlugin plugin, List<Issue> issues) {
        return new PlatformRunner.Task(plugin, CONTEXT, issues);
    }

    // The runner never reads the issues, so placeholders are enough
    private static List<Issue> issues(int count) {
        return new ArrayList<>(Collections.nCopies(count, null));
    }

    private record RecordingPlugin(String platform, Consumer<List<Issue>> onScan)
            implements StaticCodeAnalysisPlatformPlugin {
        @Override
        public void init(PlatformPluginContext platformPluginContext) {
        }

        @Override
        public void onScan(List<Issue> issues) {
            onScan.accept(issues);
        }
    }
}
