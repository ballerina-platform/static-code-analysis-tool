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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Severity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Static Code Analysis Rule tests.
 *
 * @since 0.1.0
 */
public class RuleImplTest {
    @Test(description = "test creating and retrieving values from a core rule")
    void testCoreRule() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        Assert.assertEquals(rule.id(), "ballerina:101");
        Assert.assertEquals(rule.numericId(), 101);
        Assert.assertEquals(rule.description(), "rule 101");
        Assert.assertEquals(rule.kind(), RuleKind.BUG);
    }

    @Test(description = "test creating and retrieving values from an external rule")
    void testExternalRule() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG,
                "exampleOrg", "exampleName");
        Assert.assertEquals(rule.id(), "exampleOrg/exampleName:101");
        Assert.assertEquals(rule.numericId(), 101);
        Assert.assertEquals(rule.description(), "rule 101");
        Assert.assertEquals(rule.kind(), RuleKind.BUG);
    }

    @Test(description = "test an external rule carrying the same rich metadata core rules support")
    void testExternalRuleWithRichMetadata() {
        String json = "{"
                + "\"id\": 1,"
                + "\"kind\": \"VULNERABILITY\","
                + "\"name\": \"Path injection\","
                + "\"description\": \"I/O calls should not be vulnerable to path injection\","
                + "\"fullDescription\": \"Passing unsanitized user input to an I/O function can let an "
                + "attacker read or write arbitrary files.\","
                + "\"severity\": \"HIGH\","
                + "\"tags\": [\"security\"],"
                + "\"standards\": {\"cwe\": [22]}"
                + "}";
        JsonObject ruleObject = JsonParser.parseString(json).getAsJsonObject();
        CoreRuleDefinition definition = new Gson().fromJson(ruleObject, CoreRuleDefinition.class);
        Rule rule = RuleFactory.createRule(definition.toRuleBuilder(), "ballerina", "io");

        Assert.assertEquals(rule.id(), "ballerina/io:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.kind(), RuleKind.VULNERABILITY);
        Assert.assertEquals(rule.name(), "Path injection");
        Assert.assertEquals(rule.description(), "I/O calls should not be vulnerable to path injection");
        Assert.assertEquals(rule.details(), "Passing unsanitized user input to an I/O function can let an "
                + "attacker read or write arbitrary files.");
        Assert.assertEquals(rule.severity(), Severity.HIGH);
        Assert.assertEquals(rule.tags(), List.of("security"));
        Assert.assertNotNull(rule.standards());
        Assert.assertEquals(rule.standards().cwe(), List.of(22));
    }

    @Test(description = "test an external rule without rich metadata falls back to description like core rules do")
    void testExternalRuleWithoutRichMetadataFallsBackToDescription() {
        String json = "{\"id\": 1, \"kind\": \"CODE_SMELL\", \"description\": \"rule 1\"}";
        JsonObject ruleObject = JsonParser.parseString(json).getAsJsonObject();
        CoreRuleDefinition definition = new Gson().fromJson(ruleObject, CoreRuleDefinition.class);
        Rule rule = RuleFactory.createRule(definition.toRuleBuilder(), "exampleOrg", "exampleName");

        Assert.assertEquals(rule.id(), "exampleOrg/exampleName:1");
        Assert.assertEquals(rule.name(), "rule 1");
        Assert.assertEquals(rule.details(), "rule 1");
        Assert.assertNull(rule.severity());
        Assert.assertTrue(rule.tags().isEmpty());
        Assert.assertNull(rule.standards());
    }
}
