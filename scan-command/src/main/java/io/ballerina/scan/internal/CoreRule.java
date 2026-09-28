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
import io.ballerina.scan.Rule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.scan.internal.ScanToolConstants.CORE_RULES_DIRECTORY;
import static io.ballerina.scan.internal.ScanToolConstants.RULES_FILE;

/**
 * {@code CoreRule} identifies the core static code analysis rules. The rich metadata for each rule
 * (full description, tags, severity, CWE/OWASP references, etc.) is authored once in the bundled
 * {@code core-rules/rules.json} resource file, loaded a single time, and looked up here by numeric
 * id - so the metadata can be reviewed/updated without touching this wiring.
 *
 * @since 0.1.0
 * */
enum CoreRule {

    AVOID_CHECKPANIC(1),
    UNUSED_FUNCTION_PARAMETER(2),
    PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT(3),
    PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT(4),
    PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT(5),
    PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT(6),
    OPERATION_ALWAYS_EVALUATES_TO_TRUE(7),
    OPERATION_ALWAYS_EVALUATES_TO_FALSE(8),
    OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE(9),
    SELF_ASSIGNMENT(10),
    UNUSED_PRIVATE_CLASS_FIELD(11),
    INVALID_RANGE_EXPRESSION(12),
    HARD_CODED_SECRET(13),
    NON_CONFIGURABLE_SECRET(14);

    private final int numericId;

    CoreRule(int numericId) {
        this.numericId = numericId;
    }

    Rule rule() {
        return CoreRulesEngine.getRule(numericId);
    }

    static List<Rule> rules() {
        List<Rule> coreRules = new ArrayList<>();
        for (CoreRule coreRule: CoreRule.values()) {
            coreRules.add(coreRule.rule());
        }
        return coreRules;
    }

    private static final class CoreRulesEngine {

        private static final Map<Integer, Rule> RULES_BY_ID = loadRules();

        private static Rule getRule(int numericId) {
            Rule rule = RULES_BY_ID.get(numericId);
            if (rule == null) {
                throw new IllegalStateException("Missing core rule metadata for id: " + numericId);
            }
            return rule;
        }

        private static Map<Integer, Rule> loadRules() {
            String resourcePath = CORE_RULES_DIRECTORY + RULES_FILE;
            try (InputStream input = CoreRulesEngine.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (input == null) {
                    throw new IllegalStateException("Missing core rules metadata resource: " + resourcePath);
                }
                String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                CoreRuleDefinition[] definitions = new Gson().fromJson(content, CoreRuleDefinition[].class);
                Map<Integer, Rule> rulesById = new HashMap<>();
                for (CoreRuleDefinition definition : definitions) {
                    Rule rule = RuleFactory.createCoreRule(definition.toRuleBuilder());
                    rulesById.put(rule.numericId(), rule);
                }
                return rulesById;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load core rules metadata: " + resourcePath, ex);
            }
        }

        private CoreRulesEngine() {
        }
    }
}
