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
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

import java.util.List;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECKPANIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.scan.utils.Constants.INIT_METHOD;
import static io.ballerina.scan.utils.Constants.MAIN_METHOD;

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
        if (checkExpressionNode.checkKeyword().kind().equals(CHECKPANIC_KEYWORD)) {
            reportIssue(scannerContext, document, checkExpressionNode, CoreRule.AVOID_CHECKPANIC.rule());
        }
    }

    @Override
    public void visit(ClassDefinitionNode classDefinitionNode) {
        checkNonIsolatedPublicClassDefinitions(classDefinitionNode);
        checkNonIsolatedPublicClassMembers(classDefinitionNode);
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        if (!functionDefinitionNode.functionName().text().equals(MAIN_METHOD) &&
                    !functionDefinitionNode.functionName().text().equals(INIT_METHOD)) {
            checkNonIsolatedPublicFunctions(functionDefinitionNode);
        }
        functionDefinitionNode.functionBody().children().forEach(child -> child.accept(this));
    }

    private void checkNonIsolatedPublicClassDefinitions(ClassDefinitionNode classDefinitionNode) {
        semanticModel.symbol(classDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof ClassSymbol classSymbol) {
                List<Qualifier> qualifiers = classSymbol.qualifiers();
                if (getQualifier(qualifiers, PUBLIC_KEYWORD.stringValue()) &&
                        !getQualifier(qualifiers, ISOLATED_KEYWORD.stringValue())) {
                    reportIssue(scannerContext, document, classDefinitionNode,
                            CoreRule.PUBLIC_NON_ISOLATED_CONSTRUCT.rule());
                }
            }
        });
    }

    private void checkNonIsolatedPublicClassMembers(ClassDefinitionNode classDefinitionNode) {
        classDefinitionNode.members().forEach(member -> {
            semanticModel.symbol(member).ifPresent(symbol -> {
                if (symbol.kind() == SymbolKind.METHOD) {
                    checkNonIsolatedPublicMethods((FunctionDefinitionNode) member, classDefinitionNode);
                }
                member.accept(this);
            });
        });
    }

    private void checkNonIsolatedPublicMethods(FunctionDefinitionNode member,
                                               ClassDefinitionNode classDefinitionNode) {
        semanticModel.symbol(classDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof ClassSymbol classSymbol) {
                if (getQualifier(classSymbol.qualifiers(), PUBLIC_KEYWORD.stringValue()) &&
                        getQualifier(member.qualifierList(), PUBLIC_KEYWORD) &&
                        !getQualifier(member.qualifierList(), ISOLATED_KEYWORD)) {
                    reportIssue(scannerContext, document, member, CoreRule.PUBLIC_NON_ISOLATED_CONSTRUCT.rule());
                }
            }
        });
    }

    private void checkNonIsolatedPublicFunctions(FunctionDefinitionNode functionDefinitionNode) {
        semanticModel.symbol(functionDefinitionNode).ifPresent(symbol -> {
            if (symbol.kind() != SymbolKind.METHOD) {
                NodeList<Token> qualifiers = functionDefinitionNode.qualifierList();
                if (getQualifier(qualifiers, PUBLIC_KEYWORD) && !getQualifier(qualifiers, ISOLATED_KEYWORD)) {
                    reportIssue(scannerContext, document, functionDefinitionNode,
                            CoreRule.PUBLIC_NON_ISOLATED_CONSTRUCT.rule());
                }
            }
        });
    }

    private boolean getQualifier(List<Qualifier> qualifierList, String qualifierValue) {
        for (Qualifier qualifier : qualifierList) {
            if (qualifier.getValue().equals(qualifierValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean getQualifier(NodeList<Token> qualifierList, SyntaxKind qualifier) {
        for (Token token : qualifierList) {
            if (qualifier == token.kind()) {
                return true;
            }
        }
        return false;
    }

    private void reportIssue(ScannerContext scannerContext, Document document, Node node, Rule rule) {
        scannerContext.getReporter().reportIssue(document, node.location(), rule);
    }
}
