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

import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.api.SemanticModel;
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
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.ScannerContext;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOUBLE_DOT_LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELLIPSIS_TOKEN;
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
        this.semanticModel = semanticModel;
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

    public void visit(BinaryExpressionNode binaryExpressionNode) {
        if (binaryExpressionNode.operator().kind().equals(ELLIPSIS_TOKEN)
                || binaryExpressionNode.operator().kind().equals(DOUBLE_DOT_LT_TOKEN)) {
            validateRangeExpressionOperator(scannerContext, document, binaryExpressionNode.lhsExpr(),
                    binaryExpressionNode.rhsExpr(), binaryExpressionNode.operator(), binaryExpressionNode.location());
        }
    }

    private void validateRangeExpressionOperator(ScannerContext scannerContext, Document document,
                                                 Node n1, Node n2, Token operator, NodeLocation location) {
        String lhsExpr = null;
        String rhsExpr = null;
        boolean isMinusOperatorPresentInLhs = false;
        boolean isMinusOperatorPresentInRhs = false;

        if (n1 instanceof UnaryExpressionNode unaryN1) {
            if (unaryN1.unaryOperator().kind() == SyntaxKind.MINUS_TOKEN) {
                isMinusOperatorPresentInLhs = true;
            }
            n1 = unaryN1.expression();
        }
        if (n2 instanceof UnaryExpressionNode unaryN2) {
            if (unaryN2.unaryOperator().kind() == SyntaxKind.MINUS_TOKEN) {
                isMinusOperatorPresentInRhs = true;
            }
            n2 = unaryN2.expression();
        }

        if (n1 instanceof BasicLiteralNode lhsExprNode) {
            lhsExpr = lhsExprNode.literalToken().text();
            if (isMinusOperatorPresentInLhs) {
                lhsExpr = "-" + lhsExpr;
            }
        }

        if (n2 instanceof BasicLiteralNode rhsExprNode) {
            rhsExpr = rhsExprNode.literalToken().text();
            if (isMinusOperatorPresentInRhs) {
                rhsExpr = "-" + rhsExpr;
            }
        }

        if (lhsExpr != null && rhsExpr != null) {
            // According to the spec, these literal tokens are integers. So no need to cast or check.
            try {
                int lhs = Integer.parseInt(lhsExpr);
                int rhs = Integer.parseInt(rhsExpr);
                if (operator.kind() == DOUBLE_DOT_LT_TOKEN) {
                    if (lhs >= rhs) {
                        scannerContext.getReporter().reportIssue(document, location,
                                CoreRule.INVALID_RANGE_OPERATOR.rule());
                    }
                } else if (operator.kind() == ELLIPSIS_TOKEN) {
                    if (lhs > rhs) {
                        scannerContext.getReporter().reportIssue(document, location,
                                CoreRule.INVALID_RANGE_OPERATOR.rule());
                    }
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }
    
    @Override
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
