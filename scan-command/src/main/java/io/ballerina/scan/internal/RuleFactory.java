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

import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;

import java.util.function.IntFunction;

import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_RULE_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.FORWARD_SLASH;

/**
 * {@code RuleFactory} contains the logic to create a {@link Rule} with fully qualified identifier.
 *
 * @since 0.1.0
 * */
class RuleFactory {

    /**
     * Returns a core static code analysis {@link Rule} instance.
     *
     * @param numericId   numeric identifier of the static code analysis rule
     * @param description description of the static code analysis rule
     * @param ruleKind    {@link RuleKind} of the static code analysis rule
     *
     * @return a core static code analysis rule instance
     */
    static Rule createRule(int numericId, String description, RuleKind ruleKind) {
        return RuleImpl.builder()
                .id(coreRuleId(numericId))
                .numericId(numericId)
                .description(description)
                .ruleKind(ruleKind)
                .build();
    }

    /**
     * Returns an external static code analysis {@link Rule} instance.
     *
     * @param numericId   numeric identifier of the static code analysis rule
     * @param description description of the static code analysis rule
     * @param ruleKind    {@link RuleKind} of the static code analysis rule
     * @param org         Ballerina package organisation name of the compiler plugin
     * @param name        Ballerina package name of the compiler plugin
     *
     * @return an external static code analysis rule instance
     */
    static Rule createRule(int numericId, String description, RuleKind ruleKind, String org, String name) {
        return RuleImpl.builder()
                .id(externalRuleId(org, name, numericId))
                .numericId(numericId)
                .description(description)
                .ruleKind(ruleKind)
                .build();
    }

    /**
     * Returns a fully populated core static code analysis {@link Rule} instance, built from the
     * rich rule metadata bundled for built-in Ballerina rules.
     *
     * @param builder the rich metadata describing the core rule, staged in a {@link RuleImpl.Builder}
     * @return a core static code analysis rule instance carrying the full rule metadata
     */
    static Rule createCoreRule(RuleImpl.Builder builder) {
        return finalizeRule(builder, RuleFactory::coreRuleId);
    }

    /**
     * Returns a fully populated external static code analysis {@link Rule} instance, built from the
     * rich rule metadata a compiler plugin authored in its {@code rules.json} (see
     * {@link CoreRuleDefinition}, whose JSON shape is reused for external rules too).
     *
     * @param builder the rich metadata describing the external rule, staged in a {@link RuleImpl.Builder}
     * @param org     Ballerina package organisation name of the compiler plugin
     * @param name    Ballerina package name of the compiler plugin
     * @return an external static code analysis rule instance carrying the full rule metadata
     */
    static Rule createRule(RuleImpl.Builder builder, String org, String name) {
        return finalizeRule(builder, numericId -> externalRuleId(org, name, numericId));
    }

    /**
     * Builds the staged rule from {@code builder}, then resolves its fully qualified id (via
     * {@code idResolver}, applied to the staged rule's own numeric id). The helpUri is taken as
     * authored in the rule metadata and left {@code null} when absent.
     */
    private static Rule finalizeRule(RuleImpl.Builder builder, IntFunction<String> idResolver) {
        RuleImpl staged = builder.build();
        return staged.withId(idResolver.apply(staged.numericId()));
    }

    private static String coreRuleId(int numericId) {
        return BALLERINA_RULE_PREFIX + numericId;
    }

    private static String externalRuleId(String org, String name, int numericId) {
        return org + FORWARD_SLASH + name + ":" + numericId;
    }

    private RuleFactory() {
    }
}
