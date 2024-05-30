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

package io.ballerina.scan.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code ScanTomlFile} contains an in-memory representation of the Scan.toml file.
 *
 * @since 0.1.0
 * */
public class ScanTomlFile {
    private final Set<Platform> platforms = new LinkedHashSet<>();
    private final Set<Analyzer> analyzers = new LinkedHashSet<>();
    private final Set<RuleToFilter> rulesToInclude = new LinkedHashSet<>();
    private final Set<RuleToFilter> rulesToExclude = new LinkedHashSet<>();

    ScanTomlFile() {
    }

    void setPlatform(Platform platform) {
        platforms.add(platform);
    }

    void setAnalyzer(Analyzer analyzer) {
        analyzers.add(analyzer);
    }

    void setRuleToInclude(RuleToFilter rule) {
        rulesToInclude.add(rule);
    }

    void setRuleToExclude(RuleToFilter rule) {
        rulesToExclude.add(rule);
    }

    /**
     * Returns an unmodifiable {@link Set} of platforms.
     *
     * @return an unmodifiable set of platforms
     */
    public Set<Platform> getPlatforms() {
        return Collections.unmodifiableSet(platforms);
    }

    /**
     * Returns an unmodifiable {@link Set} of analyzers.
     *
     * @return an unmodifiable set of analyzers
     */
    public Set<Analyzer> getAnalyzers() {
        return Collections.unmodifiableSet(analyzers);
    }

    /**
     * Returns an unmodifiable {@link Set} of rules to include.
     *
     * @return an unmodifiable set of rules to include
     */
    public Set<RuleToFilter> getRulesToInclude() {
        return Collections.unmodifiableSet(rulesToInclude);
    }

    /**
     * Returns an unmodifiable {@link Set} of rules to exclude.
     *
     * @return an unmodifiable set of rules to exclude
     */
    public Set<RuleToFilter> getRulesToExclude() {
        return Collections.unmodifiableSet(rulesToExclude);
    }

    /**
     * Represents a static code analysis platform.
     *
     * @param name      in-memory representation of platform name
     * @param path      in-memory representation of platform JAR path
     * @param arguments in-memory representation of platform arguments
     */
    public record Platform(String name, String path, Map<String, Object> arguments) {
        public Platform {
            arguments = Collections.unmodifiableMap(arguments);
        }
    }

    /**
     * Represents a static code analyzer.
     *
     * @param org        in-memory representation of the organization of the analyzer
     * @param name       in-memory representation of the name of the analyzer
     * @param version    in-memory representation of the version of the analyzer
     * @param repository in-memory representation of the repository of the analyzer
     */
    public record Analyzer(String org, String name, String version, String repository) { }

    /**
     * Represents a static code analysis rule to filter.
     *
     * @param id in-memory representation of the identifier of the rule to filter
     */
    public record RuleToFilter(String id) { }
}
