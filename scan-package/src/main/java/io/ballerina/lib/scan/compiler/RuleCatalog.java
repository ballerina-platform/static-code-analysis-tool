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

package io.ballerina.lib.scan.compiler;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDependencyScope;
import io.ballerina.projects.PackageManifest;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.ResolvedPackageDependency;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * The analyzer rules {@code bal scan} can report for a package, discovered the same way the scan tool discovers them:
 * from the {@code rules.json} of every direct dependency's compiler plugin. Core rules are not listed, because they
 * ship with the scan tool, which checks them when it runs.
 *
 * @since 1.0.0
 */
final class RuleCatalog {

    static final String CORE_PROVIDER = "ballerina";
    private static final String PLUGIN_RULES_FILE = "rules.json";
    private static final String BALA_PLUGIN_DIRECTORY = "compiler-plugin";
    private static final String BALA_PLUGIN_LIBS_DIRECTORY = "libs";
    private static final String PLUGIN_TOML_DEPENDENCY = "dependency";
    private static final String PLUGIN_TOML_PATH = "path";
    private static final String RULE_ID_FIELD = "id";

    private final Map<String, Set<Integer>> rulesByProvider;

    private RuleCatalog(Map<String, Set<Integer>> rulesByProvider) {
        this.rulesByProvider = rulesByProvider;
    }

    static RuleCatalog of(Package currentPackage) {
        Map<String, Set<Integer>> rulesByProvider = new HashMap<>();
        ResolvedPackageDependency root = new ResolvedPackageDependency(currentPackage, PackageDependencyScope.DEFAULT);
        for (ResolvedPackageDependency dependency : currentPackage.getResolution().dependencyGraph()
                .getDirectDependencies(root)) {
            Package dependencyPackage = dependency.packageInstance();
            PackageManifest manifest = dependencyPackage.manifest();
            readPluginRules(pluginJars(dependencyPackage)).ifPresent(rules ->
                    rulesByProvider.put(manifest.org().value() + "/" + manifest.name().value(), rules));
        }
        return new RuleCatalog(rulesByProvider);
    }

    /**
     * Returns the rule numbers the provider declares, or empty when the provider's rules are not available to this
     * compilation, because it is not a direct dependency or has no scan rules.
     *
     * @param provider {@code <org>/<package>}
     * @return the rule numbers of the provider
     */
    Optional<Set<Integer>> rulesOf(String provider) {
        return Optional.ofNullable(rulesByProvider.get(provider));
    }

    // A bala keeps its compiler plugin's jars under compiler-plugin/libs; a source package lists them in
    // CompilerPlugin.toml, relative to the package root.
    private static List<Path> pluginJars(Package dependency) {
        Path packageRoot = dependency.project().sourceRoot();
        if (dependency.project().kind() == ProjectKind.BALA_PROJECT) {
            Path libs = packageRoot.resolve(BALA_PLUGIN_DIRECTORY).resolve(BALA_PLUGIN_LIBS_DIRECTORY);
            if (!Files.isDirectory(libs)) {
                return List.of();
            }
            try (Stream<Path> jars = Files.list(libs)) {
                return jars.filter(jar -> jar.toString().endsWith(".jar")).sorted().toList();
            } catch (IOException e) {
                return List.of();
            }
        }
        return dependency.compilerPluginToml()
                .map(toml -> toml.tomlAstNode().toNativeObject().get(PLUGIN_TOML_DEPENDENCY))
                .filter(List.class::isInstance)
                .map(entries -> ((List<?>) entries).stream()
                        .filter(Map.class::isInstance)
                        .map(entry -> ((Map<?, ?>) entry).get(PLUGIN_TOML_PATH))
                        .filter(String.class::isInstance)
                        .map(path -> packageRoot.resolve((String) path).normalize())
                        .toList())
                .orElse(List.of());
    }

    private static Optional<Set<Integer>> readPluginRules(List<Path> jars) {
        for (Path jar : jars) {
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                ZipEntry entry = jarFile.getEntry(PLUGIN_RULES_FILE);
                if (entry != null) {
                    return Optional.of(parseRuleIds(jarFile.getInputStream(entry)));
                }
            } catch (IOException | RuntimeException e) {
                // An unreadable plugin contributes no rules, so its rule IDs are reported as unverifiable.
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Set<Integer> parseRuleIds(InputStream stream) throws IOException {
        Set<Integer> ids = new HashSet<>();
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for (JsonElement rule : JsonParser.parseReader(reader).getAsJsonArray()) {
                ids.add(rule.getAsJsonObject().get(RULE_ID_FIELD).getAsInt());
            }
        }
        return Set.copyOf(ids);
    }
}
