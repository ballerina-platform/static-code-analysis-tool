/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;

import java.nio.file.Path;
import java.util.ArrayList;

public class ProjectAnalyzer {

    public ArrayList<Issue> analyzeProject(Path userPath) {

        // Issues store
        ArrayList<Issue> allIssues = new ArrayList<>();
        InternalScannerContext internalScannerContext = new InternalScannerContext(allIssues);

        // Get access to the project API
        Project project = ProjectLoader.loadProject(userPath);

        // For single file inputs with bal scan
        if (!userPath.toFile().isDirectory()) {
            // if the user provided file path belongs to a build project stop the analysis
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                return null;
            }

            Module tempModule = project.currentPackage().getDefaultModule();
            DocumentId documentId = project.documentId(userPath);
            analyzeDocument(project, tempModule, documentId, internalScannerContext);
        } else {
            // Iterate through each module of the project
            project.currentPackage().moduleIds().forEach(moduleId -> {
                // Get access to the project modules
                Module module = project.currentPackage().module(moduleId);

                // Iterate through each ballerina test file in a ballerina project and perform static analysis
                module.testDocumentIds().forEach(testDocumentID -> {
                    analyzeDocument(project, module, testDocumentID, internalScannerContext);
                });

                // Iterate through each document of the Main module/project + submodules
                module.documentIds().forEach(documentId -> {
                    analyzeDocument(project, module, documentId, internalScannerContext);
                });
            });
        }

        // return the detected issues
        return allIssues;
    }

    public void analyzeDocument(Project currentProject,
                                Module currentModule,
                                DocumentId documentId,
                                InternalScannerContext internalScannerContext) {
        // Retrieve each document from the module
        Document currentDocument = currentModule.document(documentId);

        // Retrieve syntax tree of each document
        SyntaxTree syntaxTree = currentDocument.syntaxTree();

        // Get semantic model from module compilation
        ModuleCompilation compilation = currentModule.getCompilation();
        SemanticModel semanticModel = compilation.getSemanticModel();

        // Perform core scans
        runInternalScans(currentProject,
                currentModule,
                currentDocument,
                syntaxTree,
                semanticModel,
                internalScannerContext);
    }

    public void runInternalScans(Project currentProject,
                                 Module currentModule,
                                 Document currentDocument,
                                 SyntaxTree syntaxTree,
                                 SemanticModel semanticModel,
                                 InternalScannerContext scannerContext) {

        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(currentProject,
                currentModule,
                currentDocument,
                syntaxTree,
                semanticModel,
                scannerContext);

        analyzer.initialize();
    }
}
