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
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ImplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitAnonymousFunctionParameters;
import io.ballerina.compiler.syntax.tree.IncludedRecordParameterNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.Constants;

import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECKPANIC_KEYWORD;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.isDefinedQualifiedNameReference;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.isEqualToProvidedLiteralIdentifier;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.isSameSimpleExpression;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.reportIssue;

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

    StaticCodeAnalyzer(Document document, ScannerContextImpl scannerContext, SemanticModel semanticModel) {
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
            reportIssue(checkExpressionNode, CoreRule.AVOID_CHECKPANIC);
        }
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        modulePartNode.members().forEach(member -> member.accept(this));
    }

    @Override
    public void visit(BinaryExpressionNode binaryExpressionNode) {
        reportIssuesForTrivialOperations(binaryExpressionNode);
        isSameOperandRestrictedInOperator(binaryExpressionNode.operator()).ifPresent(rule -> {
            checkSameUsageInBinaryOperator(binaryExpressionNode.lhsExpr(),
                    binaryExpressionNode.rhsExpr(), rule, binaryExpressionNode.location());
        });
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        checkSameUsageInAssignment(assignmentStatementNode.varRef(), assignmentStatementNode.expression(),
                CoreRule.SELF_ASSIGNMENT, assignmentStatementNode.location());
        this.visitSyntaxNode(assignmentStatementNode);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        checkSameUsageInAssignment(compoundAssignmentStatementNode.lhsExpression(),
                compoundAssignmentStatementNode.rhsExpression(), CoreRule.SELF_ASSIGNMENT,
                compoundAssignmentStatementNode.location());
        this.visitSyntaxNode(compoundAssignmentStatementNode);
    }

    private void checkSameUsageInBinaryOperator(Node lhs, Node rhs, CoreRule rule, NodeLocation location) {
        if (isSameSimpleExpression(lhs, rhs)) {
            reportIssue(scannerContext, document, location, rule.rule());
        }
    }

    private void checkSameUsageInAssignment(Node lhs, Node rhs, CoreRule rule, NodeLocation location) {
        if (isSameSimpleExpression(lhs, rhs)) {
            reportIssue(scannerContext, document, location, rule.rule());
        }
    }

    public void reportIssuesForTrivialOperations(BinaryExpressionNode binaryExpressionNode) {
        if (binaryExpressionNode.operator().kind() == SyntaxKind.GT_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.FLOAT, Constants.Token.INFINITY)) {
                // a > Infinity is always false.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule());
            }

            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MAX_VALUE)) {
                // a > MAX_VALUE is always false.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.LT_EQUAL_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.FLOAT, Constants.Token.INFINITY)) {
                // a <= Infinity is always true.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule());
            }

            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MAX_VALUE)) {
                // a <= MAX_VALUE is always true.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.LT_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MIN_VALUE)) {
                // a < MIN_VALUE is always false.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.GT_EQUAL_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MIN_VALUE)) {
                // a >= MIN_VALUE is always true.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.LOGICAL_AND_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.FALSE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.FALSE)) {
                // a && false is always false.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule());
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.TRUE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.TRUE)) {
                // a && true is always `a`.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.LOGICAL_OR_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.FALSE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.FALSE)) {
                // a || false is always `a`.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule());
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.TRUE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.TRUE)) {
                // a || true is always true.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.BITWISE_AND_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.ZERO)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.ZERO)) {
                // a & 0 is always false.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_FALSE.rule());
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.MINUS_ONE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.MINUS_ONE)) {
                // a & -1 is always `a`.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule());
            }
        }

        if (binaryExpressionNode.operator().kind() == SyntaxKind.PIPE_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.ZERO)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.ZERO)) {
                // a | 0 is always `a`.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_SELF_VALUE.rule());
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.MINUS_ONE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.MINUS_ONE)) {
                // a | -1 is always true.
                reportIssue(scannerContext, document, binaryExpressionNode,
                        CoreRule.OPERATION_ALWAYS_EVALUATE_TO_TRUE.rule());
            }
        }
    }

    private Optional<CoreRule> isSameOperandRestrictedInOperator(Token operator) {
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
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        checkUnusedFunctionParameters(functionDefinitionNode.functionSignature());
        this.visitSyntaxNode(functionDefinitionNode);
    }

    @Override
    public void visit(ExplicitAnonymousFunctionExpressionNode explicitAnonymousFunctionExpressionNode) {
        checkUnusedFunctionParameters(explicitAnonymousFunctionExpressionNode.functionSignature());
        this.visitSyntaxNode(explicitAnonymousFunctionExpressionNode);
    }

    @Override
    public void visit(ImplicitAnonymousFunctionExpressionNode implicitAnonymousFunctionExpressionNode) {
        Node params = implicitAnonymousFunctionExpressionNode.params();
        if (params instanceof ImplicitAnonymousFunctionParameters parameters) {
            parameters.parameters().forEach(parameter -> {
                reportIssueIfNodeIsUnused(parameter, CoreRule.UNUSED_FUNCTION_PARAMETER);
            });
            return;
        }
        if (params instanceof SimpleNameReferenceNode) {
            reportIssueIfNodeIsUnused(params, CoreRule.UNUSED_FUNCTION_PARAMETER);
        }

        this.visitSyntaxNode(implicitAnonymousFunctionExpressionNode.expression());
    }

    private void checkUnusedFunctionParameters(FunctionSignatureNode functionSignatureNode) {
        functionSignatureNode.parameters().forEach(parameter -> {
            if (parameter instanceof IncludedRecordParameterNode includedRecordParameterNode) {
                includedRecordParameterNode.paramName().ifPresent(name -> {
                    reportIssueIfNodeIsUnused(name, CoreRule.UNUSED_FUNCTION_PARAMETER);
                });
            } else {
                reportIssueIfNodeIsUnused(parameter, CoreRule.UNUSED_FUNCTION_PARAMETER);
            }
            this.visitSyntaxNode(parameter);
        });
    }

    private void reportIssueIfNodeIsUnused(Node node, CoreRule coreRule) {
        if (isUnusedNode(node)) {
            reportIssue(node, coreRule);
        }
    }

    private void reportIssue(Node node, CoreRule coreRule) {
        scannerContext.getReporter().reportIssue(document, node.location(), coreRule.rule());
    }

    private boolean isUnusedNode(Node node) {
        Optional<Symbol> symbol = semanticModel.symbol(node);
        return symbol.filter(value -> semanticModel.references(value).size() == 1).isPresent();
    }
}
