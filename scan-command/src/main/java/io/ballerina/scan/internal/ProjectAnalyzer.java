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

import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.ScanTomlFile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.FORWARD_SLASH;
import static io.ballerina.scan.internal.ScanToolConstants.USE_IMPORT_AS_SERVICE;

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
        int importCounter = 0;

        for (ScanTomlFile.Analyzer analyzer : scanTomlFile.getAnalyzers()) {
            String reportingSource = analyzer.getOrg() + FORWARD_SLASH + analyzer.getName();
            String analyzerImport = IMPORT_PREFIX + reportingSource + USE_IMPORT_AS_SERVICE;
            newImports.append(analyzerImport).append("\n");

            if (analyzer.getVersion() != null) {
                tomlDependencies.append("\n");
                tomlDependencies.append("[[dependency]]" + "\n");
                tomlDependencies.append("org='").append(analyzer.getOrg()).append("'\n");
                tomlDependencies.append("name='").append(analyzer.getName()).append("'\n");
                tomlDependencies.append("version='").append(analyzer.getVersion()).append("'\n");

                if (analyzer.getRepository() != null) {
                    tomlDependencies.append("repository='").append(analyzer.getRepository()).append("'\n");
                }
            }
            importCounter++;
        }

        Module defaultModule = project.currentPackage().getDefaultModule();
        Document mainBAL = defaultModule.document(defaultModule.documentIds().iterator().next());
        String documentContent = mainBAL.textDocument().toString();
        mainBAL.modify().withContent(newImports + documentContent).apply();

        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            documentContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(documentContent + tomlDependencies).apply();
        }

        List<ScannerContext> scannerContextList = new ArrayList<>();

        // TODO: Implement the logic to pass scanner contexts to compiler plugins and perform external static analysis.

        List<Issue> externalIssues = new ArrayList<>();
        for (ScannerContext scannerContext : scannerContextList) {
            ReporterImpl reporter = (ReporterImpl) scannerContext.getReporter();
            externalIssues.addAll(reporter.getIssues());
        }
        return externalIssues;
    }
}
