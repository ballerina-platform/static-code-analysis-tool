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

public class ScanTomlFile {
    private final Set<Platform> platforms;
    private final Set<Analyzer> analyzers;
    private final Set<RuleToFilter> rulesToInclude;
    private final Set<RuleToFilter> rulesToExclude;

    ScanTomlFile() {
        this.platforms = new LinkedHashSet<>();
        this.analyzers = new LinkedHashSet<>();
        this.rulesToInclude = new LinkedHashSet<>();
        this.rulesToExclude = new LinkedHashSet<>();
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

    public Set<Platform> getPlatforms() {
        return Collections.unmodifiableSet(platforms);
    }

    public Set<Analyzer> getAnalyzers() {
        return Collections.unmodifiableSet(analyzers);
    }

    public Set<RuleToFilter> getRulesToInclude() {
        return Collections.unmodifiableSet(rulesToInclude);
    }

    public Set<RuleToFilter> getRulesToExclude() {
        return Collections.unmodifiableSet(rulesToExclude);
    }

    public static class Platform {
        private String name;
        private String path;
        private Map<String, Object> arguments;

        Platform(String name, String path, Map<String, Object> arguments) {
            this.name = name;
            this.path = path;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public Map<String, Object> getArguments() {
            return Collections.unmodifiableMap(arguments);
        }
    }

    public static class Analyzer {
        private String org;
        private String name;
        private String version;
        private String repository;

        Analyzer(String org, String name, String version, String repository) {
            this.org = org;
            this.name = name;
            this.version = version;
            this.repository = repository;
        }

        public String getOrg() {
            return org;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getRepository() {
            return repository;
        }
    }

    public static class RuleToFilter {
        private String id;

        RuleToFilter(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
