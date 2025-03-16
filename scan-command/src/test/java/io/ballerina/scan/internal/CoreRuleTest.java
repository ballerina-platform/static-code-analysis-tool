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
    public static final String OPERATION_ALWAYS_EVALUATES_TO_TRUE = "This operation always evaluates to true";
    public static final String OPERATION_ALWAYS_EVALUATES_TO_FALSE = "This operation always evaluates to false";
    public static final String OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE =
            "This operation always evaluates to the same value";
    public static final String SELF_ASSIGNMENT = "This variable is assigned to itself";
    public static final String PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT =
            "Non isolated public function";
    public static final String PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT =
            "Non isolated public method";
    public static final String PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT =
            "Non isolated public class";
    public static final String PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT =
            "Non isolated public object";

    @Test(description = "test all rules")
    void testAllRules() {
        Assert.assertEquals(CoreRule.rules().size(), 11);
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

    @Test(description = "test unused class fields rule")
    void testUnusedClassFieldsRule() {
        Rule rule = CoreRule.UNUSED_PRIVATE_CLASS_FIELD.rule();
        Assert.assertEquals(rule.id(), "ballerina:11");
        Assert.assertEquals(rule.numericId(), 11);
        Assert.assertEquals(rule.description(), "Unused class private fields");
    }
    
    @Test(description = "test always true evaluates")
    void testTrueEvaluates() {
        Rule rule = CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE.rule();
        Assert.assertEquals(rule.id(), "ballerina:7");
        Assert.assertEquals(rule.numericId(), 7);
        Assert.assertEquals(rule.description(), OPERATION_ALWAYS_EVALUATES_TO_TRUE);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test always false evaluates")
    void testFalseEvaluates() {
        Rule rule = CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE.rule();
        Assert.assertEquals(rule.id(), "ballerina:8");
        Assert.assertEquals(rule.numericId(), 8);
        Assert.assertEquals(rule.description(), OPERATION_ALWAYS_EVALUATES_TO_FALSE);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test evaluate to the same value")
    void testSelfEvaluates() {
        Rule rule = CoreRule.OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE.rule();
        Assert.assertEquals(rule.id(), "ballerina:9");
        Assert.assertEquals(rule.numericId(), 9);
        Assert.assertEquals(rule.description(), OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test self assignment")
    void testSelfAssignmentAnalyzer() {
        Rule rule = CoreRule.SELF_ASSIGNMENT.rule();
        Assert.assertEquals(rule.id(), "ballerina:10");
        Assert.assertEquals(rule.numericId(), 10);
        Assert.assertEquals(rule.description(), SELF_ASSIGNMENT);
    }

    @Test(description = "test non isolated public functions")
    void testNonIsolatedPublicFunctionConstructsRule() {
        Rule rule = CoreRule.PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT.rule();
        Assert.assertEquals(rule.id(), "ballerina:3");
        Assert.assertEquals(rule.numericId(), 3);
        Assert.assertEquals(rule.description(), PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test non isolated public methods")
    void testNonIsolatedPublicMethodConstructsRule() {
        Rule rule = CoreRule.PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT.rule();
        Assert.assertEquals(rule.id(), "ballerina:4");
        Assert.assertEquals(rule.numericId(), 4);
        Assert.assertEquals(rule.description(), PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test non isolated public classes")
    void testNonIsolatedPublicClassConstructsRule() {
        Rule rule = CoreRule.PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT.rule();
        Assert.assertEquals(rule.id(), "ballerina:5");
        Assert.assertEquals(rule.numericId(), 5);
        Assert.assertEquals(rule.description(), PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "test non isolated public objects")
    void testNonIsolatedPublicObjectConstructsRule() {
        Rule rule = CoreRule.PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT.rule();
        Assert.assertEquals(rule.id(), "ballerina:6");
        Assert.assertEquals(rule.numericId(), 6);
        Assert.assertEquals(rule.description(), PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT);
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }
}
