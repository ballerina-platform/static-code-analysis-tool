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

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
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
    private boolean suppressionEnabled = true;
    private boolean suppressionReasonRequired;
    private boolean suppressionRuleIdRequired;
    private String failOnSeverity;
    private Integer failOnCount;
    private Path path;
    private String remoteLocation;

    ScanTomlFile() {
    }

    void setRemoteLocation(String remoteLocation) {
        this.remoteLocation = remoteLocation;
    }

    void setFailOnSeverity(String severity) {
        failOnSeverity = severity;
    }

    void setFailOnCount(Integer count) {
        failOnCount = count;
    }

    void setPath(Path path) {
        this.path = path;
    }

    /**
     * Returns the severity set by {@code [gate] failOnSeverity}, in upper case.
     *
     * @return a severity name or {@code NONE}, or empty when not set
     * @since 0.13.0
     */
    public Optional<String> getFailOnSeverity() {
        return Optional.ofNullable(failOnSeverity);
    }

    /**
     * Returns the count set by {@code [gate] failOnCount}.
     *
     * @return a count of 1 or more, or empty when not set
     * @since 0.13.0
     */
    public Optional<Integer> getFailOnCount() {
        return Optional.ofNullable(failOnCount);
    }

    /**
     * Returns whether the {@code [gate]} table sets either setting.
     *
     * @return true if {@code failOnSeverity} or {@code failOnCount} is set
     * @since 0.13.0
     */
    public boolean hasGate() {
        return failOnSeverity != null || failOnCount != null;
    }

    /**
     * Returns the path of the file the configurations were read from, which is the cached copy for a remote file.
     *
     * @return the path, or empty when no Scan.toml file was read
     * @since 0.13.0
     */
    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    /**
     * Returns the location of the file as shown to the user: the URL of a remote file, or the path relative to a
     * root.
     *
     * @param root the directory a local path is shown relative to
     * @return the location, or empty when no Scan.toml file was read
     * @since 0.13.0
     */
    public Optional<String> getDisplayPath(Path root) {
        if (remoteLocation != null) {
            return Optional.of(remoteLocation);
        }
        return getPath().map(file -> ScanUtils.displayPath(root, file));
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

    void setSuppressionEnabled(boolean enabled) {
        suppressionEnabled = enabled;
    }

    void setSuppressionReasonRequired(boolean required) {
        suppressionReasonRequired = required;
    }

    void setSuppressionRuleIdRequired(boolean required) {
        suppressionRuleIdRequired = required;
    }

    /**
     * Returns whether {@code @scan:Suppress} annotations are applied, as set by {@code [suppression] enabled}.
     * Disabling them reports every issue, as {@code --ignore-suppressions} does for a single run.
     *
     * @return false if suppressions are disregarded for the project
     * @since 0.13.0
     */
    public boolean isSuppressionEnabled() {
        return suppressionEnabled;
    }

    /**
     * Returns whether every {@code @scan:Suppress} annotation must give a reason, as set by
     * {@code [suppression] requireReason}.
     *
     * @return true if a suppression without a reason fails the scan
     * @since 0.13.0
     */
    public boolean isSuppressionReasonRequired() {
        return suppressionReasonRequired;
    }

    /**
     * Returns whether every {@code @scan:Suppress} annotation must name the rules it suppresses, as set by
     * {@code [suppression] requireRuleId}.
     *
     * @return true if a suppression naming no rule fails the scan
     * @since 0.13.0
     */
    public boolean isSuppressionRuleIdRequired() {
        return suppressionRuleIdRequired;
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
