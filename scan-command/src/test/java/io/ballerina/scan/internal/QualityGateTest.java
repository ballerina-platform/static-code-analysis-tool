/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

import io.ballerina.scan.Issue;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.GateSeverity;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.util.List;
import java.util.Optional;

/**
 * Gate evaluation tests.
 *
 * @since 0.11.1
 */
public class QualityGateTest {
    private static final BLangDiagnosticLocation LOCATION = new BLangDiagnosticLocation("main.bal", 0, 0, 0, 1, 0,
            1);

    @Test(description = "test the gate is off when no setting is given or the severity is NONE")
    void testGateOff() {
        Assert.assertTrue(QualityGate.of(null, null).isOff());
        Assert.assertTrue(QualityGate.of(GateSeverity.NONE, 5).isOff());
        Assert.assertEquals(QualityGate.of(GateSeverity.NONE, null).evaluate(List.of(issue(RuleKind.BUG))),
                Optional.empty());
    }

    @Test(description = "test a severity without a count fails on the first issue at or above it")
    void testSeverityWithoutCount() {
        QualityGate gate = QualityGate.of(GateSeverity.HIGH, null);
        Assert.assertEquals(gate.evaluate(List.of(issue(RuleKind.VULNERABILITY), issue(RuleKind.CODE_SMELL))),
                Optional.empty());
        Assert.assertEquals(gate.evaluate(List.of(issue(RuleKind.BUG), issue(RuleKind.VULNERABILITY))),
                Optional.of("1 issue at or above HIGH, limit 1 (HIGH: 1)"));
        Assert.assertEquals(QualityGate.of(GateSeverity.BLOCKER, null).evaluate(List.of(issue(RuleKind.BUG))),
                Optional.empty(), "no rule kind maps to BLOCKER");
    }

    @Test(description = "test a count without a severity counts every severity, failing at the count")
    void testCountWithoutSeverity() {
        QualityGate gate = QualityGate.of(null, 3);
        Assert.assertEquals(gate.evaluate(List.of(issue(RuleKind.CODE_SMELL), issue(RuleKind.CODE_SMELL))),
                Optional.empty());
        Assert.assertEquals(gate.evaluate(List.of(issue(RuleKind.CODE_SMELL), issue(RuleKind.VULNERABILITY),
                        issue(RuleKind.BUG))),
                Optional.of("3 issues at or above INFO, limit 3 (HIGH: 1, MEDIUM: 1, LOW: 1)"));
    }

    @Test(description = "test the gate counts only the issues at or above the severity, each kind by its severity")
    void testSeverityAndCount() {
        QualityGate gate = QualityGate.of(GateSeverity.MEDIUM, 2);
        Assert.assertEquals(gate.evaluate(List.of(issue(RuleKind.VULNERABILITY), issue(RuleKind.CODE_SMELL))),
                Optional.empty());
        Assert.assertEquals(gate.evaluate(List.of(issue(RuleKind.VULNERABILITY), issue(RuleKind.BUG),
                        issue(RuleKind.CODE_SMELL))),
                Optional.of("2 issues at or above MEDIUM, limit 2 (HIGH: 1, MEDIUM: 1)"));
        Assert.assertEquals(QualityGate.of(GateSeverity.LOW, null).evaluate(List.of(issue(RuleKind.BUG),
                        issue(RuleKind.VULNERABILITY), issue(RuleKind.CODE_SMELL))),
                Optional.of("3 issues at or above LOW, limit 1 (HIGH: 1, MEDIUM: 1, LOW: 1)"));
    }

    private static Issue issue(RuleKind kind) {
        return new IssueImpl(LOCATION, RuleFactory.createRule(1, "rule", kind), Source.BUILT_IN, "main.bal",
                "main.bal");
    }
}
