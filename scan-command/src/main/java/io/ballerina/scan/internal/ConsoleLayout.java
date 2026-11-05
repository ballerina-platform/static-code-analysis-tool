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

/**
 * Lays out the console output of a scan as sections separated by exactly one blank line. A section that prints
 * nothing leaves no blank line, and the output never starts with one.
 *
 * @since 0.11.1
 */
final class ConsoleLayout {
    /**
     * The sections of the console output, in the order a scan prints them.
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
        FAILURE
    }

    private Section current;
    private boolean written;
    private boolean separatorPending;

    /**
     * Returns a stream that writes to the target, starting each section that writes to it with a blank line.
     *
     * @param target the stream to write to
     * @return the tracked stream
     */
    PrintStream track(PrintStream target) {
        OutputStream tracked = new OutputStream() {
            @Override
            public void write(int b) {
                beforeWrite(target);
                target.write(b);
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
            }

            @Override
            public void flush() {
                target.flush();
            }
        };
        return new PrintStream(tracked, true, target.charset());
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

    /**
     * Prints the blank line that separates a new section from the output before it, before its first write.
     */
    private void beforeWrite(PrintStream target) {
        if (separatorPending) {
            separatorPending = false;
            target.println();
        }
        written = true;
    }
}
