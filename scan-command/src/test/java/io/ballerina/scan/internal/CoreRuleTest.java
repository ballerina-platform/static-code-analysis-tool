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

/**
 * Core static code analysis rules tests.
 *
 * @since 0.1.0
 */
public class CoreRuleTest {
    public static final String AVOID_CHECKPANIC = "Avoid checkpanic";
    public static final String UNUSED_FUNCTION_PARAMETER = "Unused function parameter";

    @Test(description = "test all rules")
    void testAllRules() {
        Assert.assertEquals(CoreRule.rules().size(), 2);
    }

    @Test(description = "test checkpanic rule")
    void testCheckpanicRule() {
        Rule rule = CoreRule.AVOID_CHECKPANIC.rule();
        Assert.assertEquals(rule.id(), "ballerina:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), AVOID_CHECKPANIC);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test unused function parameters test")
    void testUnusedFunctionParameterRule() {
        Rule rule = CoreRule.UNUSED_FUNCTION_PARAMETER.rule();
        Assert.assertEquals(rule.id(), "ballerina:2");
        Assert.assertEquals(rule.numericId(), 2);
        Assert.assertEquals(rule.description(), UNUSED_FUNCTION_PARAMETER);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }
}
