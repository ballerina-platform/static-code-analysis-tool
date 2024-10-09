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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
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
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
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
            reportIssue(scannerContext, document, checkExpressionNode, CoreRule.AVOID_CHECKPANIC.rule());
        }
    }

    @Override
    public void visit(FunctionSignatureNode functionSignatureNode) {
        functionSignatureNode.parameters().forEach(parameter -> {
            if (parameter instanceof IncludedRecordParameterNode includedRecordParameterNode) {
                includedRecordParameterNode.paramName().ifPresent(name -> {
                    if (isUnusedNode(name, semanticModel)) {
                        reportIssue(scannerContext, document, name, CoreRule.UNUSED_FUNCTION_PARAMETERS.rule());
                    }
                });
                return;
            }

            if (isUnusedNode(parameter, semanticModel)) {
                reportIssue(scannerContext, document, parameter, CoreRule.UNUSED_FUNCTION_PARAMETERS.rule());
            }
            this.visitSyntaxNode(parameter);
        });
        functionSignatureNode.returnTypeDesc().ifPresent(returnTypeDesc -> returnTypeDesc.accept(this));
    }

    @Override
    public void visit(ImplicitAnonymousFunctionExpressionNode implicitAnonymousFunctionExpressionNode) {
        checkUnusedParametersInImplicitFunctionExpression(implicitAnonymousFunctionExpressionNode.params());
        this.visitSyntaxNode(implicitAnonymousFunctionExpressionNode.expression());
    }

    private void checkUnusedParametersInImplicitFunctionExpression(Node params) {
        if (params instanceof ImplicitAnonymousFunctionParameters parameters) {
            parameters.parameters().forEach(parameter -> {
                if (isUnusedNode(parameter, semanticModel)) {
                    reportIssue(scannerContext, document, parameter, CoreRule.UNUSED_FUNCTION_PARAMETERS.rule());
                }
            });
        }

        if (params instanceof SimpleNameReferenceNode) {
            if (isUnusedNode(params, semanticModel)) {
                reportIssue(scannerContext, document, params, CoreRule.UNUSED_FUNCTION_PARAMETERS.rule());
            }
        }
    }

    private void reportIssue(ScannerContext scannerContext, Document document, Node node, Rule rule) {
        scannerContext.getReporter().reportIssue(document, node.location(), rule);
    }

    private boolean isUnusedNode(Node node, SemanticModel semanticModel) {
        Optional<Symbol> symbol = semanticModel.symbol(node);
        if (symbol.isEmpty()) {
            return false;
        }

        if (semanticModel.references(symbol.get()).size() == 1) {
            return true;
        }
        return false;
    }
}
