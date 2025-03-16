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
import io.ballerina.compiler.api.symbols.ClassFieldSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ExplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ImplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitAnonymousFunctionParameters;
import io.ballerina.compiler.syntax.tree.IncludedRecordParameterNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.Constants;

import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.scan.utils.Constants.INIT_FUNCTION;
import static io.ballerina.scan.utils.Constants.MAIN_FUNCTION;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.isDefinedQualifiedNameReference;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.isEqualToProvidedLiteralIdentifier;
import static io.ballerina.scan.utils.ScanCodeAnalyzerUtils.isSameSimpleExpression;

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
        this.visitSyntaxNode(checkExpressionNode);
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        semanticModel.symbol(objectFieldNode).ifPresent(symbol -> {
            if (symbol instanceof ClassFieldSymbol classFieldSymbol) {
                List<Qualifier> qualifiers = classFieldSymbol.qualifiers();
                if (hasQualifier(qualifiers, PRIVATE_KEYWORD)) {
                    if (semanticModel.references(symbol).size() == 1) {
                        reportIssue(objectFieldNode, CoreRule.UNUSED_PRIVATE_CLASS_FIELD);
                    }
                }
            }
        });
        this.visitSyntaxNode(objectFieldNode);
    }
    public void visit(BinaryExpressionNode binaryExpressionNode) {
        reportIssuesWithTrivialOperations(binaryExpressionNode);
        filterSameReferenceIssueBasedOnOperandType(binaryExpressionNode.operator()).ifPresent(rule -> {
            checkUsageOfSameOperandInBinaryExpr(binaryExpressionNode.lhsExpr(),
                    binaryExpressionNode.rhsExpr(), rule, binaryExpressionNode);
        });
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        checkUsageOfAssignment(assignmentStatementNode.varRef(), assignmentStatementNode.expression(),
                CoreRule.SELF_ASSIGNMENT, assignmentStatementNode);
        this.visitSyntaxNode(assignmentStatementNode);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        checkUsageOfAssignment(compoundAssignmentStatementNode.lhsExpression(),
                compoundAssignmentStatementNode.rhsExpression(), CoreRule.SELF_ASSIGNMENT,
                compoundAssignmentStatementNode);
        this.visitSyntaxNode(compoundAssignmentStatementNode);
    }

    private void checkUsageOfSameOperandInBinaryExpr(Node lhs, Node rhs, CoreRule rule, Node binaryExprNode) {
        if (isSameSimpleExpression(lhs, rhs)) {
            reportIssue(binaryExprNode, rule);
        }
    }

    private void checkUsageOfAssignment(Node lhs, Node rhs, CoreRule rule, Node assignmentNode) {
        if (isSameSimpleExpression(lhs, rhs)) {
            reportIssue(assignmentNode, rule);
        }
    }

    private void reportIssuesWithTrivialOperations(BinaryExpressionNode binaryExpressionNode) {
        SyntaxKind binaryOperatorKind = binaryExpressionNode.operator().kind();
        if (binaryOperatorKind == SyntaxKind.GT_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.FLOAT, Constants.Token.INFINITY)) {
                // a > Infinity is always false.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }

            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MAX_VALUE)) {
                // a > MAX_VALUE is always false.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.LT_EQUAL_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.FLOAT, Constants.Token.INFINITY)) {
                // a <= Infinity is always true.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }

            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MAX_VALUE)) {
                // a <= MAX_VALUE is always true.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.LT_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MIN_VALUE)) {
                // a < MIN_VALUE is always false.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.GT_EQUAL_TOKEN) {
            if (isDefinedQualifiedNameReference(binaryExpressionNode.rhsExpr(),
                    Constants.Token.INT, Constants.Token.MIN_VALUE)) {
                // a >= MIN_VALUE is always true.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.LOGICAL_AND_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.FALSE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.FALSE)) {
                // a && false is always false.
                reportIssue(binaryExpressionNode,  CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.TRUE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.TRUE)) {
                // a && true is always `a`.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.LOGICAL_OR_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.FALSE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.FALSE)) {
                // a || false is always `a`.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE);
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.TRUE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.TRUE)) {
                // a || true is always true.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.BITWISE_AND_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.ZERO)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.ZERO)) {
                // a & 0 is always false.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.MINUS_ONE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.MINUS_ONE)) {
                // a & -1 is always `a`.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE);
            }
        }

        if (binaryOperatorKind == SyntaxKind.PIPE_TOKEN) {
            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.ZERO)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.ZERO)) {
                // a | 0 is always `a`.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE);
            }

            if (isEqualToProvidedLiteralIdentifier(binaryExpressionNode.rhsExpr(), Constants.Token.MINUS_ONE)
                    || isEqualToProvidedLiteralIdentifier(binaryExpressionNode.lhsExpr(), Constants.Token.MINUS_ONE)) {
                // a | -1 is always true.
                reportIssue(binaryExpressionNode, CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }
        }
    }

    private Optional<CoreRule> filterSameReferenceIssueBasedOnOperandType(Token operator) {
        switch (operator.kind()) {
            case GT_EQUAL_TOKEN, LT_EQUAL_TOKEN, DOUBLE_EQUAL_TOKEN, TRIPPLE_EQUAL_TOKEN -> {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }
            case GT_TOKEN, LT_TOKEN, NOT_DOUBLE_EQUAL_TOKEN, NOT_EQUAL_TOKEN -> {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }
            case LOGICAL_OR_TOKEN, LOGICAL_AND_TOKEN, BITWISE_AND_TOKEN, PIPE_TOKEN -> {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE);
            }
        }
        return Optional.empty();
    }

    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        checkUnusedFunctionParameters(functionDefinitionNode.functionSignature());
        String functionName = functionDefinitionNode.functionName().text();
        if (!functionName.equals(MAIN_FUNCTION) && !functionName.equals(INIT_FUNCTION)) {
            checkNonIsolatedPublicFunction(functionDefinitionNode);
        }
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

    @Override
    public void visit(ClassDefinitionNode classDefinitionNode) {
        checkNonIsolatedPublicClassDefinition(classDefinitionNode);
        checkNonIsolatedPublicClassMethod(classDefinitionNode);
        this.visitSyntaxNode(classDefinitionNode);
    }

    @Override
    public void visit(TypeDefinitionNode typeDefinitionNode) {
        checkNonIsolatedConstructsInTypeDefinition(typeDefinitionNode);
        this.visitSyntaxNode(typeDefinitionNode);
    }

    private void checkNonIsolatedConstructsInTypeDefinition(TypeDefinitionNode typeDefinitionNode) {
        semanticModel.symbol(typeDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                TypeSymbol typeSymbol = typeDefinitionSymbol.typeDescriptor();
                if (typeSymbol instanceof ObjectTypeSymbol objectTypeSymbol) {
                    List<Qualifier> qualifiers = objectTypeSymbol.qualifiers();
                    List<Qualifier> typeDefQualifiers = typeDefinitionSymbol.qualifiers();
                    if (hasQualifier(typeDefQualifiers, PUBLIC_KEYWORD) &&
                            !hasQualifier(qualifiers, ISOLATED_KEYWORD)) {
                        reportIssue(typeDefinitionNode, CoreRule.PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT);
                    }
                }
            }
        });
    }

    private void checkNonIsolatedPublicClassDefinition(ClassDefinitionNode classDefinitionNode) {
        semanticModel.symbol(classDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof ObjectTypeSymbol objectTypeSymbol) {
                List<Qualifier> qualifiers = objectTypeSymbol.qualifiers();
                if (isPublicIsolatedConstruct(qualifiers)) {
                    reportIssue(classDefinitionNode, CoreRule.PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT);
                }
            }
        });
    }

    private void checkNonIsolatedPublicClassMethod(ClassDefinitionNode classDefinitionNode) {
        semanticModel.symbol(classDefinitionNode).ifPresent(classSymbol -> {
            if (classSymbol instanceof ObjectTypeSymbol objectTypeSymbol) {
                boolean isPublicObjectTypeSymbol = hasQualifier(objectTypeSymbol.qualifiers(), PUBLIC_KEYWORD);
                classDefinitionNode.members().forEach(member -> {
                    semanticModel.symbol(member).ifPresent(memberSymbol -> {
                        if (isPublicObjectTypeSymbol && memberSymbol.kind() == SymbolKind.METHOD) {
                            checkNonIsolatedPublicMethod((FunctionDefinitionNode) member);
                        }
                    });
                });
            }
        });
    }

    private void checkNonIsolatedPublicMethod(FunctionDefinitionNode member) {
        if (isPublicIsolatedConstruct(member.qualifierList())) {
            reportIssue(member, CoreRule.PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT);
        }
    }

    private void checkNonIsolatedPublicFunction(FunctionDefinitionNode functionDefinitionNode) {
        semanticModel.symbol(functionDefinitionNode).ifPresent(symbol -> {
            if (symbol.kind() != SymbolKind.METHOD) {
                NodeList<Token> qualifiers = functionDefinitionNode.qualifierList();
                if (isPublicIsolatedConstruct(qualifiers)) {
                    reportIssue(functionDefinitionNode, CoreRule.PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT);
                }
            }
        });
    }

    private boolean hasQualifier(List<Qualifier> qualifierList, SyntaxKind qualifierValue) {
        String qualifierValueStr = qualifierValue.stringValue();
        for (Qualifier qualifier : qualifierList) {
            if (qualifier.getValue().equals(qualifierValueStr)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasQualifier(NodeList<Token> qualifierList, SyntaxKind qualifier) {
        for (Token token : qualifierList) {
            if (qualifier == token.kind()) {
                return true;
            }
        }
        return false;
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

    private boolean hasQualifier(List<Qualifier> qualifierList, SyntaxKind qualifierValue) {
        String qualifierValueStr = qualifierValue.stringValue();
        for (Qualifier qualifier : qualifierList) {
            if (qualifier.getValue().equals(qualifierValueStr)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isPublicIsolatedConstruct(NodeList<Token> qualifiers) {
        return hasQualifier(qualifiers, PUBLIC_KEYWORD) && !hasQualifier(qualifiers, ISOLATED_KEYWORD);
    }

    private boolean isPublicIsolatedConstruct(List<Qualifier> qualifiers) {
        return hasQualifier(qualifiers, PUBLIC_KEYWORD) && !hasQualifier(qualifiers, ISOLATED_KEYWORD);
    }
}
