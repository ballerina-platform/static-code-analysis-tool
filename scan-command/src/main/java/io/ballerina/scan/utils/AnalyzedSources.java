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

package io.ballerina.scan.utils;

import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The sources of a package that a scan analyzes: the sources of every module, and their test sources unless the
 * tests are excluded. This is the one place that decides which documents are scanned.
 *
 * <p>The core rules analyze only these documents. Static code analyzers run as compiler plugins on every document;
 * they are told through {@code ScannerContext#isExcludeTests()}, but since existing analyzers do not check it, the
 * issues they report in a test source that is not analyzed are dropped after they report them. Suppressions and the
 * HTML report follow the same documents.
 *
 * @since 0.13.0
 */
public final class AnalyzedSources {
    private static final AnalyzedSources WITH_TESTS = new AnalyzedSources(false);
    private static final AnalyzedSources WITHOUT_TESTS = new AnalyzedSources(true);

    private final boolean excludeTests;

    private AnalyzedSources(boolean excludeTests) {
        this.excludeTests = excludeTests;
    }

    /**
     * Returns the analyzed sources of a package.
     *
     * @param excludeTests true to leave the test sources out of the scan
     * @return the analyzed sources
     */
    public static AnalyzedSources of(boolean excludeTests) {
        return excludeTests ? WITHOUT_TESTS : WITH_TESTS;
    }

    /**
     * Returns whether the test sources are left out of the scan.
     *
     * @return true if the test sources are not analyzed
     */
    public boolean excludesTests() {
        return excludeTests;
    }

    /**
     * Returns the analyzed documents of a module: its sources, followed by its test sources unless they are excluded.
     *
     * @param module the module
     * @return the IDs of the analyzed documents
     */
    public Stream<DocumentId> documentIds(Module module) {
        return excludeTests ? module.documentIds().stream()
                : Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream());
    }

    /**
     * Returns the file paths, as issues report them, of the documents of a package that are not analyzed.
     *
     * @param analyzedPackage the package
     * @return the paths of the test sources when the tests are excluded, otherwise an empty set
     */
    public Set<String> excludedFilePaths(Package analyzedPackage) {
        if (!excludeTests) {
            return Set.of();
        }
        Set<String> filePaths = new HashSet<>();
        analyzedPackage.moduleIds().forEach(moduleId -> {
            Module module = analyzedPackage.module(moduleId);
            module.testDocumentIds().forEach(documentId ->
                    filePaths.add(ScanUtils.getReportFilePath(module.document(documentId))));
        });
        return filePaths;
    }
}
