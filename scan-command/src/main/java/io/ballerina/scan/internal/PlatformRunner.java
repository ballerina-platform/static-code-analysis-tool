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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs platform plugins at the same time, each on its own thread, and waits for all of them.
 *
 * @since 0.13.0
 */
final class PlatformRunner {

    private PlatformRunner() {
    }

    /**
     * Initializes each plugin and reports the issues to it.
     *
     * @param tasks the plugins to run
     * @return the failure reason of each task, in the order of the tasks, empty for a task that succeeded
     */
    static List<Optional<String>> runAll(List<Task> tasks) {
        List<Optional<String>> outcomes = new ArrayList<>();
        if (tasks.isEmpty()) {
            return outcomes;
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        AtomicInteger threadCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size(), runnable -> {
            Thread thread = new Thread(runnable, "bal-scan-platform-" + threadCount.incrementAndGet());
            thread.setDaemon(true);
            thread.setContextClassLoader(contextClassLoader);
            return thread;
        });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Task task : tasks) {
                futures.add(executor.submit(() -> {
                    task.plugin().init(task.context());
                    task.plugin().onScan(task.issues());
                }));
            }
            for (Future<?> future : futures) {
                outcomes.add(await(future));
            }
        } finally {
            executor.shutdownNow();
        }
        return outcomes;
    }

    private static Optional<String> await(Future<?> future) {
        try {
            future.get();
            return Optional.empty();
        } catch (ExecutionException ex) {
            return Optional.of(ScanFailure.describe(ex.getCause() == null ? ex : ex.getCause()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.of(ScanFailure.describe(ex));
        }
    }

    record Task(StaticCodeAnalysisPlatformPlugin plugin, PlatformPluginContext context, List<Issue> issues) {
    }
}
