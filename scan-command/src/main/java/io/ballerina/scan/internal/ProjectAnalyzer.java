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

package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Project;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.ScanTomlFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.FORWARD_SLASH;
import static io.ballerina.scan.internal.ScanToolConstants.IMPORT_GENERATOR_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.USE_IMPORT_AS_UNDERSCORE;

/**
 * Represents the project analyzer used for analyzing projects.
 *
 * @since 0.1.0
 * */
class ProjectAnalyzer {
    private final ScanTomlFile scanTomlFile;

    ProjectAnalyzer(ScanTomlFile scanTomlFile) {
        this.scanTomlFile = scanTomlFile;
    }

    List<Issue> analyze(Project project, List<Rule> inbuiltRules) {
        ScannerContextImpl scannerContext = new ScannerContextImpl(inbuiltRules);
        project.currentPackage().moduleIds().forEach(moduleId -> {
            Module module = project.currentPackage().module(moduleId);
            module.documentIds().forEach(analyzeDocument(module, scannerContext));
            module.testDocumentIds().forEach(analyzeDocument(module, scannerContext));
        });
        return scannerContext.getReporter().getIssues();
    }

    private Consumer<DocumentId> analyzeDocument(Module module, ScannerContextImpl scannerContext) {
        return documentId -> {
            Document document = module.document(documentId);
            StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(document, scannerContext);
            analyzer.analyze();
        };
    }

    List<Issue> runExternalAnalyzers(Project project) {
        StringBuilder newImports = new StringBuilder();
        StringBuilder tomlDependencies = new StringBuilder();

        for (ScanTomlFile.Analyzer analyzer : scanTomlFile.getAnalyzers()) {
            extractAnalyzerImportsAndDependencies(analyzer, newImports, tomlDependencies);
        }
        Module defaultModule = project.currentPackage().getDefaultModule();
        List<String> defaultModuleFiles = defaultModule.documentIds()
                .stream()
                .map(documentId -> defaultModule.document(documentId).name())
                .toList();
        String documentName;
        do {
            documentName = String.format("%s-%s.bal", IMPORT_GENERATOR_FILE, UUID.randomUUID());
        } while (defaultModuleFiles.contains(documentName));
        ModuleId defaultModuleId = defaultModule.moduleId();
        DocumentId documentId = DocumentId.create(documentName, defaultModuleId);
        DocumentConfig documentConfig = DocumentConfig.from(documentId, newImports.toString(), documentName);
        defaultModule.modify().addDocument(documentConfig).apply();

        project.currentPackage().ballerinaToml().ifPresent(ballerinaToml -> {
            String tomlFileContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(tomlFileContent + tomlDependencies).apply();
        });

        List<ScannerContext> scannerContextList = new ArrayList<>();

        // TODO: Implement the logic to pass scanner contexts to compiler plugins and perform external static analysis.

        List<Issue> externalIssues = new ArrayList<>(scannerContextList.size());
        for (ScannerContext scannerContext : scannerContextList) {
            ReporterImpl reporter = (ReporterImpl) scannerContext.getReporter();
            externalIssues.addAll(reporter.getIssues());
        }
        return externalIssues;
    }

    private void extractAnalyzerImportsAndDependencies(ScanTomlFile.Analyzer analyzer, StringBuilder imports,
                                                       StringBuilder dependencies) {
        String org = analyzer.org();
        String name = analyzer.name();
        String version = analyzer.version();
        String repository = analyzer.repository();
        buildStringWithNewLine(imports, IMPORT_PREFIX + org + FORWARD_SLASH + name + USE_IMPORT_AS_UNDERSCORE);

        if (version == null) {
            return;
        }
        buildStringWithNewLine(dependencies, "");
        buildStringWithNewLine(dependencies, "[[dependency]]");
        buildStringWithNewLine(dependencies, "org = '" + org + "'");
        buildStringWithNewLine(dependencies, "name = '" + name + "'");
        buildStringWithNewLine(dependencies, "version = '" + version + "'");

        if (repository == null) {
            return;
        }
        buildStringWithNewLine(dependencies, "repository = '" + repository + "'");
    }

    private void buildStringWithNewLine(StringBuilder stringBuilder, String content) {
        stringBuilder.append(content).append(System.lineSeparator());
    }
}
