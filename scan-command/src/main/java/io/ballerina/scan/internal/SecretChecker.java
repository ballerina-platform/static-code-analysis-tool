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
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.SingletonTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
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
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;
import static io.ballerina.scan.internal.SensitiveParameterTracker.FUNCTIONS_WITH_SENSITIVE_PARAMETERS;

public class SecretChecker extends NodeVisitor {
    private static final Pattern SECRET_WORDS = Pattern.compile(buildSecretKeywordPattern(),
            Pattern.CASE_INSENSITIVE);
    // Any scheme, including compound ones such as "mongodb+srv" and "jdbc:mysql", anywhere in the value.
    private static final Pattern URL_WITH_CREDENTIALS = Pattern.compile("(?<![A-Za-z0-9+.-])[A-Za-z][A-Za-z0-9+.-]*"
            + "(?::[A-Za-z][A-Za-z0-9+.-]*)?://(?<user>[^\\s:/@]*):(?<password>[^\\s/@]+)@");
    // An interpolated part of a URL template is not hard-coded, so it is treated as a template placeholder.
    private static final String INTERPOLATION_PLACEHOLDER = "${}";

    private static String buildSecretKeywordPattern() {
        String[] baseKeywords = {
                "password", "passwd", "pwd", "passphrase", 
                "secret", "auth", "apiKey", "token"
        };

        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < baseKeywords.length; i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            String keyword = baseKeywords[i];
            patternBuilder.append(keyword);
            patternBuilder.append("|").append(toSnakeCase(keyword));
            patternBuilder.append("|").append(toKebabCase(keyword));
        }
        return patternBuilder.toString();
    }

    private static String toSnakeCase(String keyword) {
        return keyword.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ENGLISH);
    }

    private static String toKebabCase(String keyword) {
        return keyword.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ENGLISH);
    }

    private final Document document;
    private final SyntaxTree syntaxTree;
    private final ScannerContext scannerContext;
    private final SemanticModel semanticModel;
    private final Map<Integer, FunctionWithSensitiveParams> functionsWithCredentialParams;
    private final Set<String> reportedIssueKeys = new HashSet<>();

    SecretChecker(Document document, ScannerContext scannerContext, SemanticModel semanticModel) {
        this.document = document;
        this.syntaxTree = document.syntaxTree();
        this.scannerContext = scannerContext;
        this.semanticModel = semanticModel;
        this.functionsWithCredentialParams = getFunctionsWithCredentialParams(scannerContext);
    }

    private Map<Integer, FunctionWithSensitiveParams> getFunctionsWithCredentialParams(ScannerContext scannerContext) {
        @SuppressWarnings("unchecked")
        Map<Integer, FunctionWithSensitiveParams> functionsWithCredentialParams =
                (Map<Integer, FunctionWithSensitiveParams>) scannerContext.userData()
                        .getOrDefault(FUNCTIONS_WITH_SENSITIVE_PARAMETERS, new HashMap<>());
        return functionsWithCredentialParams;
    }

    public void analyze() {
        this.visit((ModulePartNode) syntaxTree.rootNode());
    }

    public static boolean isSecretName(String name) {
        return SECRET_WORDS.matcher(name).find();
    }

    @Override
    public void visit(DefaultableParameterNode defaultableParameterNode) {
        if (isSecretName(defaultableParameterNode.paramName().map(Token::text).orElse(""))
                && defaultableParameterNode.expression() instanceof ExpressionNode expressionNode) {
            validateExpression(expressionNode);
        }
        super.visit(defaultableParameterNode);
    }

    @Override
    public void visit(SpecificFieldNode specificFieldNode) {
        if (isSecretName(specificFieldNode.fieldName().toSourceCode())) {
            if (specificFieldNode.valueExpr().isEmpty()) {
                validateShorthandNotation(specificFieldNode);
            } else {
                validateExpression(specificFieldNode.valueExpr().get());
            }
        }
        super.visit(specificFieldNode);
    }

    private void validateShorthandNotation(SpecificFieldNode specificFieldNode) {
        Optional<Symbol> identifier = semanticModel.symbol(specificFieldNode);
        if (identifier.isEmpty()) {
            return;
        }
        if (identifier.get() instanceof ConstantSymbol constantSymbol) {
            if (!isPlaceholderConstant(constantSymbol)) {
                reportHardCodedSecret(specificFieldNode);
            }
            return;
        }
        if (identifier.get().kind() == SymbolKind.VARIABLE) {
            VariableSymbol variableSymbol = (VariableSymbol) identifier.get();
            if (canHoldSecret(variableSymbol.typeDescriptor()) && !isConfigurable(variableSymbol)) {
                reportNonConfigurableSecret(specificFieldNode);
            }
        }
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
        if (!matcher.find()) {
            return false;
        }
        String password = matcher.group("password");
        return !password.contains(INTERPOLATION_PLACEHOLDER) && !SecretValueFilter.isPlaceholder(password);
    }

    @Override
    public void visit(TemplateExpressionNode templateExpressionNode) {
        if (templateExpressionNode.kind() == SyntaxKind.STRING_TEMPLATE_EXPRESSION) {
            String pattern = buildStringPattern(templateExpressionNode.content());
            if (isUrlWithCredentials(pattern)) {
                reportHardCodedSecret(templateExpressionNode);
            }
        }
        super.visit(templateExpressionNode);
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
        if (isSecretName(moduleVariableDeclarationNode.typedBindingPattern().bindingPattern().toSourceCode())
                && moduleVariableDeclarationNode.initializer().isPresent()) {
            validateExpression(moduleVariableDeclarationNode.initializer().get());
        }
        super.visit(moduleVariableDeclarationNode);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        if (isSecretName(variableDeclarationNode.typedBindingPattern().bindingPattern().toSourceCode())
                && variableDeclarationNode.initializer().isPresent()) {
            validateExpression(variableDeclarationNode.initializer().get());
        }
        super.visit(variableDeclarationNode);
    }

    @Override
    public void visit(NamedArgumentNode namedArgumentNode) {
        if (isSecretName(namedArgumentNode.argumentName().name().text())) {
            validateExpression(namedArgumentNode.expression());
        }
        super.visit(namedArgumentNode);
    }

    @Override
    public void visit(RecordFieldWithDefaultValueNode recordFieldWithDefaultValueNode) {
        if (isSecretName(recordFieldWithDefaultValueNode.fieldName().text().trim())) {
            validateExpression(recordFieldWithDefaultValueNode.expression());
        }
        super.visit(recordFieldWithDefaultValueNode);
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        if (isSecretName(objectFieldNode.fieldName().text().trim())
                && objectFieldNode.expression().isPresent()) {
            validateExpression(objectFieldNode.expression().get());
        }
        super.visit(objectFieldNode);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        validateFunctionCall(functionCallExpressionNode, functionCallExpressionNode.arguments());
        super.visit(functionCallExpressionNode);
    }

    @Override
    public void visit(ConstantDeclarationNode constantDeclarationNode) {
        if (isSecretName(constantDeclarationNode.variableName().text())) {
            Node initializer = constantDeclarationNode.initializer();
            if (initializer instanceof ExpressionNode expressionNode) {
                validateExpression(expressionNode);
            }
        }
        super.visit(constantDeclarationNode);
    }

    @Override
    public void visit(MethodCallExpressionNode methodCallExpressionNode) {
        validateFunctionCall(methodCallExpressionNode, methodCallExpressionNode.arguments());
        super.visit(methodCallExpressionNode);
    }

    private void validateFunctionCall(Node functionNode, NodeList<FunctionArgumentNode> arguments) {
        Optional<Symbol> functionSymbol = semanticModel.symbol(functionNode);
        functionSymbol.map(Symbol::hashCode)
                .filter(functionsWithCredentialParams::containsKey)
                .ifPresent(hashCode -> validateFunctionArguments(arguments, functionsWithCredentialParams.get(hashCode)
                        .sensitiveParamPositions()));
    }

    private void validateFunctionArguments(NodeList<FunctionArgumentNode> arguments,
                                           List<Integer> sensitiveParameterPositions) {
        int currentPos = 0;
        Iterator<Integer> targetPositions = sensitiveParameterPositions.iterator();
        int targetPos = targetPositions.hasNext() ? targetPositions.next() : -1;

        for (FunctionArgumentNode arg : arguments) {
            if (arg.kind() != SyntaxKind.POSITIONAL_ARG || targetPos == -1) {
                return;
            }

            if (currentPos == targetPos) {
                validateArgument((PositionalArgumentNode) arg);
                targetPos = targetPositions.hasNext() ? targetPositions.next() : -1;
            }
            currentPos++;
        }
    }

    private void validateArgument(PositionalArgumentNode argumentNode) {
        validateExpression(argumentNode.expression());
    }

    private void validateExpression(ExpressionNode expressionNode) {
        if (expressionNode.kind() == SyntaxKind.STRING_LITERAL) {
            validateStringLiteral((BasicLiteralNode) expressionNode);
            return;
        }
        Optional<TypeSymbol> type = semanticModel.typeOf(expressionNode);
        if (expressionNode.kind() == SyntaxKind.LIST_CONSTRUCTOR
                && !type.map(SecretChecker::isByteArray).orElse(false)) {
            ((ListConstructorExpressionNode) expressionNode).expressions().forEach(member -> {
                if (member instanceof ExpressionNode memberExpression) {
                    validateExpression(memberExpression);
                }
            });
            return;
        }
        if (expressionNode.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
            // A map of secrets is checked value by value, while a record's fields are checked by their own names.
            if (type.map(SecretChecker::isMap).orElse(false)) {
                ((MappingConstructorExpressionNode) expressionNode).fields().forEach(this::validateMapValue);
            }
            return;
        }
        if (!type.map(SecretChecker::canHoldSecret).orElse(true)) {
            return;
        }
        switch (expressionNode.kind()) {
            case SIMPLE_NAME_REFERENCE -> validateSimpleNameReference(expressionNode);
            case REQUIRED_EXPRESSION -> { }
            default -> reportNonConfigurableSecret(expressionNode);
        }
    }

    private void validateMapValue(MappingFieldNode field) {
        if (field instanceof SpecificFieldNode specificField) {
            specificField.valueExpr().ifPresentOrElse(this::validateExpression,
                    () -> validateShorthandNotation(specificField));
        }
    }

    private void validateStringLiteral(BasicLiteralNode stringLiteralNode) {
        if (!SecretValueFilter.isPlaceholder(SecretValueFilter.literalValue(stringLiteralNode.literalToken().text()))) {
            reportHardCodedSecret(stringLiteralNode);
        }
    }

    // A value can hold a secret when its type allows a string or a byte array. Records, objects, booleans, numbers
    // and nil with secret-like names cannot. A type that could not be resolved is assumed to.
    private static boolean canHoldSecret(TypeSymbol typeSymbol) {
        TypeSymbol type = unwrap(typeSymbol);
        return switch (type) {
            case ArrayTypeSymbol arrayType -> isByte(arrayType.memberTypeDescriptor());
            // A byte array constant is typed by the shape of its value, a tuple of integer singletons.
            case TupleTypeSymbol tupleType -> !tupleType.memberTypeDescriptors().isEmpty()
                    && tupleType.memberTypeDescriptors().stream().allMatch(SecretChecker::isByte);
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

    private static boolean isMap(TypeSymbol typeSymbol) {
        return unwrap(typeSymbol).typeKind() == TypeDescKind.MAP;
    }

    private static TypeSymbol unwrap(TypeSymbol typeSymbol) {
        return switch (typeSymbol) {
            case TypeReferenceTypeSymbol typeReference -> unwrap(typeReference.typeDescriptor());
            case IntersectionTypeSymbol intersectionType -> unwrap(intersectionType.effectiveTypeDescriptor());
            case SingletonTypeSymbol singletonType -> unwrap(singletonType.originalType());
            default -> typeSymbol;
        };
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

    private void validateSimpleNameReference(ExpressionNode expressionNode) {
        Optional<Symbol> identifier = semanticModel.symbol(expressionNode);
        if (identifier.isEmpty()) {
            return;
        }
        if (identifier.get().kind() == SymbolKind.VARIABLE) {
            VariableSymbol variableSymbol = (VariableSymbol) identifier.get();
            if (!isConfigurable(variableSymbol)) {
                reportNonConfigurableSecret(expressionNode);
            }
            return;
        }
        if (identifier.get() instanceof ConstantSymbol constantSymbol && !isPlaceholderConstant(constantSymbol)) {
            reportHardCodedSecret(expressionNode);
        }
    }

    private static boolean isPlaceholderConstant(ConstantSymbol constantSymbol) {
        return constantSymbol.resolvedValue()
                .map(value -> value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")
                        ? value.substring(1, value.length() - 1)
                        : value)
                .map(SecretValueFilter::isPlaceholder)
                .orElse(false);
    }

    private boolean isConfigurable(VariableSymbol variableSymbol) {
        return variableSymbol.qualifiers().stream()
                .anyMatch(qualifier -> Qualifier.CONFIGURABLE.name().equals(qualifier.name().trim()));
    }
}
