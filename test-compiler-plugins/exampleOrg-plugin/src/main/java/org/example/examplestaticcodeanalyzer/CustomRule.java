package org.example.examplestaticcodeanalyzer;

import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;

import static io.ballerina.scan.RuleKind.BUG;
import static io.ballerina.scan.RuleKind.CODE_SMELL;
import static io.ballerina.scan.RuleKind.VULNERABILITY;

public enum CustomRule implements Rule {
    RULE_1(1, "rule 1", CODE_SMELL),
    RULE_2(2, "rule 2",
            BUG),
    RULE_3(3, "rule 3", VULNERABILITY);

    private final int id;
    private final String description;
    private final RuleKind kind;

    CustomRule(int id, String description, RuleKind kind) {
        this.id = id;
        this.description = description;
        this.kind = kind;
    }

    @Override
    public String id() {
        return "";
    }

    @Override
    public int numericId() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public RuleKind kind() {
        return kind;
    }
}
