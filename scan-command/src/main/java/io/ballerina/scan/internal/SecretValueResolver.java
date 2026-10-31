/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.InterpolationNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.OptionalFieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.TextRange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code SecretValueResolver} traces the value given to a secret-named target back to where it comes from, so that
 * {@link SecretChecker} can tell a hard-coded secret from one reached through a fixed variable, a configurable
 * value, or a value computed at run time.
 *
 * <p>Variables are followed through their initializer only when it is the variable's only value: the variable is
 * {@code final}, or it is a local or non-public module variable that is never assigned again. Field access is
 * followed into the mapping constructor that a traced variable is initialized with.</p>
 *
 * @since 0.13.0
 */
class SecretValueResolver {
    private static final int MAX_DEPTH = 5;
    private static final Set<String> TRANSPARENT_CONVERSIONS = Set.of("toBytes", "toBase64", "toBase16", "trim",
            "toLowerAscii", "toUpperAscii");

    /**
     * Where a secret value comes from.
     */
    enum Origin {
        // A literal, a constant, or an expression built only from them.
        HARD_CODED,
        // A non-configurable variable that holds a fixed value.
        FIXED_VARIABLE,
        // A configurable variable without a fixed default.
        CONFIGURABLE,
        // An empty, template or dummy value, which is not a secret on its own but can be part of one.
        PLACEHOLDER,
        // A value computed at run time, a parameter, or a value already reported where it is written.
        UNKNOWN
    }

    private final SemanticModel semanticModel;
    private final Module module;
    private final Map<Symbol, Optional<ExpressionNode>> tracedInitializers = new HashMap<>();
    private final Map<Symbol, Boolean> assignedVariables = new HashMap<>();
    private Map<String, Document> documentsByName;

    SecretValueResolver(SemanticModel semanticModel, Module module) {
        this.semanticModel = semanticModel;
        this.module = module;
    }

    Origin resolve(ExpressionNode expression) {
        return resolve(expression, 0, new HashSet<>());
    }

    Origin resolveSymbol(Symbol symbol) {
        return resolveSymbol(symbol, 0, new HashSet<>());
    }

    private Origin resolve(ExpressionNode expression, int depth, Set<Symbol> visited) {
        if (depth > MAX_DEPTH) {
            return Origin.UNKNOWN;
        }
        return switch (expression.kind()) {
            case STRING_LITERAL -> SecretValueFilter.isPlaceholder(
                    SecretValueFilter.literalValue(((BasicLiteralNode) expression).literalToken().text()))
                    ? Origin.PLACEHOLDER
                    : Origin.HARD_CODED;
            case TYPE_CAST_EXPRESSION -> resolve(((TypeCastExpressionNode) expression).expression(), depth, visited);
            case BYTE_ARRAY_LITERAL -> Origin.HARD_CODED;
            case LIST_CONSTRUCTOR -> isNumericList((ListConstructorExpressionNode) expression)
                    ? Origin.HARD_CODED
                    : Origin.UNKNOWN;
            case REQUIRED_EXPRESSION -> Origin.CONFIGURABLE;
            case BRACED_EXPRESSION -> resolve(((BracedExpressionNode) expression).expression(), depth, visited);
            case SIMPLE_NAME_REFERENCE, QUALIFIED_NAME_REFERENCE -> semanticModel.symbol(expression)
                    .map(symbol -> resolveSymbol(symbol, depth, visited))
                    .orElse(Origin.UNKNOWN);
            case FIELD_ACCESS -> resolveField(((FieldAccessExpressionNode) expression).expression(),
                    ((FieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth, visited);
            case OPTIONAL_FIELD_ACCESS -> resolveField(((OptionalFieldAccessExpressionNode) expression).expression(),
                    ((OptionalFieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth,
                    visited);
            case INDEXED_EXPRESSION -> memberKey((IndexedExpressionNode) expression)
                    .map(key -> resolveField(((IndexedExpressionNode) expression).containerExpression(), key, depth,
                            visited))
                    .orElse(Origin.UNKNOWN);
            case BINARY_EXPRESSION -> resolveConcatenation((BinaryExpressionNode) expression, depth, visited);
            case STRING_TEMPLATE_EXPRESSION -> resolveTemplate((TemplateExpressionNode) expression, depth, visited);
            case METHOD_CALL -> resolveConversion(expression, ((MethodCallExpressionNode) expression).methodName(),
                    ((MethodCallExpressionNode) expression).arguments().isEmpty()
                            ? Optional.of(((MethodCallExpressionNode) expression).expression())
                            : Optional.empty(), depth, visited);
            case FUNCTION_CALL -> resolveConversion(expression,
                    ((FunctionCallExpressionNode) expression).functionName(),
                    singlePositionalArgument((FunctionCallExpressionNode) expression), depth, visited);
            default -> Origin.UNKNOWN;
        };
    }

    private Origin resolveSymbol(Symbol symbol, int depth, Set<Symbol> visited) {
        // A constant is a hard-coded value wherever it is used; its declaration is not a secret on its own.
        if (symbol instanceof ConstantSymbol constant) {
            return isPlaceholder(constant) ? Origin.PLACEHOLDER : Origin.HARD_CODED;
        }
        if (symbol.kind() != SymbolKind.VARIABLE || !visited.add(symbol)) {
            return Origin.UNKNOWN;
        }
        try {
            VariableSymbol variable = (VariableSymbol) symbol;
            Origin initializerOrigin = findTracedInitializer(variable)
                    .map(value -> resolve(value, depth + 1, visited))
                    .orElse(Origin.UNKNOWN);
            if (initializerOrigin == Origin.PLACEHOLDER) {
                return Origin.PLACEHOLDER;
            }
            if (initializerOrigin == Origin.HARD_CODED || initializerOrigin == Origin.FIXED_VARIABLE) {
                // A configurable's literal default is a hard-coded fallback for the secret.
                return reachedThrough(variable, isConfigurable(variable) ? Origin.HARD_CODED : Origin.FIXED_VARIABLE);
            }
            return isConfigurable(variable) ? Origin.CONFIGURABLE : initializerOrigin;
        } finally {
            visited.remove(symbol);
        }
    }

    // A secret-named variable is reported where it is declared, so the values that reach a target through it are not.
    private static Origin reachedThrough(Symbol variable, Origin origin) {
        return SecretNameMatcher.isSecretName(variable.getName().orElse("")) ? Origin.UNKNOWN : origin;
    }

    private Origin resolveField(ExpressionNode container, String fieldName, int depth, Set<Symbol> visited) {
        Optional<Symbol> containerSymbol = container.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE
                ? semanticModel.symbol(container)
                : Optional.empty();
        if (containerSymbol.filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .map(symbol -> isConfigurable((VariableSymbol) symbol))
                .orElse(false)) {
            return Origin.CONFIGURABLE;
        }
        Optional<SpecificFieldNode> field = findMapping(container, depth, visited)
                .flatMap(mapping -> findField(mapping, fieldName));
        if (field.isEmpty()) {
            return Origin.UNKNOWN;
        }
        Origin origin = field.get().valueExpr()
                .map(value -> resolve(value, depth + 1, visited))
                .orElseGet(() -> semanticModel.symbol(field.get())
                        .map(symbol -> resolveSymbol(symbol, depth + 1, visited))
                        .orElse(Origin.UNKNOWN));
        if (origin != Origin.HARD_CODED && origin != Origin.FIXED_VARIABLE) {
            return origin;
        }
        if (isConstant(container)) {
            return Origin.HARD_CODED;
        }
        // A secret-named field is reported where it is written, and any other fixed field is reached through the
        // variable that holds the mapping.
        return SecretNameMatcher.isSecretName(fieldName) ? Origin.UNKNOWN : Origin.FIXED_VARIABLE;
    }

    // Whether a field access chain starts at a constant, as in DEFAULTS.db or DEFAULTS["db"].
    private boolean isConstant(ExpressionNode container) {
        ExpressionNode root = container;
        while (true) {
            switch (root) {
                case FieldAccessExpressionNode fieldAccess -> root = fieldAccess.expression();
                case OptionalFieldAccessExpressionNode optionalFieldAccess -> root = optionalFieldAccess.expression();
                case IndexedExpressionNode memberAccess -> root = memberAccess.containerExpression();
                case BracedExpressionNode braced -> root = braced.expression();
                default -> {
                    return semanticModel.symbol(root).filter(ConstantSymbol.class::isInstance).isPresent();
                }
            }
        }
    }

    private Optional<MappingConstructorExpressionNode> findMapping(ExpressionNode expression, int depth,
                                                                   Set<Symbol> visited) {
        if (depth > MAX_DEPTH) {
            return Optional.empty();
        }
        return switch (expression.kind()) {
            case MAPPING_CONSTRUCTOR -> Optional.of((MappingConstructorExpressionNode) expression);
            case BRACED_EXPRESSION -> findMapping(((BracedExpressionNode) expression).expression(), depth, visited);
            case SIMPLE_NAME_REFERENCE -> semanticModel.symbol(expression)
                    .flatMap(this::findMappingInitializer)
                    .flatMap(initializer -> findMapping(initializer, depth + 1, visited));
            case FIELD_ACCESS -> findFieldMapping(((FieldAccessExpressionNode) expression).expression(),
                    ((FieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth, visited);
            case OPTIONAL_FIELD_ACCESS -> findFieldMapping(
                    ((OptionalFieldAccessExpressionNode) expression).expression(),
                    ((OptionalFieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth,
                    visited);
            case INDEXED_EXPRESSION -> memberKey((IndexedExpressionNode) expression)
                    .flatMap(key -> findFieldMapping(((IndexedExpressionNode) expression).containerExpression(), key,
                            depth, visited));
            default -> Optional.empty();
        };
    }

    private Optional<MappingConstructorExpressionNode> findFieldMapping(ExpressionNode container, String fieldName,
                                                                        int depth, Set<Symbol> visited) {
        return findMapping(container, depth + 1, visited)
                .flatMap(mapping -> findField(mapping, fieldName))
                .flatMap(field -> field.valueExpr().or(() -> semanticModel.symbol(field)
                        .flatMap(this::findMappingInitializer)))
                .flatMap(value -> findMapping(value, depth + 1, visited));
    }

    // A configurable's initializer is only its default, so a mapping is not followed into it. A final variable's
    // mapping can still have its fields assigned, which counts as an assignment of the variable.
    private Optional<ExpressionNode> findMappingInitializer(Symbol symbol) {
        if (symbol instanceof ConstantSymbol) {
            return symbol.getLocation().flatMap(this::findNode).flatMap(SecretValueResolver::findConstantInitializer);
        }
        if (symbol.kind() != SymbolKind.VARIABLE || isConfigurable((VariableSymbol) symbol)
                || isAssigned((VariableSymbol) symbol)) {
            return Optional.empty();
        }
        return findTracedInitializer((VariableSymbol) symbol);
    }

    // The key of a member access with a single string literal key, such as values["db"].
    static Optional<String> memberKey(IndexedExpressionNode memberAccess) {
        if (memberAccess.keyExpression().size() != 1
                || memberAccess.keyExpression().get(0).kind() != SyntaxKind.STRING_LITERAL) {
            return Optional.empty();
        }
        return Optional.of(SecretValueFilter.literalValue(
                ((BasicLiteralNode) memberAccess.keyExpression().get(0)).literalToken().text()));
    }

    private static Optional<SpecificFieldNode> findField(MappingConstructorExpressionNode mapping, String fieldName) {
        for (MappingFieldNode field : mapping.fields()) {
            if (field instanceof SpecificFieldNode specificField
                    && stripFieldNameQuotes(SecretChecker.fieldNameText(specificField)).equals(fieldName)) {
                return Optional.of(specificField);
            }
        }
        return Optional.empty();
    }

    private Origin resolveConcatenation(BinaryExpressionNode binaryExpression, int depth, Set<Symbol> visited) {
        if (binaryExpression.operator().kind() != SyntaxKind.PLUS_TOKEN) {
            return Origin.UNKNOWN;
        }
        return combine(Stream.of(binaryExpression.lhsExpr(), binaryExpression.rhsExpr())
                .map(operand -> operand instanceof ExpressionNode expression
                        ? resolve(expression, depth, visited)
                        : Origin.UNKNOWN));
    }

    private Origin resolveTemplate(TemplateExpressionNode template, int depth, Set<Symbol> visited) {
        String text = template.content().stream()
                .filter(node -> node.kind() == SyntaxKind.TEMPLATE_STRING)
                .map(Node::toSourceCode)
                .reduce("", String::concat);
        List<Origin> interpolations = template.content().stream()
                .filter(InterpolationNode.class::isInstance)
                .map(interpolation -> resolve(((InterpolationNode) interpolation).expression(), depth, visited))
                .toList();
        Origin textOrigin = SecretValueFilter.isPlaceholder(text) ? Origin.PLACEHOLDER : Origin.HARD_CODED;
        return combine(Stream.concat(Stream.of(textOrigin), interpolations.stream()));
    }

    // A value built from fixed parts is fixed, and hard-coded only when no part is reached through a variable. It
    // is a placeholder when every part is, as in "" + "" or "${DB}" + "${PW}".
    private static Origin combine(Stream<Origin> parts) {
        Set<Origin> origins = new HashSet<>();
        parts.forEach(origins::add);
        if (origins.contains(Origin.UNKNOWN) || origins.contains(Origin.CONFIGURABLE)) {
            return Origin.UNKNOWN;
        }
        origins.remove(Origin.PLACEHOLDER);
        if (origins.isEmpty()) {
            return Origin.PLACEHOLDER;
        }
        return origins.contains(Origin.FIXED_VARIABLE) ? Origin.FIXED_VARIABLE : Origin.HARD_CODED;
    }

    // A value converted by a lang library function, such as "secret".toBytes() or string:trim(secret), keeps the
    // origin of the value.
    private Origin resolveConversion(ExpressionNode call, Node functionName, Optional<ExpressionNode> value,
                                     int depth, Set<Symbol> visited) {
        String name = functionName instanceof QualifiedNameReferenceNode qualifiedName
                ? qualifiedName.identifier().text()
                : functionName.toSourceCode().trim();
        boolean isLangLibConversion = value.isPresent() && TRANSPARENT_CONVERSIONS.contains(name)
                && semanticModel.symbol(call)
                .flatMap(Symbol::getModule)
                .map(moduleSymbol -> moduleSymbol.id().orgName().equals("ballerina")
                        && moduleSymbol.id().moduleName().startsWith("lang."))
                .orElse(false);
        if (!isLangLibConversion) {
            return Origin.UNKNOWN;
        }
        Origin origin = resolve(value.get(), depth, visited);
        return origin == Origin.UNKNOWN || origin == Origin.CONFIGURABLE ? Origin.UNKNOWN : origin;
    }

    private static Optional<ExpressionNode> singlePositionalArgument(FunctionCallExpressionNode functionCall) {
        return functionCall.arguments().size() == 1
                && functionCall.arguments().get(0) instanceof PositionalArgumentNode argument
                ? Optional.of(argument.expression())
                : Optional.empty();
    }

    private static Optional<ExpressionNode> findConstantInitializer(Node constantNode) {
        for (Node node = constantNode; node != null; node = node.parent()) {
            if (node instanceof ConstantDeclarationNode constantDeclaration) {
                return constantDeclaration.initializer() instanceof ExpressionNode initializer
                        ? Optional.of(initializer)
                        : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<ExpressionNode> findTracedInitializer(VariableSymbol variable) {
        return tracedInitializers.computeIfAbsent(variable, symbol -> computeTracedInitializer(variable));
    }

    private Optional<ExpressionNode> computeTracedInitializer(VariableSymbol variable) {
        if (!isDeclaredInModule(variable)) {
            return Optional.empty();
        }
        boolean isFinal = variable.qualifiers().contains(Qualifier.FINAL);
        if (!isFinal && variable.qualifiers().contains(Qualifier.PUBLIC)) {
            // Another module can assign a public variable, which the references of this module do not show.
            return Optional.empty();
        }
        Optional<ExpressionNode> initializer = variable.getLocation().flatMap(this::findNode)
                .flatMap(SecretValueResolver::findInitializer);
        return initializer.isPresent() && (isFinal || !isAssigned(variable)) ? initializer : Optional.empty();
    }

    private boolean isDeclaredInModule(Symbol symbol) {
        return symbol.getModule()
                .map(moduleSymbol -> moduleSymbol.id().orgName().equals(module.descriptor().org().value())
                        && moduleSymbol.id().moduleName().equals(module.descriptor().name().toString()))
                .orElse(false);
    }

    // The initializer of a variable declared on its own, not by destructuring, a foreach or a query.
    private static Optional<ExpressionNode> findInitializer(Node variableNode) {
        Node bindingPattern = variableNode.kind() == SyntaxKind.CAPTURE_BINDING_PATTERN
                ? variableNode
                : variableNode.parent();
        if (bindingPattern == null || bindingPattern.kind() != SyntaxKind.CAPTURE_BINDING_PATTERN
                || !(bindingPattern.parent() instanceof TypedBindingPatternNode typedBindingPattern)) {
            return Optional.empty();
        }
        return switch (typedBindingPattern.parent()) {
            case VariableDeclarationNode variableDeclaration -> variableDeclaration.initializer();
            case ModuleVariableDeclarationNode moduleVariableDeclaration -> moduleVariableDeclaration.initializer();
            case null, default -> Optional.empty();
        };
    }

    // Whether the variable, or a member of its value, is the target of an assignment anywhere in the module.
    private boolean isAssigned(VariableSymbol variable) {
        return assignedVariables.computeIfAbsent(variable, symbol -> hasAssignedReference(variable));
    }

    private boolean hasAssignedReference(VariableSymbol variable) {
        for (Location reference : semanticModel.references(variable)) {
            Optional<Node> node = findNode(reference);
            if (node.isEmpty() || isAssignmentTarget(node.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignmentTarget(Node node) {
        for (Node ancestor = node.parent(); ancestor != null; ancestor = ancestor.parent()) {
            if (ancestor instanceof AssignmentStatementNode assignment) {
                return contains(assignment.varRef(), node);
            }
            if (ancestor instanceof CompoundAssignmentStatementNode compoundAssignment) {
                return contains(compoundAssignment.lhsExpression(), node);
            }
            if (ancestor instanceof StatementNode || ancestor instanceof ModuleMemberDeclarationNode) {
                return false;
            }
        }
        return false;
    }

    private static boolean contains(Node container, Node node) {
        TextRange containerRange = container.textRange();
        TextRange range = node.textRange();
        return containerRange.startOffset() <= range.startOffset() && range.endOffset() <= containerRange.endOffset();
    }

    private Optional<Node> findNode(Location location) {
        if (documentsByName == null) {
            documentsByName = new HashMap<>();
            Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream())
                    .map(module::document)
                    .forEach(document -> documentsByName.putIfAbsent(document.name(), document));
        }
        return Optional.ofNullable(documentsByName.get(location.lineRange().fileName()))
                .map(Document::syntaxTree)
                .map(syntaxTree -> ((ModulePartNode) syntaxTree.rootNode()).findNode(location.textRange(), true));
    }

    private static boolean isPlaceholder(ConstantSymbol constant) {
        return constant.resolvedValue()
                .map(value -> value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")
                        ? value.substring(1, value.length() - 1)
                        : value)
                .map(SecretValueFilter::isPlaceholder)
                .orElse(false);
    }

    private static boolean isNumericList(ListConstructorExpressionNode list) {
        return !list.expressions().isEmpty() && list.expressions().stream()
                .allMatch(element -> element.kind() == SyntaxKind.NUMERIC_LITERAL);
    }

    private static boolean isConfigurable(VariableSymbol variable) {
        return variable.qualifiers().contains(Qualifier.CONFIGURABLE);
    }

    private static String stripFieldNameQuotes(String fieldName) {
        return fieldName.length() > 1 && fieldName.startsWith("\"") && fieldName.endsWith("\"")
                ? fieldName.substring(1, fieldName.length() - 1)
                : fieldName;
    }
}
