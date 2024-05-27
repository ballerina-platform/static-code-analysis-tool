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
}
