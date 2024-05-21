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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Core static code analysis rules tests.
 *
 * @since 0.1.0
 */
public class CoreRulesTest {
    @Test(description = "test core rules")
    void testCoreRules() {
        CoreRules coreRules = new CoreRules();
        Map<Integer, Rule> rules = new HashMap<>();
        coreRules.getCoreRules().forEach(rule -> {
            rules.put(rule.numericId(), rule);
        });
        Rule ruleCheckPanic = rules.get(1);
        Assert.assertEquals(ruleCheckPanic.id(), "B1");
        Assert.assertEquals(ruleCheckPanic.numericId(), 1);
        Assert.assertEquals(ruleCheckPanic.description(), "Should avoid checkpanic");
        Assert.assertEquals(ruleCheckPanic.kind(), RuleKind.CODE_SMELL);
    }
}
