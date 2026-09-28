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

import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Severity;
import io.ballerina.scan.Standards;

import java.util.List;

/**
 * {@code CoreRuleDefinition} is a Gson deserialization target mirroring the JSON shape of a single
 * rule entry in the bundled core rules metadata resource file (see {@code core-rules/rules.json}),
 * and is reused for external (compiler-plugin authored) rule entries as well.
 *
 * @since 0.1.0
 */
final class CoreRuleDefinition {

    private int id;
    private String kind;
    private String name;
    private String description;
    private String fullDescription;
    private String helpUri;
    private String severity;
    private List<String> tags;
    private Standards standards;

    RuleImpl.Builder toRuleBuilder() {
        return RuleImpl.builder()
                .numericId(id)
                .name(name)
                .description(description)
                .details(fullDescription)
                .helpUri(helpUri)
                .ruleKind(RuleKind.valueOf(kind))
                .severity(severity != null ? Severity.valueOf(severity) : null)
                .tags(tags != null ? tags : List.of())
                .standards(standards);
    }
}
