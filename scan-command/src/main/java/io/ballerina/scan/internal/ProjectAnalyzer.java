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
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageResolution;
import io.ballerina.projects.Project;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.internal.model.CompilerPluginDescriptor;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.ScannerContext;
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.FORWARD_SLASH;
import static io.ballerina.scan.internal.ScanToolConstants.IMPORT_GENERATOR_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.RULES_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.USE_IMPORT_AS_UNDERSCORE;

/**
 * Represents the project analyzer used for analyzing projects.
 *
 * @since 0.1.0
 * */
class ProjectAnalyzer {
    private final ScanTomlFile scanTomlFile;
    private String generatedDocumentName = "";

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

    Map<String, List<Rule>> getExternalAnalyzers(Project project) {
        StringBuilder newImports = new StringBuilder();
        StringBuilder tomlDependencies = new StringBuilder();
        List<String> analyzerDescriptors = new ArrayList<>();
        for (ScanTomlFile.Analyzer analyzer : scanTomlFile.getAnalyzers()) {
            extractAnalyzerImportsAndDependencies(analyzer, newImports, tomlDependencies, analyzerDescriptors);
        }

        Module defaultModule = project.currentPackage().getDefaultModule();
        List<String> defaultModuleFiles = defaultModule.documentIds()
                .stream()
                .map(documentId -> defaultModule.document(documentId).name())
                .toList();
        do {
            generatedDocumentName = String.format("%s-%s.bal", IMPORT_GENERATOR_FILE, UUID.randomUUID());
        } while (defaultModuleFiles.contains(generatedDocumentName));

        ModuleId defaultModuleId = defaultModule.moduleId();
        DocumentId documentId = DocumentId.create(generatedDocumentName, defaultModuleId);
        DocumentConfig documentConfig = DocumentConfig.from(documentId, newImports.toString(), generatedDocumentName);
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
            PackageOrg org = pkgManifest.org();
            PackageName name = pkgManifest.name();
            if (pkgManifest.compilerPluginDescriptor().isEmpty() || !analyzerDescriptors
                    .contains(org.value() + FORWARD_SLASH + name.value())) {
                continue;
            }

            CompilerPluginDescriptor pluginDesc = pkgManifest.compilerPluginDescriptor().get();
            List<String> jarPaths = new ArrayList<>();
            pluginDesc.dependencies().forEach(dependency -> {
                jarPaths.add(dependency.getPath());
            });

            List<URL> jarUrls = new ArrayList<>();
            jarPaths.forEach(jarPath -> {
                try {
                    URL jarUrl = Path.of(jarPath).toUri().toURL();
                    jarUrls.add(jarUrl);
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            });

            URLClassLoader ucl = AccessController.doPrivileged((PrivilegedAction<URLClassLoader>) () ->
                    new URLClassLoader(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader())
            );
            InputStream resourceAsStream = ucl.getResourceAsStream(RULES_FILE);
            if (resourceAsStream == null) {
                continue;
            }

            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resourceAsStream,
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String fqn = pluginDesc.plugin().getClassName();
            List<Rule> externalRules = new ArrayList<>();
            Gson gson = new Gson();
            JsonArray ruleArray = gson.fromJson(stringBuilder.toString(), JsonArray.class);
            ruleArray.forEach(rule -> {
                JsonObject ruleObject = rule.getAsJsonObject();
                int numericId = ruleObject.get("id").getAsInt();
                RuleKind ruleKind = switch (ruleObject.get("ruleKind").getAsString()) {
                    case "BUG" -> RuleKind.BUG;
                    case "VULNERABILITY" -> RuleKind.VULNERABILITY;
                    case "CODE_SMELL" -> RuleKind.CODE_SMELL;
                    default -> null;
                };

                String description = ruleObject.get("description").getAsString();
                if (ruleKind != null) {
                    Rule inMemoryRule = RuleFactory.createRule(numericId, description, ruleKind,
                            org.value(), name.value());
                    externalRules.add(inMemoryRule);
                }
            });
            externalAnalyzers.put(fqn, externalRules);
        }
        return externalAnalyzers;
    }

    private void extractAnalyzerImportsAndDependencies(ScanTomlFile.Analyzer analyzer, StringBuilder imports,
                                                       StringBuilder dependencies, List<String> analyzerDescriptors) {
        String org = analyzer.org();
        String name = analyzer.name();
        String analyzerDescriptor = org + FORWARD_SLASH + name;
        analyzerDescriptors.add(analyzerDescriptor);
        String version = analyzer.version();
        String repository = analyzer.repository();
        buildStringWithNewLine(imports, IMPORT_PREFIX + analyzerDescriptor + USE_IMPORT_AS_UNDERSCORE);

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

    List<Issue> runExternalAnalyzers(Project project, Map<String, List<Rule>> externalAnalyzers) {
        List<ScannerContext> scannerContextList = new ArrayList<>();
        for (Map.Entry<String, List<Rule>> externalAnalyzer : externalAnalyzers.entrySet()) {
            ScannerContextImpl scannerContext = new ScannerContextImpl(externalAnalyzer.getValue());
            scannerContextList.add(scannerContext);
            Map<String, Object> pluginProperties = new HashMap<>();
            pluginProperties.put("ScannerContext", scannerContext);
            project.projectEnvironmentContext().getService(CompilerPluginCache.class)
                    .putData(externalAnalyzer.getKey(), pluginProperties);
        }

        project.currentPackage().getCompilation();
        List<Issue> externalIssues = new ArrayList<>(scannerContextList.size());
        for (ScannerContext scannerContext : scannerContextList) {
            ReporterImpl reporter = (ReporterImpl) scannerContext.getReporter();
            for (Issue issue : reporter.getIssues()) {
                if (issue.location().lineRange().fileName().equals(generatedDocumentName)) {
                    continue;
                }
                externalIssues.add(issue);
            }
        }
        return externalIssues;
    }
}
