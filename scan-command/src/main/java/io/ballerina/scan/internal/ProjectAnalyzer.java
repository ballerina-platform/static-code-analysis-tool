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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.CompilerPluginCache;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.PackageDependencyScope;
import io.ballerina.projects.PackageManifest;
import io.ballerina.projects.PackageResolution;
import io.ballerina.projects.Project;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.internal.model.CompilerPluginDescriptor;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleProvider;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanToolException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.FORWARD_SLASH;
import static io.ballerina.scan.internal.ScanToolConstants.IMPORT_GENERATOR_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.SCANNER_CONTEXT;
import static io.ballerina.scan.internal.ScanToolConstants.USE_IMPORT_AS_UNDERSCORE;

/**
 * Represents the project analyzer used for analyzing projects.
 *
 * @since 0.1.0
 */
public class ProjectAnalyzer {
    private final Project project;
    private final ScanTomlFile scanTomlFile;
    private String pluginImportsDocumentName;

    protected ProjectAnalyzer(Project project, ScanTomlFile scanTomlFile) {
        this.project = project;
        this.scanTomlFile = scanTomlFile;
        Module defaultModule = project.currentPackage().getDefaultModule();
        List<String> defaultModuleFiles = defaultModule.documentIds()
                .stream()
                .map(documentId -> defaultModule.document(documentId).name())
                .toList();
        do {
            pluginImportsDocumentName = String.format("%s-%s.bal", IMPORT_GENERATOR_FILE, UUID.randomUUID());
        } while (defaultModuleFiles.contains(pluginImportsDocumentName));
    }

    List<Issue> analyze(List<Rule> inbuiltRules) {
        ScannerContextImpl scannerContext = new ScannerContextImpl(inbuiltRules);
        project.currentPackage().moduleIds().forEach(moduleId -> {
            Module module = project.currentPackage().module(moduleId);
            module.documentIds().forEach(analyzeDocument(module, scannerContext));
            module.testDocumentIds().forEach(analyzeDocument(module, scannerContext));
        });
        return scannerContext.getReporter().getIssues();
    }

    private Consumer<DocumentId> analyzeDocument(Module module, ScannerContextImpl scannerContext) {
        SemanticModel semanticModel = module.getCompilation().getSemanticModel();
        return documentId -> {
            Document document = module.document(documentId);
            StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(document, scannerContext, semanticModel);
            analyzer.analyze();
        };
    }

    Map<String, List<Rule>> getExternalAnalyzers() {
        StringBuilder newImports = new StringBuilder();
        StringBuilder tomlDependencies = new StringBuilder();
        Set<ScanTomlFile.Analyzer> analyzers = scanTomlFile.getAnalyzers();
        for (ScanTomlFile.Analyzer analyzer : analyzers) {
            extractAnalyzerImportsAndDependencies(analyzer, newImports, tomlDependencies);
        }

        Module defaultModule = project.currentPackage().getDefaultModule();
        ModuleId defaultModuleId = defaultModule.moduleId();
        DocumentId documentId = DocumentId.create(pluginImportsDocumentName, defaultModuleId);
        DocumentConfig documentConfig = DocumentConfig.from(documentId, newImports.toString(),
                pluginImportsDocumentName);
        defaultModule.modify().addDocument(documentConfig).apply();
        project.currentPackage().ballerinaToml().ifPresent(ballerinaToml -> {
            String tomlFileContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(tomlFileContent + tomlDependencies).apply();
        });

        PackageResolution packageResolution = project.currentPackage().getResolution(project.currentPackage()
                .compilationOptions());
        ResolvedPackageDependency rootPkgNode = new ResolvedPackageDependency(project.currentPackage(),
                PackageDependencyScope.DEFAULT);
        Map<String, List<Rule>> externalAnalyzers = new HashMap<>();

        packageResolution.dependencyGraph().getDirectDependencies(rootPkgNode).stream()
                .map(ResolvedPackageDependency::packageInstance).forEach(pkgDependency -> {
                    PackageManifest pkgManifest = pkgDependency.manifest();
                    String org = pkgManifest.org().value();
                    String name = pkgManifest.name().value();
                    if (pkgManifest.compilerPluginDescriptor().isEmpty()) {
                        return;
                    }

                    CompilerPluginDescriptor pluginDesc = pkgDependency.manifest().compilerPluginDescriptor()
                            .orElse(null);
                    if (pluginDesc == null) {
                        externalAnalyzers.put(pkgDependency.manifest().compilerPluginDescriptor().toString(),
                                new ArrayList<>());
                        return;
                    }
                    List<Rule> externalRules;
                    try {
                        externalRules = loadRulesFromEnum(pluginDesc, org, name);
                    } catch (IOException e) {
                        DiagnosticLog.error(DiagnosticCode.FAILED_TO_LOAD_COMPILER_PLUGIN,
                                "IOException occurred while loading rules: " + e.getMessage());
                        externalRules = new ArrayList<>();
                    } catch (ScanToolException e) {
                        DiagnosticLog.error(DiagnosticCode.FAILED_TO_LOAD_COMPILER_PLUGIN,
                                "ScanToolException occurred while loading rules: " + e.getMessage());
                        externalRules = new ArrayList<>();
                    }
                    externalAnalyzers.put(pluginDesc.plugin().getClassName(), externalRules);
                });
        return externalAnalyzers;
    }

    private List<Rule> loadRulesFromEnum(CompilerPluginDescriptor pluginDesc, String org, String name)
            throws IOException {
        List<URL> jarUrls = pluginDesc.dependencies().stream().map(dependency -> {
            try {
                return Path.of(dependency.getPath()).toUri().toURL();
            } catch (MalformedURLException ex) {
                throw new ScanToolException(
                        DiagnosticLog.error(DiagnosticCode.FAILED_TO_LOAD_COMPILER_PLUGIN, ex.getMessage()));
            }
        }).toList();

        try (URLClassLoader ucl = URLClassLoader.newInstance(jarUrls.toArray(URL[]::new),
                this.getClass().getClassLoader())) {
            ServiceLoader<RuleProvider> loader = ServiceLoader.load(RuleProvider.class, ucl);
            List<Rule> rules = new ArrayList<>();
            for (RuleProvider provider : loader) {
                for (Rule r : provider.getRules()) {
                    Rule inMemoryRule = RuleFactory.createRule(r.numericId(),
                            r.description(), r.kind(), org, name);
                    rules.add(inMemoryRule);
                }
            }
            return rules;
        }
    }

    private void extractAnalyzerImportsAndDependencies(ScanTomlFile.Analyzer analyzer, StringBuilder imports,
                                                       StringBuilder dependencies) {
        String org = analyzer.org();
        String name = analyzer.name();
        String analyzerDescriptor = org + FORWARD_SLASH + name;
        buildStringWithNewLine(imports, IMPORT_PREFIX + analyzerDescriptor + USE_IMPORT_AS_UNDERSCORE);

        String version = analyzer.version();
        if (version != null) {
            buildStringWithNewLine(dependencies, "");
            buildStringWithNewLine(dependencies, "[[dependency]]");
            buildStringWithNewLine(dependencies, "org = '" + org + "'");
            buildStringWithNewLine(dependencies, "name = '" + name + "'");
            buildStringWithNewLine(dependencies, "version = '" + version + "'");
        }

        String repository = analyzer.repository();
        if (repository != null) {
            buildStringWithNewLine(dependencies, "repository = '" + repository + "'");
        }
    }

    private void buildStringWithNewLine(StringBuilder stringBuilder, String content) {
        stringBuilder.append(content).append(System.lineSeparator());
    }

    List<Issue> runExternalAnalyzers(Map<String, List<Rule>> externalAnalyzers) {
        List<ScannerContext> scannerContextList = new ArrayList<>(externalAnalyzers.size());
        for (Map.Entry<String, List<Rule>> externalAnalyzer : externalAnalyzers.entrySet()) {
            ScannerContext scannerContext = getScannerContext(externalAnalyzer.getValue());
            scannerContextList.add(scannerContext);

            Map<String, Object> pluginProperties = new HashMap<>();
            pluginProperties.put(SCANNER_CONTEXT, scannerContext);
            project.projectEnvironmentContext()
                    .getService(CompilerPluginCache.class)
                    .putData(externalAnalyzer.getKey(), pluginProperties);
        }

        project.currentPackage().getCompilation();
        List<Issue> externalIssues = new ArrayList<>(scannerContextList.size());
        for (ScannerContext scannerContext : scannerContextList) {
            ReporterImpl reporter = (ReporterImpl) scannerContext.getReporter();
            for (Issue issue : reporter.getIssues()) {
                if (!issue.location().lineRange().fileName().equals(pluginImportsDocumentName)) {
                    externalIssues.add(issue);
                }
            }
        }
        return externalIssues;
    }

    protected ScannerContext getScannerContext(List<Rule> rules) {
        return new ScannerContextImpl(rules);
    }
}
