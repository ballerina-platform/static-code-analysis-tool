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
import io.ballerina.compiler.api.TypeBuilder;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassFieldSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectFieldSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExternalFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FieldBindingPatternFullNode;
import io.ballerina.compiler.syntax.tree.FieldBindingPatternVarnameNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ImplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.IncludedRecordParameterNode;
import io.ballerina.compiler.syntax.tree.LetVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingBindingPatternNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestBindingPatternNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SpreadMemberNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WildcardBindingPatternNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utils.Constants;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOUBLE_DOT_LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELLIPSIS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.READONLY_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SERVICE_KEYWORD;
import static io.ballerina.scan.utils.Constants.INIT_FUNCTION;
import static io.ballerina.scan.utils.Constants.MAIN_FUNCTION;
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
    private final SecretValueResolver secretValueResolver;
    // Test sources are compiled only into the test module, so their public constructs are not a public API
    private final boolean isTestSource;
    private final Map<Node, Set<MethodSymbol>> implementedMethodsByParent = new HashMap<>();

    StaticCodeAnalyzer(Document document, ScannerContextImpl scannerContext, SemanticModel semanticModel) {
        this(document, scannerContext, semanticModel, new SecretValueResolver(semanticModel, document.module()));
    }

    StaticCodeAnalyzer(Document document, ScannerContextImpl scannerContext, SemanticModel semanticModel,
                       SecretValueResolver secretValueResolver) {
        this.document = document;
        this.syntaxTree = document.syntaxTree();
        this.scannerContext = scannerContext;
        this.semanticModel = semanticModel;
        this.secretValueResolver = secretValueResolver;
        this.isTestSource = document.module().testDocumentIds().contains(document.documentId());
    }

    void analyze() {
        this.visit((ModulePartNode) syntaxTree.rootNode());
        SecretChecker secretChecker = new SecretChecker(document, scannerContext, semanticModel, secretValueResolver);
        secretChecker.analyze();
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

    public void visit(BinaryExpressionNode binaryExpressionNode) {
        reportIssuesWithTrivialOperations(binaryExpressionNode);
        filterSameReferenceIssueBasedOnOperandType(binaryExpressionNode.operator()).ifPresent(rule -> {
            checkUsageOfSameOperandInBinaryExpr(binaryExpressionNode.lhsExpr(),
                    binaryExpressionNode.rhsExpr(), rule, binaryExpressionNode);
        });

        SyntaxKind binaryOperatorKind = binaryExpressionNode.operator().kind();
        if (binaryOperatorKind.equals(ELLIPSIS_TOKEN)
                || binaryOperatorKind.equals(DOUBLE_DOT_LT_TOKEN)) {
            validateRangeExpressionOperator(scannerContext, document, binaryExpressionNode.lhsExpr(),
                    binaryExpressionNode.rhsExpr(), binaryExpressionNode.operator(), binaryExpressionNode.location());
        }
        this.visitSyntaxNode(binaryExpressionNode);
    }

    private void validateRangeExpressionOperator(ScannerContext scannerContext, Document document,
                                                 Node lhsNode, Node rhsNode, Token operator, NodeLocation location) {
        Optional<Long> lhsValue = getIntValueFromRangeExprNode(lhsNode);
        Optional<Long> rhsValue = getIntValueFromRangeExprNode(rhsNode);

        if (lhsValue.isEmpty() || rhsValue.isEmpty()) {
            return;
        }

        boolean isEmpty = operator.kind() == DOUBLE_DOT_LT_TOKEN
                ? lhsValue.get() >= rhsValue.get()
                : lhsValue.get() > rhsValue.get();
        if (isEmpty) {
            scannerContext.getReporter().reportIssue(document, location, CoreRule.INVALID_RANGE_EXPRESSION.rule());
        }
    }
    
    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        semanticModel.symbol(objectFieldNode).ifPresent(symbol -> {
            if (symbol instanceof ClassFieldSymbol classFieldSymbol) {
                List<Qualifier> qualifiers = classFieldSymbol.qualifiers();
                if (hasQualifier(qualifiers, PRIVATE_KEYWORD)) {
                    if (semanticModel.references(symbol).size() == 1) {
                        reportIssue(withoutMetadata(objectFieldNode), CoreRule.UNUSED_PRIVATE_CLASS_FIELD);
                    }
                }
            }
        });
        this.visitSyntaxNode(objectFieldNode);
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
        reportComparisonWithTypeBound(binaryExpressionNode);
        SyntaxKind binaryOperatorKind = binaryExpressionNode.operator().kind();
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

    /**
     * Reports a comparison against the largest or smallest value of its type, such as {@code x > int:MAX_VALUE}
     * or {@code int:MIN_VALUE > x}. A bound on the left is compared as if the operands were swapped.
     */
    private void reportComparisonWithTypeBound(BinaryExpressionNode binaryExpressionNode) {
        SyntaxKind operator = binaryExpressionNode.operator().kind();
        if (operator != SyntaxKind.LT_TOKEN && operator != SyntaxKind.LT_EQUAL_TOKEN
                && operator != SyntaxKind.GT_TOKEN && operator != SyntaxKind.GT_EQUAL_TOKEN) {
            return;
        }
        Node lhs = binaryExpressionNode.lhsExpr();
        Node rhs = binaryExpressionNode.rhsExpr();
        typeBound(rhs).flatMap(bound -> outcome(bound, operator, lhs))
                .or(() -> typeBound(lhs).flatMap(bound -> outcome(bound, swapOperands(operator), rhs)))
                .ifPresent(rule -> reportIssue(binaryExpressionNode, rule));
    }

    // A comparison with nil, or with NaN for a float bound, is false, so it is always true only when neither can be.
    private Optional<CoreRule> outcome(TypeBound bound, SyntaxKind operator, Node otherOperand) {
        return bound.outcome(operator)
                .filter(rule -> rule != CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE || !canBeNil(otherOperand));
    }

    private boolean canBeNil(Node expression) {
        return semanticModel.typeOf(expression)
                .map(type -> semanticModel.types().NIL.subtypeOf(type))
                .orElse(true);
    }

    private static SyntaxKind swapOperands(SyntaxKind operator) {
        return switch (operator) {
            case GT_TOKEN -> SyntaxKind.LT_TOKEN;
            case LT_TOKEN -> SyntaxKind.GT_TOKEN;
            case GT_EQUAL_TOKEN -> SyntaxKind.LT_EQUAL_TOKEN;
            case LT_EQUAL_TOKEN -> SyntaxKind.GT_EQUAL_TOKEN;
            default -> operator;
        };
    }

    private Optional<TypeBound> typeBound(Node node) {
        Node operand = withoutPlusOrBraces(node);
        if (operand instanceof UnaryExpressionNode unary && unary.unaryOperator().kind() == SyntaxKind.MINUS_TOKEN) {
            return langLibConstant(withoutPlusOrBraces(unary.expression()))
                    .filter(constant -> constant.equals(Constants.Token.FLOAT + ":" + Constants.Token.INFINITY))
                    .map(constant -> TypeBound.FLOAT_LOWER);
        }
        return langLibConstant(operand).flatMap(constant -> switch (constant) {
            case Constants.Token.INT + ":" + Constants.Token.MAX_VALUE -> Optional.of(TypeBound.INT_UPPER);
            case Constants.Token.INT + ":" + Constants.Token.MIN_VALUE -> Optional.of(TypeBound.INT_LOWER);
            case Constants.Token.FLOAT + ":" + Constants.Token.INFINITY -> Optional.of(TypeBound.FLOAT_UPPER);
            default -> Optional.empty();
        });
    }

    private static Node withoutPlusOrBraces(Node node) {
        return switch (node) {
            case BracedExpressionNode braced -> withoutPlusOrBraces(braced.expression());
            case UnaryExpressionNode unary when unary.unaryOperator().kind() == SyntaxKind.PLUS_TOKEN ->
                    withoutPlusOrBraces(unary.expression());
            default -> node;
        };
    }

    /**
     * Returns the name of the lang library constant the node refers to, such as {@code int:MAX_VALUE}, whatever
     * prefix the module is imported with.
     */
    private Optional<String> langLibConstant(Node node) {
        if (!(node instanceof QualifiedNameReferenceNode)) {
            return Optional.empty();
        }
        return semanticModel.symbol(node)
                .filter(symbol -> symbol.kind() == SymbolKind.CONSTANT && SemanticUtils.isLangLib(symbol))
                .flatMap(symbol -> symbol.getModule().flatMap(module -> symbol.getName()
                        .map(name -> module.id().moduleName().substring("lang.".length()) + ":" + name)));
    }

    /**
     * The largest or smallest value of a type. Nothing is greater than an upper bound, but a value can be not
     * less than or equal to {@code float:Infinity} when it is {@code NaN}.
     */
    private enum TypeBound {
        INT_UPPER(true, false),
        INT_LOWER(false, false),
        FLOAT_UPPER(true, true),
        FLOAT_LOWER(false, true);

        private final boolean upper;
        private final boolean hasNaN;

        TypeBound(boolean upper, boolean hasNaN) {
            this.upper = upper;
            this.hasNaN = hasNaN;
        }

        Optional<CoreRule> outcome(SyntaxKind operator) {
            if (operator == (upper ? SyntaxKind.GT_TOKEN : SyntaxKind.LT_TOKEN)) {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATES_TO_FALSE);
            }
            if (!hasNaN && operator == (upper ? SyntaxKind.LT_EQUAL_TOKEN : SyntaxKind.GT_EQUAL_TOKEN)) {
                return Optional.of(CoreRule.OPERATION_ALWAYS_EVALUATES_TO_TRUE);
            }
            return Optional.empty();
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
        if (!(functionDefinitionNode.functionBody() instanceof ExternalFunctionBodyNode)
                && !isDeclaredByIncludedType(functionDefinitionNode)) {
            checkUnusedFunctionParameters(functionDefinitionNode.functionSignature());
        }
        String functionName = functionDefinitionNode.functionName().text();
        if (!isTestSource && !functionName.equals(MAIN_FUNCTION) && !functionName.equals(INIT_FUNCTION)) {
            checkNonIsolatedPublicFunction(functionDefinitionNode);
        }
        this.visitSyntaxNode(functionDefinitionNode);
    }

    @Override
    public void visit(ExplicitAnonymousFunctionExpressionNode explicitAnonymousFunctionExpressionNode) {
        if (!isParameterListExpected(explicitAnonymousFunctionExpressionNode)) {
            checkUnusedFunctionParameters(explicitAnonymousFunctionExpressionNode.functionSignature());
        }
        this.visitSyntaxNode(explicitAnonymousFunctionExpressionNode);
    }

    /**
     * The parameters of an implicit anonymous function, such as {@code (acc, x) => acc + 1}, are never reported:
     * the function type expected where it is used fixes how many there are.
     */
    @Override
    public void visit(ImplicitAnonymousFunctionExpressionNode implicitAnonymousFunctionExpressionNode) {
        implicitAnonymousFunctionExpressionNode.expression().accept(this);
    }

    /**
     * Checks whether the anonymous function is used where a function type with a parameter list is expected, such
     * as an argument to {@code reduce}. The parameters cannot be removed there, even when they are unused.
     */
    private boolean isParameterListExpected(ExplicitAnonymousFunctionExpressionNode anonymousFunction) {
        return contextualTypes(anonymousFunction).stream()
                .flatMap(StaticCodeAnalyzer::unionMembers)
                .anyMatch(type -> type instanceof FunctionTypeSymbol functionType
                        && functionType.params().isPresent());
    }

    /**
     * Returns the types the context expects the expression to have, such as the type of the parameter it is passed
     * to; the value of a {@code var} variable has none.
     */
    private List<TypeSymbol> contextualTypes(ExpressionNode expression) {
        Node parent = expression.parent();
        return switch (parent) {
            case PositionalArgumentNode argument -> parameterType(argument).stream().toList();
            case NamedArgumentNode argument -> parameterType(argument).stream().toList();
            case BracedExpressionNode braced -> contextualTypes(braced);
            case ListConstructorExpressionNode list -> boundType(list, expression).map(List::of)
                    .orElseGet(() -> contextualTypes(list).stream()
                            .flatMap(StaticCodeAnalyzer::unionMembers)
                            .flatMap(listType -> memberType(listType, list, expression).stream())
                            .toList());
            case SpecificFieldNode field when field.parent() instanceof MappingConstructorExpressionNode mapping ->
                    boundType(mapping, expression).map(List::of)
                            .orElseGet(() -> contextualTypes(mapping).stream()
                                    .flatMap(StaticCodeAnalyzer::unionMembers)
                                    .flatMap(mappingType -> fieldType(mappingType, field).stream())
                                    .toList());
            case VariableDeclarationNode variable -> declaredType(variable.typedBindingPattern());
            case ModuleVariableDeclarationNode variable -> declaredType(variable.typedBindingPattern());
            case LetVariableDeclarationNode variable -> declaredType(variable.typedBindingPattern());
            case ObjectFieldNode field -> declarationType(field);
            case RecordFieldWithDefaultValueNode field -> declarationType(field);
            case DefaultableParameterNode parameter -> declarationType(parameter);
            // A cast with only annotations, as in <@a> value, leaves the expected type to its context
            case TypeCastExpressionNode cast -> cast.typeCastParam().type().isPresent()
                    ? semanticModel.typeOf(cast).stream().toList()
                    : contextualTypes(cast);
            case ForEachStatementNode forEach -> forEachType(forEach)
                    .map(memberType -> List.of(iterableType(memberType, expression)))
                    .orElseGet(() -> expectedTypes(expression));
            default -> expectedTypes(expression);
        };
    }

    private List<TypeSymbol> expectedTypes(ExpressionNode expression) {
        return semanticModel.expectedType(document, expression.lineRange().startLine()).stream().toList();
    }

    // A destructuring or wildcard pattern binds no variable of the declared type, which is then resolved by name
    private List<TypeSymbol> declaredType(TypedBindingPatternNode typedBindingPattern) {
        if (typedBindingPattern.typeDescriptor().kind() == SyntaxKind.VAR_TYPE_DESC) {
            return List.of();
        }
        Optional<TypeSymbol> variableType = destructuringPattern(typedBindingPattern).isPresent()
                ? Optional.empty()
                : variableType(typedBindingPattern.bindingPattern());
        return variableType.or(() -> semanticModel.symbol(typedBindingPattern.typeDescriptor())
                        .filter(TypeSymbol.class::isInstance)
                        .map(TypeSymbol.class::cast))
                .stream().toList();
    }

    private List<TypeSymbol> declarationType(Node declaration) {
        return semanticModel.symbol(declaration).flatMap(symbol -> switch (symbol) {
            case ObjectFieldSymbol field -> Optional.of(field.typeDescriptor());
            case RecordFieldSymbol field -> Optional.of(field.typeDescriptor());
            case ParameterSymbol parameter -> Optional.of(parameter.typeDescriptor());
            default -> Optional.<TypeSymbol>empty();
        }).stream().toList();
    }

    private Optional<TypeSymbol> boundType(ExpressionNode constructor, ExpressionNode member) {
        return isDestructured(constructor) ? boundType(member) : Optional.empty();
    }

    /**
     * Returns the type of the variable a destructured member is bound to, as f in {@code [F, int] [f, n] = [fn, 1]},
     * since the semantic model gives no type for a binding pattern. A member bound to {@code _} has none.
     */
    private Optional<TypeSymbol> boundType(ExpressionNode member) {
        Optional<Node> pattern = bindingPattern(member);
        if (pattern.isPresent()) {
            return pattern.filter(node -> !(node instanceof WildcardBindingPatternNode)).flatMap(this::variableType);
        }
        // A member with no pattern of its own is bound to the rest variable, as in [n, ...rest] or {f, ...rest}
        if (member.parent() instanceof SpecificFieldNode field
                && field.parent() instanceof MappingConstructorExpressionNode mapping) {
            return restVariableType(bindingPattern(mapping)).flatMap(restType -> fieldType(restType, field));
        }
        if (!(member.parent() instanceof ListConstructorExpressionNode list)
                || !(bindingPattern(list).orElse(null) instanceof ListBindingPatternNode listPattern)) {
            return Optional.empty();
        }
        int boundBefore = listPattern.bindingPatterns().size() - 1;
        OptionalInt restPosition = position(list, member).stream().map(index -> index - boundBefore).findFirst();
        return restVariableType(Optional.of(listPattern)).flatMap(restType -> switch (restType) {
            case ArrayTypeSymbol arrayType -> Optional.of(arrayType.memberTypeDescriptor());
            case TupleTypeSymbol tupleType -> tupleMemberType(tupleType, restPosition);
            default -> Optional.empty();
        });
    }

    private Optional<TypeSymbol> restVariableType(Optional<Node> pattern) {
        List<? extends Node> patterns = switch (pattern.orElse(null)) {
            case ListBindingPatternNode listPattern -> listPattern.bindingPatterns().stream().toList();
            case MappingBindingPatternNode mappingPattern -> mappingPattern.fieldBindingPatterns().stream().toList();
            case null, default -> List.of();
        };
        return patterns.isEmpty() || !(patterns.get(patterns.size() - 1) instanceof RestBindingPatternNode rest)
                ? Optional.empty()
                : variableType(rest.variableName()).map(SemanticUtils::unwrap);
    }

    private boolean isDestructured(ExpressionNode constructor) {
        return bindingPattern(constructor)
                .filter(pattern -> pattern instanceof ListBindingPatternNode
                        || pattern instanceof MappingBindingPatternNode)
                .isPresent();
    }

    // The variables a foreach binds are narrowed to the members' own types, so the declared type is resolved instead
    private Optional<TypeSymbol> forEachType(ForEachStatementNode forEach) {
        Node typeDescriptor = forEach.typedBindingPattern().typeDescriptor();
        return typeDescriptor.kind() == SyntaxKind.VAR_TYPE_DESC
                ? Optional.empty()
                : semanticModel.symbol(typeDescriptor).filter(TypeSymbol.class::isInstance).map(TypeSymbol.class::cast);
    }

    private TypeSymbol iterableType(TypeSymbol memberType, ExpressionNode iterable) {
        ExpressionNode source = iterable;
        while (source instanceof BracedExpressionNode braced) {
            source = braced.expression();
        }
        TypeBuilder builder = semanticModel.types().builder();
        return source instanceof MappingConstructorExpressionNode
                ? builder.MAP_TYPE.withTypeParam(memberType).build()
                : builder.ARRAY_TYPE.withType(memberType).build();
    }

    /**
     * Returns the binding pattern that destructures the expression, or the part of it that binds the expression when
     * it is a member of a destructured list or mapping. A member after a spread of unknown length has none.
     */
    private Optional<Node> bindingPattern(ExpressionNode expression) {
        return switch (expression.parent()) {
            case BracedExpressionNode braced -> bindingPattern(braced);
            case VariableDeclarationNode variable -> destructuringPattern(variable.typedBindingPattern());
            case ModuleVariableDeclarationNode variable -> destructuringPattern(variable.typedBindingPattern());
            case AssignmentStatementNode assignment when assignment.varRef() instanceof BindingPatternNode pattern ->
                    Optional.of(pattern);
            case ListConstructorExpressionNode list -> {
                OptionalInt position = position(list, expression);
                if (position.isEmpty()) {
                    yield Optional.empty();
                }
                yield bindingPattern(list)
                        .filter(ListBindingPatternNode.class::isInstance)
                        .map(pattern -> ((ListBindingPatternNode) pattern).bindingPatterns())
                        .filter(patterns -> position.getAsInt() < patterns.size()
                                && !(patterns.get(position.getAsInt()) instanceof RestBindingPatternNode))
                        .map(patterns -> patterns.get(position.getAsInt()));
            }
            case SpecificFieldNode field when field.parent() instanceof MappingConstructorExpressionNode mapping -> {
                String name = SemanticUtils.fieldName(field);
                yield bindingPattern(mapping)
                        .filter(MappingBindingPatternNode.class::isInstance)
                        .flatMap(pattern -> ((MappingBindingPatternNode) pattern).fieldBindingPatterns().stream()
                                .flatMap(fieldPattern -> switch (fieldPattern) {
                                    case FieldBindingPatternFullNode full
                                            when patternFieldName(full.variableName()).equals(name) ->
                                            Stream.of((Node) full.bindingPattern());
                                    case FieldBindingPatternVarnameNode varName
                                            when patternFieldName(varName.variableName()).equals(name) ->
                                            Stream.of((Node) varName.variableName());
                                    default -> Stream.empty();
                                })
                                .findFirst());
            }
            case null, default -> Optional.empty();
        };
    }

    private static String patternFieldName(SimpleNameReferenceNode fieldName) {
        return SemanticUtils.identifierName(fieldName.name().text());
    }

    private static Optional<Node> destructuringPattern(TypedBindingPatternNode typedBindingPattern) {
        Node pattern = typedBindingPattern.bindingPattern();
        return typedBindingPattern.typeDescriptor().kind() != SyntaxKind.VAR_TYPE_DESC
                && (pattern instanceof ListBindingPatternNode || pattern instanceof MappingBindingPatternNode)
                ? Optional.of(pattern)
                : Optional.empty();
    }

    private Optional<TypeSymbol> variableType(Node pattern) {
        return semanticModel.symbol(pattern)
                .filter(VariableSymbol.class::isInstance)
                .map(symbol -> ((VariableSymbol) symbol).typeDescriptor());
    }

    private Optional<TypeSymbol> parameterType(FunctionArgumentNode argument) {
        Node call = argument.parent() instanceof ParenthesizedArgList argList ? argList.parent() : argument.parent();
        Optional<FunctionTypeSymbol> functionType = calleeType(call);
        if (functionType.isEmpty()) {
            return Optional.empty();
        }
        List<ParameterSymbol> parameters = functionType.get().params().orElse(List.of());
        if (argument instanceof NamedArgumentNode namedArgument) {
            String name = SemanticUtils.identifierName(namedArgument.argumentName().name().text());
            return parameters.stream()
                    .filter(parameter -> parameter.getName().map(SemanticUtils::identifierName)
                            .filter(name::equals).isPresent())
                    .findFirst()
                    .map(ParameterSymbol::typeDescriptor);
        }
        // A lang library function called as a method, as in value.f(a), receives the value as its first parameter
        int position = isLangLibMethodCall(call) ? 1 : 0;
        for (Node sibling : argument.parent().children()) {
            if (sibling == argument) {
                break;
            }
            if (sibling instanceof PositionalArgumentNode) {
                position++;
            }
        }
        if (position < parameters.size()) {
            return Optional.of(parameters.get(position).typeDescriptor());
        }
        return functionType.get().restParam()
                .map(ParameterSymbol::typeDescriptor)
                .map(SemanticUtils::unwrap)
                .filter(ArrayTypeSymbol.class::isInstance)
                .map(type -> ((ArrayTypeSymbol) type).memberTypeDescriptor());
    }

    private Optional<FunctionTypeSymbol> calleeType(Node call) {
        if (call instanceof ImplicitNewExpressionNode || call instanceof ExplicitNewExpressionNode) {
            // The type of new is a union with error when the init method can return one
            return semanticModel.typeOf(call).stream()
                    .flatMap(StaticCodeAnalyzer::unionMembers)
                    .filter(ClassSymbol.class::isInstance)
                    .findFirst()
                    .flatMap(type -> ((ClassSymbol) type).initMethod())
                    .map(MethodSymbol::typeDescriptor);
        }
        Optional<TypeSymbol> type = semanticModel.symbol(call).flatMap(symbol -> switch (symbol) {
            case FunctionSymbol function -> Optional.of(function.typeDescriptor());
            case VariableSymbol variable -> Optional.of(variable.typeDescriptor());
            case ParameterSymbol parameter -> Optional.of(parameter.typeDescriptor());
            case ObjectFieldSymbol field -> Optional.of(field.typeDescriptor());
            default -> Optional.empty();
        });
        return type.map(SemanticUtils::unwrap)
                .filter(FunctionTypeSymbol.class::isInstance)
                .map(FunctionTypeSymbol.class::cast);
    }

    private boolean isLangLibMethodCall(Node call) {
        return call.kind() == SyntaxKind.METHOD_CALL
                && semanticModel.symbol(call).filter(SemanticUtils::isLangLib).isPresent();
    }

    private Optional<TypeSymbol> memberType(TypeSymbol listType, ListConstructorExpressionNode list,
                                            ExpressionNode member) {
        return switch (listType) {
            case ArrayTypeSymbol arrayType -> Optional.of(arrayType.memberTypeDescriptor());
            case TupleTypeSymbol tupleType -> tupleMemberType(tupleType, position(list, member));
            default -> Optional.empty();
        };
    }

    // A spread of unknown length is only allowed in the rest of a tuple, so a member after one is in it too
    private static Optional<TypeSymbol> tupleMemberType(TupleTypeSymbol tupleType, OptionalInt position) {
        List<TypeSymbol> memberTypes = tupleType.memberTypeDescriptors();
        return position.isPresent() && position.getAsInt() >= 0 && position.getAsInt() < memberTypes.size()
                ? Optional.of(memberTypes.get(position.getAsInt()))
                : tupleType.restTypeDescriptor();
    }

    // The position of the member among the values of the list, unless a spread before it has an unknown length
    private OptionalInt position(ListConstructorExpressionNode list, Node member) {
        List<Node> members = list.expressions().stream().toList();
        int index = members.indexOf(member);
        return index < 0 ? OptionalInt.empty() : length(members.subList(0, index));
    }

    // The number of members of a tuple without a rest type or of an array of fixed size, such as int[2].
    private static OptionalInt fixedLength(TypeSymbol listType) {
        return switch (listType) {
            case TupleTypeSymbol tuple when tuple.restTypeDescriptor().isEmpty() ->
                    OptionalInt.of(tuple.memberTypeDescriptors().size());
            case ArrayTypeSymbol array when array.size().isPresent() -> OptionalInt.of(array.size().get());
            default -> OptionalInt.empty();
        };
    }

    // The number of values the members give, unless a spread member's length is not known from its type.
    private OptionalInt length(List<Node> members) {
        int length = 0;
        for (Node member : members) {
            if (!(member instanceof SpreadMemberNode spread)) {
                length++;
                continue;
            }
            OptionalInt spreadLength = semanticModel.typeOf(spread.expression())
                    .map(SemanticUtils::unwrap)
                    .map(StaticCodeAnalyzer::fixedLength)
                    .orElse(OptionalInt.empty());
            if (spreadLength.isEmpty()) {
                return OptionalInt.empty();
            }
            length += spreadLength.getAsInt();
        }
        return OptionalInt.of(length);
    }

    private static Optional<TypeSymbol> fieldType(TypeSymbol mappingType, SpecificFieldNode field) {
        String name = SemanticUtils.fieldName(field);
        return switch (mappingType) {
            case MapTypeSymbol mapType -> Optional.of(mapType.typeParam());
            // Field descriptors are keyed by the identifier as written, with its escapes
            case RecordTypeSymbol recordType -> Optional.ofNullable(recordType.fieldDescriptors().get(name))
                    .or(() -> recordType.fieldDescriptors().entrySet().stream()
                            .filter(entry -> SemanticUtils.identifierName(entry.getKey()).equals(name))
                            .findFirst()
                            .map(Map.Entry::getValue))
                    .map(RecordFieldSymbol::typeDescriptor)
                    .or(recordType::restTypeDescriptor);
            default -> Optional.empty();
        };
    }


    private static Stream<TypeSymbol> unionMembers(TypeSymbol typeSymbol) {
        TypeSymbol type = SemanticUtils.unwrap(typeSymbol);
        return type instanceof UnionTypeSymbol union
                ? union.memberTypeDescriptors().stream().flatMap(StaticCodeAnalyzer::unionMembers)
                : Stream.of(type);
    }

    /**
     * Checks whether the method implements a method of an object type included with {@code *T}, or of the type
     * of its service declaration or object constructor. That type fixes the parameters of such a method.
     */
    private boolean isDeclaredByIncludedType(FunctionDefinitionNode method) {
        if (method.functionSignature().parameters().isEmpty()) {
            return false;
        }
        Node parent = method.parent();
        if (!(parent instanceof ClassDefinitionNode || parent instanceof ServiceDeclarationNode
                || parent instanceof ObjectConstructorExpressionNode)) {
            return false;
        }
        Set<MethodSymbol> implementedMethods = implementedMethodsByParent.computeIfAbsent(parent,
                this::implementedMethods);
        return semanticModel.symbol(method)
                .filter(MethodSymbol.class::isInstance)
                .filter(implementedMethods::contains)
                .isPresent();
    }

    private Set<MethodSymbol> implementedMethods(Node objectDefinition) {
        Optional<ObjectTypeSymbol> objectType;
        List<TypeSymbol> declaringTypes = new ArrayList<>();
        switch (objectDefinition) {
            case ClassDefinitionNode classDefinition -> objectType = semanticModel.symbol(classDefinition)
                    .filter(ClassSymbol.class::isInstance)
                    .map(ClassSymbol.class::cast);
            case ServiceDeclarationNode serviceDeclaration -> {
                Optional<ServiceDeclarationSymbol> service = semanticModel.symbol(serviceDeclaration)
                        .filter(ServiceDeclarationSymbol.class::isInstance)
                        .map(ServiceDeclarationSymbol.class::cast);
                service.flatMap(ServiceDeclarationSymbol::typeDescriptor).ifPresent(declaringTypes::add);
                return service.map(symbol -> implementedMethods(symbol.methods(), declaringTypes)).orElse(Set.of());
            }
            case ObjectConstructorExpressionNode objectConstructor -> {
                objectType = semanticModel.typeOf(objectConstructor)
                        .map(SemanticUtils::unwrap)
                        .filter(ObjectTypeSymbol.class::isInstance)
                        .map(ObjectTypeSymbol.class::cast);
                declaringTypes.addAll(contextualTypes(objectConstructor));
            }
            default -> objectType = Optional.empty();
        }
        if (objectType.isEmpty()) {
            return Set.of();
        }
        declaringTypes.addAll(objectType.get().typeInclusions());
        return implementedMethods(objectType.get().methods(), declaringTypes);
    }

    private static Set<MethodSymbol> implementedMethods(Map<String, ? extends MethodSymbol> methods,
                                                        List<TypeSymbol> declaringTypes) {
        Set<String> declaredMethodNames = declaringTypes.stream()
                .flatMap(StaticCodeAnalyzer::unionMembers)
                .filter(ObjectTypeSymbol.class::isInstance)
                .flatMap(type -> ((ObjectTypeSymbol) type).methods().keySet().stream())
                .collect(Collectors.toSet());
        return methods.entrySet().stream()
                .filter(entry -> declaredMethodNames.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    @Override
    public void visit(ClassDefinitionNode classDefinitionNode) {
        if (!isTestSource) {
            checkNonIsolatedPublicClassDefinition(classDefinitionNode);
            checkNonIsolatedPublicClassMethod(classDefinitionNode);
        }
        this.visitSyntaxNode(classDefinitionNode);
    }

    @Override
    public void visit(TypeDefinitionNode typeDefinitionNode) {
        if (!isTestSource) {
            checkNonIsolatedConstructsInTypeDefinition(typeDefinitionNode);
        }
        this.visitSyntaxNode(typeDefinitionNode);
    }

    private void checkNonIsolatedConstructsInTypeDefinition(TypeDefinitionNode typeDefinitionNode) {
        semanticModel.symbol(typeDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                TypeSymbol typeSymbol = typeDefinitionSymbol.typeDescriptor();
                if (typeSymbol instanceof ObjectTypeSymbol objectTypeSymbol) {
                    List<Qualifier> qualifiers = objectTypeSymbol.qualifiers();
                    Optional<Token> visibilityQualifier = typeDefinitionNode.visibilityQualifier();
                    // A service object type is the contract of a listener, and user code never shares its values
                    if (visibilityQualifier.isPresent() && visibilityQualifier.get().kind().equals(PUBLIC_KEYWORD)
                            && !hasQualifier(qualifiers, ISOLATED_KEYWORD)
                            && !hasQualifier(qualifiers, SERVICE_KEYWORD)) {
                        reportIssue(visibilityQualifier.get(), CoreRule.PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT);
                    }
                }
            }
        });
    }

    private void checkNonIsolatedPublicClassDefinition(ClassDefinitionNode classDefinitionNode) {
        semanticModel.symbol(classDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof ObjectTypeSymbol objectTypeSymbol) {
                List<Qualifier> qualifiers = objectTypeSymbol.qualifiers();
                // Instances of a readonly class are immutable, so they can be shared without a lock
                if (isPublicNonIsolatedConstruct(qualifiers)
                        && !hasQualifier(classDefinitionNode.classTypeQualifiers(), READONLY_KEYWORD)) {
                    Token visibilityQualifier = classDefinitionNode.visibilityQualifier().orElseThrow(
                            () -> new IllegalStateException("Expected visibility qualifier to be present"));
                    reportIssue(visibilityQualifier, CoreRule.PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT);
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
        NodeList<Token> qualifiers = member.qualifierList();
        if (isPublicNonIsolatedConstruct(qualifiers)) {
            Token publicToken = getQualifier(qualifiers, PUBLIC_KEYWORD).orElseThrow(
                    () -> new IllegalStateException("Expected public token to be present"));
            reportIssue(publicToken, CoreRule.PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT);
        }
    }

    private void checkNonIsolatedPublicFunction(FunctionDefinitionNode functionDefinitionNode) {
        semanticModel.symbol(functionDefinitionNode).ifPresent(symbol -> {
            if (symbol.kind() != SymbolKind.METHOD) {
                NodeList<Token> qualifiers = functionDefinitionNode.qualifierList();
                if (isPublicNonIsolatedConstruct(qualifiers)) {
                    Token publicToken = getQualifier(qualifiers, PUBLIC_KEYWORD).orElseThrow(
                            () -> new IllegalStateException("Expected public token to be present"));
                    reportIssue(publicToken, CoreRule.PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT);
                }
            }
        });
    }

    private Optional<Token> getQualifier(NodeList<Token> qualifierList, SyntaxKind qualifier) {
        return qualifierList.stream()
                .filter(t -> t.kind() == qualifier)
                .findFirst();
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
                includedRecordParameterNode.paramName().filter(this::isUnusedParameter)
                        .ifPresent(name -> reportIssue(name, CoreRule.UNUSED_FUNCTION_PARAMETER));
            } else if (isUnusedParameter(parameter)) {
                reportIssue(withoutAnnotations(parameter), CoreRule.UNUSED_FUNCTION_PARAMETER);
            }
        });
    }

    private void reportIssue(Node node, CoreRule coreRule) {
        reportIssue(node.location(), coreRule);
    }

    private void reportIssue(Location location, CoreRule coreRule) {
        scannerContext.getReporter().reportIssue(document, location, coreRule.rule());
    }

    private Location withoutMetadata(ObjectFieldNode objectFieldNode) {
        if (objectFieldNode.metadata().isEmpty()) {
            return objectFieldNode.location();
        }
        Node firstNode = objectFieldNode.visibilityQualifier().map(Node.class::cast)
                .or(() -> objectFieldNode.qualifierList().stream().findFirst())
                .orElse(objectFieldNode.typeName());
        return locationFrom(firstNode, objectFieldNode);
    }

    private Location withoutAnnotations(Node parameter) {
        NodeList<AnnotationNode> annotations;
        Node typeName;
        if (parameter instanceof RequiredParameterNode requiredParameterNode) {
            annotations = requiredParameterNode.annotations();
            typeName = requiredParameterNode.typeName();
        } else if (parameter instanceof DefaultableParameterNode defaultableParameterNode) {
            annotations = defaultableParameterNode.annotations();
            typeName = defaultableParameterNode.typeName();
        } else if (parameter instanceof RestParameterNode restParameterNode) {
            annotations = restParameterNode.annotations();
            typeName = restParameterNode.typeName();
        } else {
            return parameter.location();
        }
        return annotations.isEmpty() ? parameter.location() : locationFrom(typeName, parameter);
    }

    private Location locationFrom(Node firstNode, Node node) {
        TextDocument textDocument = document.syntaxTree().textDocument();
        int startOffset = firstNode.textRange().startOffset();
        int endOffset = node.textRange().endOffset();
        LinePosition start = textDocument.linePositionFrom(startOffset);
        LinePosition end = textDocument.linePositionFrom(endOffset);
        return new SourceLocation(LineRange.from(node.lineRange().fileName(), start, end),
                TextRange.from(startOffset, endOffset - startOffset));
    }

    // Valid only for a symbol declared in this document, as a parameter is. The references of a symbol, unlike its
    // references in a document, miss or repeat the definition of a parameter of an object constructor method.
    private boolean isUnusedParameter(Node parameter) {
        return semanticModel.symbol(parameter)
                .filter(symbol -> semanticModel.references(symbol, document, false).isEmpty())
                .isPresent();
    }

    private Optional<Long> getIntValueFromRangeExprNode(Node rangeExprNode) {
        boolean isMinusOperatorPresent = false;

        if (rangeExprNode instanceof UnaryExpressionNode unaryNode) {
            SyntaxKind unaryOperator = unaryNode.unaryOperator().kind();
            if (unaryOperator != SyntaxKind.MINUS_TOKEN && unaryOperator != SyntaxKind.PLUS_TOKEN) {
                return Optional.empty();
            }
            isMinusOperatorPresent = unaryOperator == SyntaxKind.MINUS_TOKEN;
            rangeExprNode = unaryNode.expression();
        }

        if (!(rangeExprNode instanceof BasicLiteralNode literalNode)) {
            return Optional.empty();
        }
        Token literal = literalNode.literalToken();
        try {
            // The digits are parsed with the sign, so that -9223372036854775808 does not overflow
            String sign = isMinusOperatorPresent ? "-" : "";
            if (literal.kind() == SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN) {
                return Optional.of(Long.parseLong(sign + literal.text()));
            }
            if (literal.kind() == SyntaxKind.HEX_INTEGER_LITERAL_TOKEN) {
                return Optional.of(Long.parseLong(sign + literal.text().substring(2), 16));
            }
        } catch (NumberFormatException e) {
            // A literal out of the int range is a compile error, which is reported by the compiler
        }
        return Optional.empty();
    }

    private boolean isPublicNonIsolatedConstruct(NodeList<Token> qualifiers) {
        return hasQualifier(qualifiers, PUBLIC_KEYWORD) && !hasQualifier(qualifiers, ISOLATED_KEYWORD);
    }

    private boolean isPublicNonIsolatedConstruct(List<Qualifier> qualifiers) {
        return hasQualifier(qualifiers, PUBLIC_KEYWORD) && !hasQualifier(qualifiers, ISOLATED_KEYWORD);
    }
}
