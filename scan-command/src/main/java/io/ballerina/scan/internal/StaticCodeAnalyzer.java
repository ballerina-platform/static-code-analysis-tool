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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.scan.ScannerContext;

import java.util.Optional;

/**
 * {@code StaticCodeAnalyzer} contains the logic to perform core static code analysis on Ballerina documents.
 *
 * @since 0.1.0
 * */
class StaticCodeAnalyzer extends NodeVisitor {
    private final Document document;
    private final SyntaxTree syntaxTree;
    private final ScannerContext scannerContext;
    private final SemanticModel semanticModel;

    StaticCodeAnalyzer(Document document, ScannerContextImpl scannerContext) {
        this.document = document;
        this.syntaxTree = document.syntaxTree();
        this.scannerContext = scannerContext;
        semanticModel = document.module().getCompilation().getSemanticModel();
    }

    void analyze() {
        this.visit((ModulePartNode) syntaxTree.rootNode());
    }

    /**
     * Visits check expressions in a Ballerina document and perform static code analysis.
     *
     * @param checkExpressionNode node that represents a check expression
     */
    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        if (checkExpressionNode.checkKeyword().kind().equals(SyntaxKind.CHECKPANIC_KEYWORD)) {
            scannerContext.getReporter().reportIssue(document, checkExpressionNode.location(),
                    CoreRule.AVOID_CHECKPANIC.rule());
        }
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        modulePartNode.members().forEach(member -> member.accept(this));
    }

    @Override
    public void visit(BinaryExpressionNode binaryExpressionNode) {
        isSameOperandRestrictedInOperator(binaryExpressionNode.operator()).ifPresent(rule -> {
            checkSameUsageInBinaryOperator(binaryExpressionNode.lhsExpr(),
                    binaryExpressionNode.rhsExpr(), rule, binaryExpressionNode.location());
        });
    }

    private void checkSameUsageInBinaryOperator(Node lhs, Node rhs, CoreRule rule, NodeLocation location) {
        if (isSameSimpleExpression(lhs, rhs)) {
            scannerContext.getReporter().reportIssue(document, location, rule.rule());
        }
    }

    private boolean isSameSimpleExpression(Node n1, Node n2) {
        if (n1 instanceof SimpleNameReferenceNode lhsExp && n2 instanceof SimpleNameReferenceNode rhsExpr) {
            return lhsExp.name().text().equals(rhsExpr.name().text());
        }

        if (n1 instanceof QualifiedNameReferenceNode lhsExp && n2 instanceof QualifiedNameReferenceNode rhsExpr) {
            return lhsExp.modulePrefix() != null && rhsExpr.modulePrefix() != null
                    && lhsExp.modulePrefix().text().equals(rhsExpr.modulePrefix().text())
                    && lhsExp.identifier().text().equals(rhsExpr.identifier().text());
        }

        if (n1 instanceof FieldAccessExpressionNode lhsExp && n2 instanceof FieldAccessExpressionNode rhsExpr) {
            // only the simple field access expressions will be considered.
            return isSameSimpleExpression(lhsExp.fieldName(), rhsExpr.fieldName())
                    && isSameSimpleExpression(lhsExp.expression(), rhsExpr.expression());
        }

        if (n1 instanceof BuiltinSimpleNameReferenceNode lhsExp
                && n2 instanceof BuiltinSimpleNameReferenceNode rhsExpr) {
            return lhsExp.name().text().equals(rhsExpr.name().text());
        }
        return false;
    }

    public Optional<CoreRule> isSameOperandRestrictedInOperator(Token operator) {
        switch (operator.kind()) {
            case GT_EQUAL_TOKEN, LT_EQUAL_TOKEN, DOUBLE_EQUAL_TOKEN, TRIPPLE_EQUAL_TOKEN -> {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE);
            }
            case GT_TOKEN, LT_TOKEN, NOT_DOUBLE_EQUAL_TOKEN, NOT_EQUAL_TOKEN -> {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE);
            }
            case LOGICAL_OR_TOKEN, LOGICAL_AND_TOKEN, BITWISE_AND_TOKEN, PIPE_TOKEN -> {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE);
            }
        }
        return Optional.empty();
    }
}
