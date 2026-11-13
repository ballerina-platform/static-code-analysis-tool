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
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ObjectFieldSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.SingletonTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ConditionalExpressionNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;
import static io.ballerina.scan.internal.SecretNameMatcher.kind;
import static io.ballerina.scan.internal.SemanticUtils.unwrap;

public class SecretChecker extends NodeVisitor {
    // Any scheme, including compound ones such as "mongodb+srv" and "jdbc:mysql", anywhere in the value.
    private static final Pattern URL_WITH_CREDENTIALS = Pattern.compile("(?<![A-Za-z0-9+.-])[A-Za-z][A-Za-z0-9+.-]*"
            + "(?::[A-Za-z][A-Za-z0-9+.-]*)?://(?<user>[^\\s:/@]*):(?<password>[^\\s/@]+)@");
    // An interpolated part of a URL template is not hard-coded, so it is treated as a template placeholder.
    private static final String INTERPOLATION_PLACEHOLDER = "${}";

    private final Document document;
    private final SyntaxTree syntaxTree;
    private final ScannerContext scannerContext;
    private final SemanticModel semanticModel;
    private final SecretValueResolver valueResolver;
    private final Set<String> reportedIssueKeys = new HashSet<>();
    private final Map<String, SecretNameMatcher.Kind> secretParameterKinds = new HashMap<>();

    SecretChecker(Document document, ScannerContext scannerContext, SemanticModel semanticModel,
                  SecretValueResolver valueResolver) {
        this.document = document;
        this.syntaxTree = document.syntaxTree();
        this.scannerContext = scannerContext;
        this.semanticModel = semanticModel;
        this.valueResolver = valueResolver;
    }

    public void analyze() {
        this.visit((ModulePartNode) syntaxTree.rootNode());
    }

    @Override
    public void visit(DefaultableParameterNode defaultableParameterNode) {
        SecretNameMatcher.Kind kind = kind(defaultableParameterNode.paramName().map(Token::text).orElse(""));
        if (kind.isSecret() && defaultableParameterNode.expression() instanceof ExpressionNode expressionNode) {
            validateExpression(expressionNode, kind);
        }
        super.visit(defaultableParameterNode);
    }

    @Override
    public void visit(SpecificFieldNode specificFieldNode) {
        SecretNameMatcher.Kind kind = kind(fieldNameText(specificFieldNode));
        if (kind.isSecret()) {
            if (specificFieldNode.valueExpr().isEmpty()) {
                validateShorthandNotation(specificFieldNode, kind);
            } else {
                validateExpression(specificFieldNode.valueExpr().get(), kind);
            }
        }
        super.visit(specificFieldNode);
    }

    // The field name without its minutiae, so that a comment above the field is not read as part of its name.
    static String fieldNameText(SpecificFieldNode specificFieldNode) {
        Node fieldName = specificFieldNode.fieldName();
        return switch (fieldName) {
            case Token token -> token.text();
            case BasicLiteralNode literal -> literal.literalToken().text();
            default -> fieldName.toSourceCode().trim();
        };
    }

    private void validateShorthandNotation(SpecificFieldNode specificFieldNode, SecretNameMatcher.Kind kind) {
        semanticModel.symbol(specificFieldNode)
                .filter(symbol -> !(symbol instanceof VariableSymbol variable)
                        || canHoldSecret(variable.typeDescriptor()))
                .ifPresent(symbol -> report(specificFieldNode, valueResolver.resolveSymbol(symbol), kind));
    }

    @Override
    public void visit(BasicLiteralNode basicLiteralNode) {
        if (basicLiteralNode.kind() == SyntaxKind.STRING_LITERAL) {
            String cleanedLiteral = SecretValueFilter.literalValue(basicLiteralNode.literalToken().text());
            if (isUrlWithCredentials(cleanedLiteral)) {
                reportHardCodedSecret(basicLiteralNode);
            }
        }
        super.visit(basicLiteralNode);
    }

    private static boolean isUrlWithCredentials(String url) {
        Matcher matcher = URL_WITH_CREDENTIALS.matcher(url);
        while (matcher.find()) {
            if (!SecretValueFilter.isPlaceholder(matcher.group("password"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(TemplateExpressionNode templateExpressionNode) {
        if (isUrlTemplateWithCredentials(templateExpressionNode)) {
            reportHardCodedSecret(templateExpressionNode);
        }
        super.visit(templateExpressionNode);
    }

    private boolean containsUrlWithCredentials(Node node) {
        return switch (node) {
            case BasicLiteralNode literal -> literal.kind() == SyntaxKind.STRING_LITERAL
                    && isUrlWithCredentials(SecretValueFilter.literalValue(literal.literalToken().text()));
            case TemplateExpressionNode template -> isUrlTemplateWithCredentials(template);
            case NonTerminalNode parent -> {
                for (Node child : parent.children()) {
                    if (containsUrlWithCredentials(child)) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean isUrlTemplateWithCredentials(TemplateExpressionNode templateExpressionNode) {
        return templateExpressionNode.kind() == SyntaxKind.STRING_TEMPLATE_EXPRESSION
                && isUrlWithCredentials(buildStringPattern(templateExpressionNode.content()));
    }

    private String buildStringPattern(NodeList<Node> content) {
        StringBuilder pattern = new StringBuilder();
        for (Node node : content) {
            if (node.kind() == SyntaxKind.TEMPLATE_STRING) {
                pattern.append(node.toSourceCode());
            } else {
                pattern.append(INTERPOLATION_PLACEHOLDER);
            }
        }
        return pattern.toString();
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        SecretNameMatcher.Kind kind = kind(variableName(moduleVariableDeclarationNode.typedBindingPattern()));
        if (kind.isSecret() && moduleVariableDeclarationNode.initializer().isPresent()) {
            validateExpression(moduleVariableDeclarationNode.initializer().get(), kind);
        }
        super.visit(moduleVariableDeclarationNode);
    }

    // The name of a variable declared on its own; a destructuring pattern declares no single secret-named variable.
    static String variableName(TypedBindingPatternNode typedBindingPattern) {
        return typedBindingPattern.bindingPattern() instanceof CaptureBindingPatternNode captureBindingPattern
                ? captureBindingPattern.variableName().text()
                : "";
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        SecretNameMatcher.Kind kind = kind(variableName(variableDeclarationNode.typedBindingPattern()));
        if (kind.isSecret() && variableDeclarationNode.initializer().isPresent()) {
            validateExpression(variableDeclarationNode.initializer().get(), kind);
        }
        super.visit(variableDeclarationNode);
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        SecretNameMatcher.Kind kind = targetKind(assignmentStatementNode.varRef());
        if (kind.isSecret()) {
            validateExpression(assignmentStatementNode.expression(), kind);
        }
        super.visit(assignmentStatementNode);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        if (compoundAssignmentStatementNode.binaryOperator().kind() == SyntaxKind.PLUS_TOKEN) {
            SecretNameMatcher.Kind kind = targetKind(compoundAssignmentStatementNode.lhsExpression());
            if (kind.isSecret()) {
                validateExpression(compoundAssignmentStatementNode.rhsExpression(), kind);
            }
        }
        super.visit(compoundAssignmentStatementNode);
    }

    private static SecretNameMatcher.Kind targetKind(Node target) {
        return switch (target) {
            case SimpleNameReferenceNode variable -> kind(variable.name().text());
            case FieldAccessExpressionNode fieldAccess -> kind(fieldAccess.fieldName().toSourceCode().trim());
            case IndexedExpressionNode memberAccess -> SecretValueResolver.memberKey(memberAccess)
                    .map(SecretNameMatcher::kind)
                    .orElse(SecretNameMatcher.Kind.NONE);
            default -> SecretNameMatcher.Kind.NONE;
        };
    }

    @Override
    public void visit(NamedArgumentNode namedArgumentNode) {
        SecretNameMatcher.Kind kind = kind(namedArgumentNode.argumentName().name().text());
        if (kind.isSecret()) {
            validateExpression(namedArgumentNode.expression(), kind);
        }
        super.visit(namedArgumentNode);
    }

    @Override
    public void visit(RecordFieldWithDefaultValueNode recordFieldWithDefaultValueNode) {
        SecretNameMatcher.Kind kind = kind(recordFieldWithDefaultValueNode.fieldName().text().trim());
        if (kind.isSecret()) {
            validateExpression(recordFieldWithDefaultValueNode.expression(), kind);
        }
        super.visit(recordFieldWithDefaultValueNode);
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        SecretNameMatcher.Kind kind = kind(objectFieldNode.fieldName().text().trim());
        if (kind.isSecret() && objectFieldNode.expression().isPresent()) {
            validateExpression(objectFieldNode.expression().get(), kind);
        }
        super.visit(objectFieldNode);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        validateFunctionCall(functionCallExpressionNode, functionCallExpressionNode.arguments());
        super.visit(functionCallExpressionNode);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        validateFunctionCall(remoteMethodCallActionNode, remoteMethodCallActionNode.arguments());
        super.visit(remoteMethodCallActionNode);
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        clientResourceAccessActionNode.arguments().ifPresent(argList ->
                validateFunctionCall(clientResourceAccessActionNode, argList.arguments()));
        super.visit(clientResourceAccessActionNode);
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        implicitNewExpressionNode.parenthesizedArgList().ifPresent(argList ->
                validateNewExpression(implicitNewExpressionNode, argList));
        super.visit(implicitNewExpressionNode);
    }

    @Override
    public void visit(ExplicitNewExpressionNode explicitNewExpressionNode) {
        validateNewExpression(explicitNewExpressionNode, explicitNewExpressionNode.parenthesizedArgList());
        super.visit(explicitNewExpressionNode);
    }

    @Override
    public void visit(MethodCallExpressionNode methodCallExpressionNode) {
        validateFunctionCall(methodCallExpressionNode, methodCallExpressionNode.arguments());
        super.visit(methodCallExpressionNode);
    }

    private void validateFunctionCall(Node functionNode, NodeList<FunctionArgumentNode> arguments) {
        if (!hasPositionalArgument(arguments)) {
            return;
        }
        Optional<Symbol> symbol = semanticModel.symbol(functionNode);
        if (symbol.isEmpty()) {
            return;
        }
        if (symbol.get() instanceof FunctionSymbol function) {
            validatePositionalArguments(arguments, function.typeDescriptor(),
                    isLangLibMethodCall(functionNode, function) ? 1 : 0);
            return;
        }
        // A call through a function-typed variable, parameter or field, whose type may still name its parameters.
        getFunctionType(symbol.get())
                .ifPresent(functionType -> validatePositionalArguments(arguments, functionType, 0));
    }

    private static Optional<FunctionTypeSymbol> getFunctionType(Symbol symbol) {
        Optional<TypeSymbol> type = switch (symbol) {
            case VariableSymbol variable -> Optional.of(variable.typeDescriptor());
            case ParameterSymbol parameter -> Optional.of(parameter.typeDescriptor());
            case ObjectFieldSymbol objectField -> Optional.of(objectField.typeDescriptor());
            default -> Optional.empty();
        };
        return type.map(SemanticUtils::unwrap)
                .filter(FunctionTypeSymbol.class::isInstance)
                .map(FunctionTypeSymbol.class::cast);
    }

    // A lang library function called as a method, as in value.f(a), receives the value as its first parameter.
    private static boolean isLangLibMethodCall(Node functionNode, FunctionSymbol function) {
        return functionNode.kind() == SyntaxKind.METHOD_CALL && SemanticUtils.isLangLib(function);
    }

    private void validateNewExpression(Node newExpressionNode, ParenthesizedArgList argList) {
        if (!hasPositionalArgument(argList.arguments())) {
            return;
        }
        semanticModel.typeOf(newExpressionNode)
                .flatMap(SecretChecker::getClassSymbol)
                .flatMap(ClassSymbol::initMethod)
                .ifPresent(initMethod -> validatePositionalArguments(argList.arguments(),
                        initMethod.typeDescriptor(), 0));
    }

    private static boolean hasPositionalArgument(NodeList<FunctionArgumentNode> arguments) {
        return !arguments.isEmpty() && arguments.get(0).kind() == SyntaxKind.POSITIONAL_ARG;
    }

    private static Optional<ClassSymbol> getClassSymbol(TypeSymbol typeSymbol) {
        return switch (unwrap(typeSymbol)) {
            case ClassSymbol classSymbol -> Optional.of(classSymbol);
            case UnionTypeSymbol unionType -> unionType.memberTypeDescriptors().stream()
                    .map(SecretChecker::getClassSymbol)
                    .flatMap(Optional::stream)
                    .findFirst();
            default -> Optional.empty();
        };
    }

    private void validatePositionalArguments(NodeList<FunctionArgumentNode> arguments,
                                             FunctionTypeSymbol functionType, int firstParameter) {
        List<ParameterSymbol> parameters = functionType.params().orElse(List.of());
        Optional<ParameterSymbol> restParameter = functionType.restParam();
        int position = firstParameter;
        for (FunctionArgumentNode argument : arguments) {
            if (argument.kind() != SyntaxKind.POSITIONAL_ARG) {
                return;
            }
            Optional<ParameterSymbol> parameter = position < parameters.size()
                    ? Optional.of(parameters.get(position))
                    : restParameter;
            SecretNameMatcher.Kind kind = parameter.flatMap(ParameterSymbol::getName)
                    .map(this::parameterKind)
                    .orElse(SecretNameMatcher.Kind.NONE);
            if (kind.isSecret()) {
                validateExpression(((PositionalArgumentNode) argument).expression(), kind);
            }
            position++;
        }
    }

    // The same parameters are matched at every call to a function, so their names are matched once.
    private SecretNameMatcher.Kind parameterKind(String name) {
        return secretParameterKinds.computeIfAbsent(name, SecretNameMatcher::kind);
    }

    private void validateExpression(ExpressionNode expressionNode, SecretNameMatcher.Kind kind) {
        // Each branch of a conditional or elvis expression is checked, and reported, where it is written.
        if (expressionNode instanceof ConditionalExpressionNode conditional) {
            validateExpression(conditional.middleExpression(), kind);
            validateExpression(conditional.endExpression(), kind);
            return;
        }
        if (expressionNode instanceof BinaryExpressionNode binary
                && binary.operator().kind() == SyntaxKind.ELVIS_TOKEN) {
            Stream.of(binary.lhsExpr(), binary.rhsExpr())
                    .filter(ExpressionNode.class::isInstance)
                    .forEach(operand -> validateExpression((ExpressionNode) operand, kind));
            return;
        }
        if (expressionNode.kind() == SyntaxKind.STRING_LITERAL) {
            report(expressionNode, valueResolver.resolve(expressionNode), kind);
            return;
        }
        Optional<TypeSymbol> type = semanticModel.typeOf(expressionNode);
        if (expressionNode.kind() == SyntaxKind.LIST_CONSTRUCTOR
                && !type.map(SecretChecker::isByteArray).orElse(false)) {
            ((ListConstructorExpressionNode) expressionNode).expressions().forEach(member -> {
                if (member instanceof ExpressionNode memberExpression) {
                    validateExpression(memberExpression, kind);
                }
            });
            return;
        }
        if (expressionNode.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
            // A map of secrets is checked value by value, while a record's fields are checked by their own names.
            if (type.map(SecretChecker::isMap).orElse(true)) {
                ((MappingConstructorExpressionNode) expressionNode).fields()
                        .forEach(field -> validateMapValue(field, kind));
            }
            return;
        }
        if (!valueResolver.isMapConstant(expressionNode)
                && !type.map(valueType -> canHoldSecret(slotType(expressionNode, valueType))).orElse(true)) {
            return;
        }
        // A URL with credentials is reported once, as ballerina:13, when the literal or template is visited.
        if (containsUrlWithCredentials(expressionNode)) {
            return;
        }
        report(expressionNode, valueResolver.resolve(expressionNode), kind);
    }

    // A list constant is typed by the shape of its value, so the type of the slot it is given to tells int[] from
    // byte[].
    private TypeSymbol slotType(ExpressionNode expressionNode, TypeSymbol valueType) {
        if (!(unwrap(valueType) instanceof TupleTypeSymbol)) {
            return valueType;
        }
        return semanticModel.expectedType(document, expressionNode.lineRange().startLine())
                .filter(expectedType -> unwrap(expectedType) instanceof ArrayTypeSymbol)
                .orElse(valueType);
    }

    // A field of a map of secrets is checked with the stricter of its own kind and the map's, as a token in a map of
    // passwords can hold any value of a password.
    private void validateMapValue(MappingFieldNode field, SecretNameMatcher.Kind kind) {
        if (field instanceof SpecificFieldNode specificField) {
            SecretNameMatcher.Kind fieldKind = kind(fieldNameText(specificField)).stricter(kind);
            specificField.valueExpr().ifPresentOrElse(value -> validateExpression(value, fieldKind),
                    () -> validateShorthandNotation(specificField, fieldKind));
        }
    }

    private void report(Node node, SecretValueResolver.Value value, SecretNameMatcher.Kind kind) {
        if (!value.isReported(kind)) {
            return;
        }
        switch (value.origin()) {
            case HARD_CODED -> reportHardCodedSecret(node);
            case FIXED_VARIABLE -> reportNonConfigurableSecret(node);
            default -> { }
        }
    }

    // A value can hold a secret when its type allows a string or a byte array. Records, objects, booleans, numbers
    // and nil with secret-like names cannot. A type that could not be resolved is assumed to.
    private static boolean canHoldSecret(TypeSymbol typeSymbol) {
        TypeSymbol type = unwrap(typeSymbol);
        return switch (type) {
            case ArrayTypeSymbol arrayType -> isByte(arrayType.memberTypeDescriptor())
                    || canHoldSecret(arrayType.memberTypeDescriptor());
            case MapTypeSymbol mapType -> canHoldSecret(mapType.typeParam());
            // A byte array constant is typed by the shape of its value, a tuple of integer singletons.
            case TupleTypeSymbol tupleType -> !tupleType.memberTypeDescriptors().isEmpty()
                    && (tupleType.memberTypeDescriptors().stream().allMatch(SecretChecker::isByte)
                    || tupleType.memberTypeDescriptors().stream().anyMatch(SecretChecker::canHoldSecret));
            case UnionTypeSymbol unionType -> unionType.memberTypeDescriptors().stream()
                    .anyMatch(SecretChecker::canHoldSecret);
            default -> switch (type.typeKind()) {
                case STRING, STRING_CHAR, JSON, ANYDATA, ANY, READONLY, COMPILATION_ERROR -> true;
                default -> false;
            };
        };
    }

    private static boolean isByte(TypeSymbol typeSymbol) {
        TypeSymbol type = unwrap(typeSymbol);
        return type.typeKind() == TypeDescKind.BYTE || (typeSymbol instanceof SingletonTypeSymbol
                && type.typeKind() == TypeDescKind.INT && isByteValue(typeSymbol.signature()));
    }

    private static boolean isByteValue(String value) {
        try {
            int number = Integer.parseInt(value.trim());
            return number >= 0 && number <= 255;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isByteArray(TypeSymbol typeSymbol) {
        return unwrap(typeSymbol) instanceof ArrayTypeSymbol arrayType && isByte(arrayType.memberTypeDescriptor());
    }

    static boolean isMap(TypeSymbol typeSymbol) {
        return unwrap(typeSymbol).typeKind() == TypeDescKind.MAP;
    }

    private void reportHardCodedSecret(Node node) {
        reportIssueOnce(node, HARD_CODED_SECRET.rule());
    }

    private void reportNonConfigurableSecret(Node node) {
        reportIssueOnce(node, NON_CONFIGURABLE_SECRET.rule());
    }

    private void reportIssueOnce(Node node, Rule rule) {
        String issueKey = rule.id() + ":" + node.location().textRange().startOffset() + ":"
                + node.location().textRange().length();
        if (reportedIssueKeys.add(issueKey)) {
            scannerContext.getReporter().reportIssue(document, node.location(), rule);
        }
    }
}
