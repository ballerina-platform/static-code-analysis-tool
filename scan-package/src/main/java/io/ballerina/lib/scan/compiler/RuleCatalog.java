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
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.internal.model.CompilerPluginDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * The rules {@code bal scan} can report for a package, discovered the same way the scan tool discovers them: the
 * core rules shipped with the tool, plus the {@code rules.json} of every direct dependency's compiler plugin.
 *
 * @since 0.13.0
 */
final class RuleCatalog {

    static final String CORE_PROVIDER = "ballerina";
    private static final String CORE_RULES_RESOURCE = "core-rules/rules.json";
    private static final String PLUGIN_RULES_FILE = "rules.json";
    private static final String RULE_ID_FIELD = "id";
    private static final Set<Integer> CORE_RULES = loadCoreRules();

    private final Map<String, Set<Integer>> rulesByProvider;

    private RuleCatalog(Map<String, Set<Integer>> rulesByProvider) {
        this.rulesByProvider = rulesByProvider;
    }

    static RuleCatalog of(Package currentPackage) {
        Map<String, Set<Integer>> rulesByProvider = new HashMap<>();
        rulesByProvider.put(CORE_PROVIDER, CORE_RULES);
        ResolvedPackageDependency root = new ResolvedPackageDependency(currentPackage, PackageDependencyScope.DEFAULT);
        for (ResolvedPackageDependency dependency : currentPackage.getResolution().dependencyGraph()
                .getDirectDependencies(root)) {
            PackageManifest manifest = dependency.packageInstance().manifest();
            manifest.compilerPluginDescriptor().flatMap(RuleCatalog::readPluginRules).ifPresent(rules ->
                    rulesByProvider.put(manifest.org().value() + "/" + manifest.name().value(), rules));
        }
        return new RuleCatalog(rulesByProvider);
    }

    /**
     * Returns the rule numbers the provider declares, or empty when the provider's rules are not available to this
     * compilation, because it is not a direct dependency or has no scan rules.
     *
     * @param provider {@code ballerina} for core rules, or {@code <org>/<package>}
     * @return the rule numbers of the provider
     */
    Optional<Set<Integer>> rulesOf(String provider) {
        return Optional.ofNullable(rulesByProvider.get(provider));
    }

    private static Optional<Set<Integer>> readPluginRules(CompilerPluginDescriptor descriptor) {
        for (CompilerPluginDescriptor.Dependency jar : descriptor.dependencies()) {
            try (JarFile jarFile = new JarFile(Path.of(jar.getPath()).toFile())) {
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

    private static Set<Integer> loadCoreRules() {
        try (InputStream stream = RuleCatalog.class.getClassLoader().getResourceAsStream(CORE_RULES_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(CORE_RULES_RESOURCE + " is missing from the scan compiler plugin");
            }
            return parseRuleIds(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + CORE_RULES_RESOURCE, e);
        }
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
