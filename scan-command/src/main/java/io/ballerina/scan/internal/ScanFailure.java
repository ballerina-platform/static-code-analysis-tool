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

import io.ballerina.cli.launcher.BLauncherException;
import io.ballerina.scan.utils.ScanToolException;

import java.util.ArrayList;
import java.util.List;

/**
 * Stops a scan with an exit code and the error report written to standard error: a {@code bal scan: error:} line,
 * followed by its diagnostics and, for a usage error, a pointer to the help.
 *
 * @since 0.13.0
 */
final class ScanFailure extends RuntimeException {
    static final String TOOL_PREFIX = "bal scan: ";
    private static final List<String> USAGE_HINT = List.of(
            "Usage: bal scan [OPTIONS] [<package>|<source-file>]",
            "Try 'bal scan --help' for more information.");

    private final ExitCode exitCode;
    private final List<String> details;

    private ScanFailure(ExitCode exitCode, String message, List<String> details) {
        super(message);
        this.exitCode = exitCode;
        this.details = List.copyOf(details);
    }

    static ScanFailure usage(String message, String... details) {
        return new ScanFailure(ExitCode.USAGE_ERROR, message, List.of(details));
    }

    static ScanFailure invalidTarget(String message, List<String> diagnostics) {
        return new ScanFailure(ExitCode.INVALID_TARGET, message, diagnostics);
    }

    static ScanFailure runtime(String message) {
        return runtime(message, List.of());
    }

    static ScanFailure runtime(String message, List<String> diagnostics) {
        return new ScanFailure(ExitCode.RUNTIME_ERROR, message, diagnostics);
    }

    static ScanFailure of(ExitCode exitCode, String message, List<String> diagnostics) {
        return new ScanFailure(exitCode, message, diagnostics);
    }

    static ScanFailure from(RuntimeException exception) {
        if (exception instanceof ScanFailure failure) {
            return failure;
        }
        if (exception instanceof ScanToolException toolException) {
            return runtime(toolException.getMessage(), toolException.diagnostics());
        }
        // A launcher exception, such as for dependencies that are not locked, carries its message in a list
        if (exception instanceof BLauncherException launcherException && !launcherException.getMessages().isEmpty()) {
            return runtime(String.join(System.lineSeparator(), launcherException.getMessages())
                    .replaceFirst("^error: ", ""));
        }
        return runtime(describe(exception));
    }

    static String describe(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && !message.isBlank() ? message : exception.getClass().getName();
    }

    static String error(String message) {
        return TOOL_PREFIX + "error: " + message;
    }

    static String warning(String message) {
        return TOOL_PREFIX + "warning: " + message;
    }

    ExitCode exitCode() {
        return exitCode;
    }

    List<String> details() {
        return details;
    }

    List<String> reportLines() {
        List<String> lines = new ArrayList<>();
        lines.add(error(getMessage()));
        lines.addAll(details);
        if (exitCode == ExitCode.USAGE_ERROR) {
            lines.addAll(USAGE_HINT);
        }
        return lines;
    }
}
