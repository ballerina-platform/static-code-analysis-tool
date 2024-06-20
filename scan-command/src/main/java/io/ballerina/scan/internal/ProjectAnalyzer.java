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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.projects.CompilerPluginCache;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDependencyScope;
import io.ballerina.projects.PackageManifest;
import io.ballerina.projects.PackageResolution;
import io.ballerina.projects.Project;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.internal.model.CompilerPluginDescriptor;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.DiagnosticCode;
import io.ballerina.scan.utils.DiagnosticLog;
import io.ballerina.scan.utils.ScanTomlFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.BUG;
import static io.ballerina.scan.internal.ScanToolConstants.CODE_SMELL;
import static io.ballerina.scan.internal.ScanToolConstants.FORWARD_SLASH;
import static io.ballerina.scan.internal.ScanToolConstants.IMPORT_GENERATOR_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.RULES_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.RULE_DESCRIPTION;
import static io.ballerina.scan.internal.ScanToolConstants.RULE_ID;
import static io.ballerina.scan.internal.ScanToolConstants.RULE_KIND;
import static io.ballerina.scan.internal.ScanToolConstants.SCANNER_CONTEXT;
import static io.ballerina.scan.internal.ScanToolConstants.USE_IMPORT_AS_UNDERSCORE;
import static io.ballerina.scan.internal.ScanToolConstants.VULNERABILITY;

/**
 * Represents the project analyzer used for analyzing projects.
 *
 * @since 0.1.0
 * */
class ProjectAnalyzer {
    private final Project project;
    private final ScanTomlFile scanTomlFile;
    private String pluginImportsDocumentName;

    ProjectAnalyzer(Project project, ScanTomlFile scanTomlFile) {
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
        return documentId -> {
            Document document = module.document(documentId);
            StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(document, scannerContext);
            analyzer.analyze();
        };
    }

    Map<String, List<Rule>> getExternalAnalyzers() {
        StringBuilder newImports = new StringBuilder();
        StringBuilder tomlDependencies = new StringBuilder();
        Set<ScanTomlFile.Analyzer> analyzers = scanTomlFile.getAnalyzers();
        List<String> analyzerDescriptors = new ArrayList<>(analyzers.size());
        for (ScanTomlFile.Analyzer analyzer : analyzers) {
            analyzerDescriptors.add(extractAnalyzerImportsAndDependencies(analyzer, newImports, tomlDependencies));
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

        PackageResolution packageResolution = project.currentPackage().getResolution();
        ResolvedPackageDependency rootPkgNode = new ResolvedPackageDependency(project.currentPackage(),
                PackageDependencyScope.DEFAULT);
        List<Package> directDependencies = packageResolution.dependencyGraph().getDirectDependencies(rootPkgNode)
                .stream().map(ResolvedPackageDependency::packageInstance).toList();
        Map<String, List<Rule>> externalAnalyzers = new HashMap<>();
        for (Package pkgDependency : directDependencies) {
            PackageManifest pkgManifest = pkgDependency.manifest();
            String org = pkgManifest.org().value();
            String name = pkgManifest.name().value();
            String pluginName = org + FORWARD_SLASH + name;
            if (pkgManifest.compilerPluginDescriptor().isEmpty() || !analyzerDescriptors.contains(pluginName)) {
                continue;
            }

            CompilerPluginDescriptor pluginDesc = pkgManifest.compilerPluginDescriptor().get();
            List<URL> jarUrls = pluginDesc.dependencies().stream().map(dependency -> {
                        try {
                            return Path.of(dependency.getPath()).toUri().toURL();
                        } catch (MalformedURLException ex) {
                            throw new RuntimeException(ex);
                        }
            }).toList();

            URLClassLoader ucl = URLClassLoader.newInstance(jarUrls.toArray(URL[]::new),
                    this.getClass().getClassLoader());
            InputStream resourceAsStream = ucl.getResourceAsStream(RULES_FILE);
            if (resourceAsStream == null) {
                continue;
            }

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                    StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            List<Rule> externalRules = new ArrayList<>();
            Gson gson = new Gson();
            JsonElement element = gson.fromJson(output, JsonElement.class);
            if (!element.isJsonArray()) {
                throw new RuntimeException(DiagnosticLog.error(DiagnosticCode.INVALID_JSON_FORMAT, RULES_FILE,
                        pluginName, gson.toJson(element)));
            }

            JsonArray ruleArray = element.getAsJsonArray();
            for (JsonElement rule : ruleArray) {
                JsonObject ruleObject = rule.getAsJsonObject();
                if (!isValidRule(ruleObject)) {
                    throw new RuntimeException(DiagnosticLog.error(DiagnosticCode.INVALID_JSON_FORMAT_RULE,
                            pluginName, gson.toJson(ruleObject)));
                }

                String kind = ruleObject.get(RULE_KIND).getAsString();
                RuleKind ruleKind;
                switch (kind) {
                    case BUG -> ruleKind = RuleKind.BUG;
                    case VULNERABILITY -> ruleKind = RuleKind.VULNERABILITY;
                    case CODE_SMELL -> ruleKind = RuleKind.CODE_SMELL;
                    default -> {
                        throw new RuntimeException(DiagnosticLog.error(DiagnosticCode.INVALID_JSON_FORMAT_RULE_KIND,
                                pluginName, Arrays.toString(RuleKind.values()), kind));
                    }
                };
                Rule inMemoryRule = RuleFactory.createRule(ruleObject.get(RULE_ID).getAsInt(),
                        ruleObject.get(RULE_DESCRIPTION).getAsString(), ruleKind, org, name);
                externalRules.add(inMemoryRule);
            }
            externalAnalyzers.put(pluginDesc.plugin().getClassName(), externalRules);
        }
        return externalAnalyzers;
    }

    private String extractAnalyzerImportsAndDependencies(ScanTomlFile.Analyzer analyzer, StringBuilder imports,
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
        return analyzerDescriptor;
    }

    private void buildStringWithNewLine(StringBuilder stringBuilder, String content) {
        stringBuilder.append(content).append(System.lineSeparator());
    }

    private boolean isValidRule(JsonObject ruleObject) {
        return ruleObject.has(RULE_ID) &&
                ruleObject.get(RULE_ID).isJsonPrimitive() &&
                ruleObject.get(RULE_ID).getAsJsonPrimitive().isNumber() &&
                ruleObject.has(RULE_KIND) &&
                ruleObject.has(RULE_DESCRIPTION);
    }

    List<Issue> runExternalAnalyzers(Map<String, List<Rule>> externalAnalyzers) {
        List<ScannerContext> scannerContextList = new ArrayList<>(externalAnalyzers.size());
        for (Map.Entry<String, List<Rule>> externalAnalyzer : externalAnalyzers.entrySet()) {
            ScannerContextImpl scannerContext = new ScannerContextImpl(externalAnalyzer.getValue());
            scannerContextList.add(scannerContext);

            // Save the scanner context to plugin cache for the compiler plugin to use during package compilation
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
}
