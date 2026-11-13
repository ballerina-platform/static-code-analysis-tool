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

import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code ScanTomlFile} contains an in-memory representation of the Scan.toml file.
 *
 * @since 0.1.0
 * */
public class ScanTomlFile {
    public static final String RULE_INCLUDE_SETTING = "[rule] include";
    public static final String RULE_EXCLUDE_SETTING = "[rule] exclude";
    public static final String SUPPRESSION_ENABLED_SETTING = "[suppression] enabled";
    public static final String SUPPRESSION_REQUIRE_REASON_SETTING = "[suppression] requireReason";
    public static final String SUPPRESSION_REQUIRE_RULE_ID_SETTING = "[suppression] requireRuleId";
    public static final String GATE_FAIL_ON_SEVERITY_SETTING = "[gate] failOnSeverity";
    public static final String GATE_FAIL_ON_COUNT_SETTING = "[gate] failOnCount";
    public static final String ANALYSIS_EXCLUDE_TESTS_SETTING = "[analysis] excludeTests";
    private static final String ANALYZER_SETTING = "[[analyzer]] ";

    private final Set<Platform> platforms = new LinkedHashSet<>();
    private final Set<Analyzer> analyzers = new LinkedHashSet<>();
    private final Set<RuleToFilter> rulesToInclude = new LinkedHashSet<>();
    private final Set<RuleToFilter> rulesToExclude = new LinkedHashSet<>();
    private final Map<String, LineRange> ruleLocations = new LinkedHashMap<>();
    private final Map<Analyzer, LineRange> analyzerLocations = new LinkedHashMap<>();
    private final Set<String> platformsWithoutPath = new LinkedHashSet<>();
    // Each setting the file writes, keyed by its name, with its value as written in TOML
    private final Map<String, String> writtenSettings = new LinkedHashMap<>();
    private boolean suppressionEnabled = true;
    private boolean suppressionReasonRequired;
    private boolean suppressionRuleIdRequired;
    private GateSeverity failOnSeverity;
    private Integer failOnCount;
    private boolean excludeTests;
    private Path path;
    private String remoteLocation;

    ScanTomlFile() {
    }

    void setRemoteLocation(String remoteLocation) {
        this.remoteLocation = remoteLocation;
    }

    void setFailOnSeverity(GateSeverity severity) {
        failOnSeverity = severity;
        writtenSettings.put(GATE_FAIL_ON_SEVERITY_SETTING, "\"" + severity + "\"");
    }

    void setFailOnCount(Integer count) {
        failOnCount = count;
        writtenSettings.put(GATE_FAIL_ON_COUNT_SETTING, String.valueOf(count));
    }

    void setPath(Path path) {
        this.path = path;
    }

    void writeExcludeTests(boolean excludeTests) {
        this.excludeTests = excludeTests;
        writtenSettings.put(ANALYSIS_EXCLUDE_TESTS_SETTING, String.valueOf(excludeTests));
    }

    /**
     * Records the rule IDs of include or exclude as written in the file.
     */
    void writeRules(boolean include, Collection<String> ruleIds) {
        writtenSettings.put(include ? RULE_INCLUDE_SETTING : RULE_EXCLUDE_SETTING, quotedList(ruleIds));
    }

    /**
     * Returns whether the test sources are left out of the scan, as set by {@code [analysis] excludeTests}.
     *
     * @return true if the test sources are not scanned
     * @since 0.11.1
     */
    public boolean isExcludeTests() {
        return excludeTests;
    }

    /**
     * Returns each setting the file writes explicitly, in the order it was read. The key names the setting, such as
     * {@link #GATE_FAIL_ON_COUNT_SETTING} or {@code [[analyzer]] org/name}, and the value is as written in TOML,
     * such as {@code 2} or {@code ["ballerina:1"]}; an analyzer entry has an empty value. A setting left at its default
     * because the file does not write it is not included.
     *
     * @return an unmodifiable map of the written settings
     * @since 0.11.1
     */
    public Map<String, String> getWrittenSettings() {
        return Collections.unmodifiableMap(writtenSettings);
    }

    /**
     * Returns the value of a setting as written in the file.
     *
     * @param setting the name of the setting, such as {@link #GATE_FAIL_ON_COUNT_SETTING}
     * @return the value as written in TOML, or empty when the file does not write the setting
     * @since 0.11.1
     */
    public Optional<String> getWrittenValue(String setting) {
        return Optional.ofNullable(writtenSettings.get(setting));
    }

    /**
     * Returns the severity set by {@code [gate] failOnSeverity}.
     *
     * @return the gate severity, or empty when not set
     * @since 0.11.1
     */
    public Optional<GateSeverity> getFailOnSeverity() {
        return Optional.ofNullable(failOnSeverity);
    }

    /**
     * Returns the count set by {@code [gate] failOnCount}.
     *
     * @return a count of 1 or more, or empty when not set
     * @since 0.11.1
     */
    public Optional<Integer> getFailOnCount() {
        return Optional.ofNullable(failOnCount);
    }

    /**
     * Returns the path of the file the configurations were read from, which is the cached copy for a remote file.
     *
     * @return the path, or empty when no Scan.toml file was read
     * @since 0.11.1
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
     * @since 0.11.1
     */
    public Optional<String> getDisplayPath(Path root) {
        if (remoteLocation != null) {
            return Optional.of(remoteLocation);
        }
        return getPath().map(file -> ScanUtils.displayPath(root, file));
    }

    /**
     * Formats values as a TOML array of strings, such as {@code ["a", "b"]}.
     *
     * @param values the values
     * @return the TOML array
     * @since 0.11.1
     */
    public static String quotedList(Collection<String> values) {
        return values.stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(", ", "[", "]"));
    }

    void setPlatform(Platform platform) {
        platforms.add(platform);
    }

    /**
     * Adds an analyzer, and records it as written in the file.
     */
    void setAnalyzer(Analyzer analyzer) {
        analyzers.add(analyzer);
        String id = analyzer.org() + "/" + analyzer.name()
                + (analyzer.version() == null ? "" : ":" + analyzer.version());
        writtenSettings.put(ANALYZER_SETTING + id, "");
    }

    void setAnalyzer(Analyzer analyzer, LineRange location) {
        setAnalyzer(analyzer);
        analyzerLocations.putIfAbsent(analyzer, location);
    }

    void setRuleToInclude(RuleToFilter rule) {
        rulesToInclude.add(rule);
    }

    void setRuleToExclude(RuleToFilter rule) {
        rulesToExclude.add(rule);
    }

    void setRuleLocation(String ruleId, LineRange location) {
        ruleLocations.putIfAbsent(ruleId, location);
    }

    void addPlatformWithoutPath(String name) {
        platformsWithoutPath.add(name);
    }

    /**
     * Returns where a rule of the [rule] table is written in the file.
     *
     * @param ruleId the rule ID
     * @return the location of its first entry, or empty when the file does not name the rule
     * @since 0.11.2
     */
    public Optional<LineRange> getRuleLocation(String ruleId) {
        return Optional.ofNullable(ruleLocations.get(ruleId));
    }

    /**
     * Returns where an analyzer is written in the file.
     *
     * @param analyzer the analyzer
     * @return the location of its [[analyzer]] table, or empty when the file does not declare it
     * @since 0.11.2
     */
    public Optional<LineRange> getAnalyzerLocation(Analyzer analyzer) {
        return Optional.ofNullable(analyzerLocations.get(analyzer));
    }

    /**
     * Returns the names of the [[platform]] entries that have no {@code path}, which are not loaded as platforms.
     *
     * @return an unmodifiable set of platform names
     * @since 0.11.2
     */
    public Set<String> getPlatformsWithoutPath() {
        return Collections.unmodifiableSet(platformsWithoutPath);
    }

    void setSuppressionEnabled(boolean enabled) {
        suppressionEnabled = enabled;
        writtenSettings.put(SUPPRESSION_ENABLED_SETTING, String.valueOf(enabled));
    }

    void setSuppressionReasonRequired(boolean required) {
        suppressionReasonRequired = required;
        writtenSettings.put(SUPPRESSION_REQUIRE_REASON_SETTING, String.valueOf(required));
    }

    void setSuppressionRuleIdRequired(boolean required) {
        suppressionRuleIdRequired = required;
        writtenSettings.put(SUPPRESSION_REQUIRE_RULE_ID_SETTING, String.valueOf(required));
    }

    /**
     * Returns whether {@code @scan:Suppress} annotations are applied, as set by {@code [suppression] enabled}.
     * Disabling them reports every issue, as {@code --ignore-suppressions} does for a single run.
     *
     * @return false if suppressions are disregarded for the project
     * @since 0.11.1
     */
    public boolean isSuppressionEnabled() {
        return suppressionEnabled;
    }

    /**
     * Returns whether every {@code @scan:Suppress} annotation must give a reason, as set by
     * {@code [suppression] requireReason}.
     *
     * @return true if a suppression without a reason fails the scan
     * @since 0.11.1
     */
    public boolean isSuppressionReasonRequired() {
        return suppressionReasonRequired;
    }

    /**
     * Returns whether every {@code @scan:Suppress} annotation must name the rules it suppresses, as set by
     * {@code [suppression] requireRuleId}.
     *
     * @return true if a suppression naming no rule fails the scan
     * @since 0.11.1
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
     * @param path      the absolute path of a local platform JAR, or the URL of a remote one, which is downloaded
     *                  only when the platform is selected with {@code --platforms}
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
