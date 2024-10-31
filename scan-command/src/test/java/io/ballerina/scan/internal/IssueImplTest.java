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
import io.ballerina.scan.Source;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

/**
 * Static Code Analysis Issue tests.
 *
 * @since 0.1.0
 */
public class IssueImplTest {
    @Test(description = "test creating and retrieving values from a static code analysis issue")
    void testIssue() {
        Rule rule = RuleFactory.createRule(101, "rule 101", RuleKind.BUG);
        BLangDiagnosticLocation location = new BLangDiagnosticLocation("main.bal", 16, 23,
                17, 1, 748, 4);
        IssueImpl issue = new IssueImpl(location, rule, Source.BUILT_IN, "main.bal",
                "valid_bal_project/main.bal");
        Assert.assertEquals(issue.location().lineRange().startLine().line(), 17);
        Assert.assertEquals(issue.location().lineRange().endLine().line(), 24);
        Assert.assertEquals(issue.location().lineRange().startLine().offset(), 18);
        Assert.assertEquals(issue.location().lineRange().endLine().offset(), 2);
        Assert.assertEquals(issue.rule(), rule);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
        Assert.assertEquals(issue.fileName(), "main.bal");
        Assert.assertEquals(issue.filePath(), "valid_bal_project/main.bal");
    }
}
