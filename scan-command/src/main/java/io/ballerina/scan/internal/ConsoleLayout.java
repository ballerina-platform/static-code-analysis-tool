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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lays out the console output of a scan as sections separated by exactly one blank line. A section that prints
 * nothing leaves no blank line, and the output never starts with one.
 *
 * @since 0.13.0
 */
final class ConsoleLayout {
    /**
     * The sections of the console output.
     */
    enum Section {
        RESOLVING,
        LOADING,
        WARNINGS,
        RUNNING,
        COMBINING,
        RULES,
        RESULTS,
        SUMMARY,
        RESULTS_FILE,
        SCAN_REPORT,
        PLATFORMS,
        PLATFORM_OUTPUT,
        FAILURE
    }

    private final List<PrintStream> targets = new ArrayList<>();
    private PrintStream separatorTarget;
    private Section current;
    private boolean written;
    private boolean separatorPending;
    // Line feeds at the end of the output, so that output already ending with a blank line gets no second one
    private int trailingLineFeeds;

    /**
     * Returns a stream that writes to the target, starting each section that writes to it with a blank line.
     *
     * @param target the stream to write to
     * @return the tracked stream
     */
    PrintStream track(PrintStream target) {
        Objects.requireNonNull(target, "target");
        targets.add(target);
        OutputStream tracked = new OutputStream() {
            @Override
            public void write(int b) {
                beforeWrite(target);
                target.write(b);
                afterWrite(b);
            }

            /**
             * Writes the bytes, ignoring an empty write so that it does not start a section.
             */
            @Override
            public void write(byte[] bytes, int offset, int length) {
                if (length == 0) {
                    return;
                }
                beforeWrite(target);
                target.write(bytes, offset, length);
                for (int i = offset; i < offset + length; i++) {
                    afterWrite(bytes[i]);
                }
            }

            @Override
            public void flush() {
                target.flush();
            }
        };
        return new PrintStream(tracked, true, target.charset());
    }

    /**
     * Writes every blank line that separates sections to the given stream, so that the other streams, such as
     * standard output, hold only their own content.
     *
     * @param target the stream the separators are written to
     */
    void separateWith(PrintStream target) {
        separatorTarget = target;
    }

    /**
     * Starts a section, unless it is the current one, so that its first line is separated from the output before it.
     *
     * @param section the section the next lines belong to
     */
    void enter(Section section) {
        if (section != current) {
            current = section;
            separatorPending = written;
        }
    }

    Section current() {
        return current;
    }

    private void beforeWrite(PrintStream target) {
        if (separatorPending) {
            separatorPending = false;
            if (trailingLineFeeds < 2) {
                PrintStream separator = separatorTarget == null ? target : separatorTarget;
                // The streams are flushed first, so that the separator lands between the sections on a console
                targets.forEach(PrintStream::flush);
                separator.println();
                separator.flush();
                trailingLineFeeds++;
            }
        }
        written = true;
    }

    private void afterWrite(int b) {
        if (b == '\n') {
            trailingLineFeeds++;
        } else if (b != '\r') {
            trailingLineFeeds = 0;
        }
    }
}
