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
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
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
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.OptionalFieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RestArgumentNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.scan.internal.SecretNameMatcher.Kind;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.TextRange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
        // A fixed value that cannot be the secret on its own but can be part of one: an empty, template or dummy
        // value, or for a secret other than a password, a value that does not look generated.
        PLACEHOLDER,
        // A value computed at run time, a parameter, or a value already reported where it is written.
        UNKNOWN
    }

    private final SemanticModel semanticModel;
    private final Module module;
    private final Map<Symbol, Optional<ExpressionNode>> tracedInitializers = new HashMap<>();
    private final Map<Symbol, Boolean> assignedVariables = new HashMap<>();
    private final Map<Symbol, Boolean> sharedMappings = new HashMap<>();
    private Map<String, Document> documentsByName;

    SecretValueResolver(SemanticModel semanticModel, Module module) {
        this.semanticModel = semanticModel;
        this.module = module;
    }

    /**
     * Resolves the value given to a target whose name suggests the given kind of secret. Any fixed text can be a
     * password, but only a text that looks generated can be another secret, such as a token or a key.
     */
    Origin resolve(ExpressionNode expression, Kind kind) {
        return resolve(expression, kind, 0, new HashSet<>());
    }

    Origin resolveSymbol(Symbol symbol, Kind kind) {
        return resolveSymbol(symbol, kind, 0, new HashSet<>());
    }

    // Whether a fixed text cannot be the secret on its own, which depends on the kind of the secret.
    private static boolean isNotSecret(String text, Kind kind) {
        return SecretValueFilter.isPlaceholder(text) || isNotGenerated(text, kind);
    }

    // A joined text is a placeholder only when all its parts are, as a template part such as "${ENV}" does not make
    // a real password next to it one.
    private static boolean isNotGenerated(String text, Kind kind) {
        return kind == Kind.SECRET && !SecretValueFilter.looksGenerated(text);
    }

    private Origin resolve(ExpressionNode expression, Kind kind, int depth, Set<Symbol> visited) {
        if (depth > MAX_DEPTH) {
            return Origin.UNKNOWN;
        }
        return switch (expression.kind()) {
            case STRING_LITERAL -> isNotSecret(
                    SecretValueFilter.literalValue(((BasicLiteralNode) expression).literalToken().text()), kind)
                    ? Origin.PLACEHOLDER
                    : Origin.HARD_CODED;
            case TYPE_CAST_EXPRESSION -> resolve(((TypeCastExpressionNode) expression).expression(), kind, depth,
                    visited);
            case BYTE_ARRAY_LITERAL -> Origin.HARD_CODED;
            case LIST_CONSTRUCTOR -> isNumericList((ListConstructorExpressionNode) expression)
                    ? Origin.HARD_CODED
                    : combine(((ListConstructorExpressionNode) expression).expressions().stream()
                    .map(member -> member instanceof ExpressionNode memberExpression
                            ? resolve(memberExpression, kind, depth, visited)
                            : Origin.UNKNOWN));
            case REQUIRED_EXPRESSION -> Origin.CONFIGURABLE;
            case BRACED_EXPRESSION -> resolve(((BracedExpressionNode) expression).expression(), kind, depth, visited);
            case SIMPLE_NAME_REFERENCE, QUALIFIED_NAME_REFERENCE -> semanticModel.symbol(expression)
                    .map(symbol -> resolveSymbol(symbol, kind, depth, visited))
                    .orElse(Origin.UNKNOWN);
            case FIELD_ACCESS -> resolveField(((FieldAccessExpressionNode) expression).expression(),
                    ((FieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), kind, depth, visited);
            case OPTIONAL_FIELD_ACCESS -> resolveField(((OptionalFieldAccessExpressionNode) expression).expression(),
                    ((OptionalFieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), kind, depth,
                    visited);
            case INDEXED_EXPRESSION -> memberKey((IndexedExpressionNode) expression)
                    .map(key -> resolveField(((IndexedExpressionNode) expression).containerExpression(), key, kind,
                            depth, visited))
                    .orElse(Origin.UNKNOWN);
            case BINARY_EXPRESSION, STRING_TEMPLATE_EXPRESSION -> fixedText(expression, depth, visited)
                    .map(text -> text.isPlaceholder() || isNotGenerated(text.text(), kind) ? Origin.PLACEHOLDER
                            : text.isThroughVariable() ? Origin.FIXED_VARIABLE : Origin.HARD_CODED)
                    .orElseGet(() -> expression instanceof TemplateExpressionNode template
                            ? resolveTemplate(template, kind, depth, visited)
                            : resolveConcatenation((BinaryExpressionNode) expression, kind, depth, visited));
            case METHOD_CALL -> resolveConversion(expression, ((MethodCallExpressionNode) expression).methodName(),
                    ((MethodCallExpressionNode) expression).arguments().isEmpty()
                            ? Optional.of(((MethodCallExpressionNode) expression).expression())
                            : Optional.empty(), kind, depth, visited);
            case FUNCTION_CALL -> resolveConversion(expression,
                    ((FunctionCallExpressionNode) expression).functionName(),
                    singlePositionalArgument((FunctionCallExpressionNode) expression), kind, depth, visited);
            default -> Origin.UNKNOWN;
        };
    }

    private Origin resolveSymbol(Symbol symbol, Kind kind, int depth, Set<Symbol> visited) {
        if (symbol instanceof ConstantSymbol constant) {
            return resolveConstant(constant, kind, depth, visited);
        }
        if (symbol.kind() != SymbolKind.VARIABLE || !visited.add(symbol)) {
            return Origin.UNKNOWN;
        }
        try {
            VariableSymbol variable = (VariableSymbol) symbol;
            Optional<ExpressionNode> initializer = findTracedInitializer(variable);
            Origin initializerOrigin = initializer
                    .map(value -> resolve(value, kind, depth + 1, visited))
                    .orElse(Origin.UNKNOWN);
            if (!isFixed(initializerOrigin)) {
                return isConfigurable(variable) ? Origin.CONFIGURABLE : initializerOrigin;
            }
            if (isReportedWhereWritten(variable.getName().orElse(""), kind,
                    writtenKind -> resolve(initializer.get(), writtenKind, depth + 1, visited))) {
                return Origin.UNKNOWN;
            }
            // A configurable's fixed default is a fallback for the secret, which keeps the origin of the default.
            return isConfigurable(variable) ? initializerOrigin : Origin.FIXED_VARIABLE;
        } finally {
            visited.remove(symbol);
        }
    }

    // A constant is a hard-coded value wherever it is used; its declaration is not a secret on its own.
    private Origin resolveConstant(ConstantSymbol constant, Kind kind, int depth, Set<Symbol> visited) {
        if (isNamedChoice(constant)) {
            return Origin.UNKNOWN;
        }
        // The initializer of a constant of this module is resolved as written, as for a list of texts
        Origin initializerOrigin = isDeclaredInModule(constant)
                ? constant.getLocation().flatMap(this::findNode)
                        .flatMap(SecretValueResolver::findConstantInitializer)
                        .map(initializer -> resolve(initializer, kind, depth + 1, visited))
                        .orElse(Origin.UNKNOWN)
                : Origin.UNKNOWN;
        if (initializerOrigin == Origin.PLACEHOLDER) {
            return Origin.PLACEHOLDER;
        }
        if (isFixed(initializerOrigin)) {
            return Origin.HARD_CODED;
        }
        return isNotSecretConstant(constant, kind) ? Origin.PLACEHOLDER : Origin.HARD_CODED;
    }

    private static boolean isFixed(Origin origin) {
        return origin == Origin.HARD_CODED || origin == Origin.FIXED_VARIABLE;
    }

    // Whether a fixed value reaching the target through a secret-named variable or field was reported where it is
    // written: always for a password or the target's own kind, otherwise only if it passes the check of the name.
    private static boolean isReportedWhereWritten(String name, Kind kind, Function<Kind, Origin> resolveAsWritten) {
        Kind writtenKind = SecretNameMatcher.kind(name);
        if (!writtenKind.isSecret()) {
            return false;
        }
        return writtenKind == kind || writtenKind == Kind.PASSWORD || isFixed(resolveAsWritten.apply(writtenKind));
    }

    private Origin resolveField(ExpressionNode container, String fieldName, Kind kind, int depth,
                                Set<Symbol> visited) {
        Optional<Symbol> root = semanticModel.symbol(rootExpression(container));
        boolean isConfigurableRoot = root.filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .map(symbol -> isConfigurable((VariableSymbol) symbol))
                .orElse(false);
        Optional<SpecificFieldNode> field = findMapping(container, depth, visited, true)
                .flatMap(mapping -> findField(mapping, fieldName));
        if (field.isEmpty()) {
            return isConfigurableRoot ? Origin.CONFIGURABLE : Origin.UNKNOWN;
        }
        Function<Kind, Origin> resolveValue = valueKind -> field.get().valueExpr()
                .map(value -> resolve(value, valueKind, depth + 1, visited))
                .orElseGet(() -> semanticModel.symbol(field.get())
                        .map(symbol -> resolveSymbol(symbol, valueKind, depth + 1, visited))
                        .orElse(Origin.UNKNOWN));
        Origin origin = resolveValue.apply(kind);
        if (!isFixed(origin)) {
            return isConfigurableRoot ? Origin.CONFIGURABLE : origin;
        }
        // A secret-named field, or a field of a map of secrets, is reported where it is written. Another fixed field
        // keeps its origin in a constant or a configurable's default, and is otherwise a fixed variable.
        String writtenName = SecretNameMatcher.kind(fieldName).isSecret()
                ? fieldName
                : mapName((MappingConstructorExpressionNode) field.get().parent()).orElse(fieldName);
        if (isReportedWhereWritten(writtenName, kind, resolveValue)) {
            return Origin.UNKNOWN;
        }
        boolean isConstantRoot = root.filter(symbol -> symbol.kind() == SymbolKind.CONSTANT).isPresent();
        return isConstantRoot || isConfigurableRoot ? origin : Origin.FIXED_VARIABLE;
    }

    // The name a map is written to, whose kind its values are checked with, as for passwords in passwords = {...}.
    // A map inside a map of secrets is checked with the outer name, as in tokens = {github: {dev: "..."}}.
    private Optional<String> mapName(MappingConstructorExpressionNode mapping) {
        if (!semanticModel.typeOf(mapping).map(SecretChecker::isMap).orElse(true)) {
            return Optional.empty();
        }
        Optional<String> name = switch (mapping.parent()) {
            case VariableDeclarationNode variable -> Optional.of(SecretChecker.variableName(
                    variable.typedBindingPattern()));
            case ModuleVariableDeclarationNode variable -> Optional.of(SecretChecker.variableName(
                    variable.typedBindingPattern()));
            case SpecificFieldNode field -> Optional.of(SecretChecker.fieldNameText(field));
            case null, default -> Optional.empty();
        };
        if (name.map(SecretNameMatcher::kind).filter(Kind::isSecret).isEmpty()
                && mapping.parent() instanceof SpecificFieldNode field
                && field.parent() instanceof MappingConstructorExpressionNode outer) {
            Optional<String> outerName = mapName(outer);
            if (outerName.map(SecretNameMatcher::kind).filter(Kind::isSecret).isPresent()) {
                return outerName;
            }
        }
        return name;
    }

    // The variable or constant that a field access or member access chain starts at, as in DEFAULTS["db"].
    private static Node rootExpression(Node expression) {
        Node root = expression;
        while (true) {
            switch (root) {
                case FieldAccessExpressionNode fieldAccess -> root = fieldAccess.expression();
                case OptionalFieldAccessExpressionNode optionalFieldAccess -> root = optionalFieldAccess.expression();
                case IndexedExpressionNode memberAccess -> root = memberAccess.containerExpression();
                case BracedExpressionNode braced -> root = braced.expression();
                default -> {
                    return root;
                }
            }
        }
    }

    // A configurable's default is followed only from the start of an access chain, as in cfg.inner.value, and not
    // through a variable or field that holds the configurable's value.
    private Optional<MappingConstructorExpressionNode> findMapping(ExpressionNode expression, int depth,
                                                                   Set<Symbol> visited, boolean followConfigurable) {
        if (depth > MAX_DEPTH) {
            return Optional.empty();
        }
        return switch (expression.kind()) {
            case MAPPING_CONSTRUCTOR -> Optional.of((MappingConstructorExpressionNode) expression);
            case BRACED_EXPRESSION -> findMapping(((BracedExpressionNode) expression).expression(), depth, visited,
                    followConfigurable);
            case SIMPLE_NAME_REFERENCE -> semanticModel.symbol(expression)
                    .flatMap(symbol -> findMappingInitializer(symbol, followConfigurable))
                    .flatMap(initializer -> findMapping(initializer, depth + 1, visited, false));
            case FIELD_ACCESS -> findFieldMapping(((FieldAccessExpressionNode) expression).expression(),
                    ((FieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth, visited,
                    followConfigurable);
            case OPTIONAL_FIELD_ACCESS -> findFieldMapping(
                    ((OptionalFieldAccessExpressionNode) expression).expression(),
                    ((OptionalFieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth,
                    visited, followConfigurable);
            case INDEXED_EXPRESSION -> memberKey((IndexedExpressionNode) expression)
                    .flatMap(key -> findFieldMapping(((IndexedExpressionNode) expression).containerExpression(), key,
                            depth, visited, followConfigurable));
            default -> Optional.empty();
        };
    }

    private Optional<MappingConstructorExpressionNode> findFieldMapping(ExpressionNode container, String fieldName,
                                                                        int depth, Set<Symbol> visited,
                                                                        boolean followConfigurable) {
        return findMapping(container, depth + 1, visited, followConfigurable)
                .flatMap(mapping -> findField(mapping, fieldName))
                .flatMap(field -> field.valueExpr().or(() -> semanticModel.symbol(field)
                        .flatMap(symbol -> findMappingInitializer(symbol, false))))
                .flatMap(value -> findMapping(value, depth + 1, visited, false));
    }

    // A mapping is followed only while its fields cannot change: it is not public, it is not assigned, and it is not
    // passed on or changed through a method.
    private Optional<ExpressionNode> findMappingInitializer(Symbol symbol, boolean followConfigurable) {
        if (symbol.kind() == SymbolKind.CONSTANT) {
            return symbol.getLocation().flatMap(this::findNode).flatMap(SecretValueResolver::findConstantInitializer);
        }
        if (symbol.kind() != SymbolKind.VARIABLE) {
            return Optional.empty();
        }
        VariableSymbol variable = (VariableSymbol) symbol;
        if (isConfigurable(variable)) {
            return followConfigurable
                    ? variable.getLocation().flatMap(this::findNode).flatMap(SecretValueResolver::findInitializer)
                    : Optional.empty();
        }
        if (variable.qualifiers().contains(Qualifier.PUBLIC) || isAssigned(variable) || isShared(variable)) {
            return Optional.empty();
        }
        return findTracedInitializer(variable);
    }

    // Whether a mutable mapping is passed on or has a method called on it, as in load(cfg) or cfg.put("db", value),
    // either of which can change its fields.
    private boolean isShared(VariableSymbol variable) {
        if (variable.typeDescriptor().subtypeOf(semanticModel.types().READONLY)) {
            return false;
        }
        return sharedMappings.computeIfAbsent(variable, symbol -> hasSharedReference(variable));
    }

    private boolean hasSharedReference(VariableSymbol variable) {
        for (Location reference : semanticModel.references(variable)) {
            Optional<Node> node = findNode(reference).map(found -> found instanceof Token ? found.parent() : found);
            if (node.isEmpty() || isPassedOn(node.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPassedOn(Node reference) {
        return switch (reference.parent()) {
            case MethodCallExpressionNode methodCall -> methodCall.expression() == reference;
            case PositionalArgumentNode ignored -> true;
            case NamedArgumentNode ignored -> true;
            case RestArgumentNode ignored -> true;
            case null, default -> false;
        };
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
                    && SecretValueFilter.literalValue(SecretChecker.fieldNameText(specificField)).equals(fieldName)) {
                return Optional.of(specificField);
            }
        }
        return Optional.empty();
    }

    private Origin resolveConcatenation(BinaryExpressionNode binaryExpression, Kind kind, int depth,
                                        Set<Symbol> visited) {
        if (binaryExpression.operator().kind() != SyntaxKind.PLUS_TOKEN) {
            return Origin.UNKNOWN;
        }
        return combine(Stream.of(binaryExpression.lhsExpr(), binaryExpression.rhsExpr())
                .map(operand -> operand instanceof ExpressionNode expression
                        ? resolve(expression, kind, depth, visited)
                        : Origin.UNKNOWN));
    }

    private Origin resolveTemplate(TemplateExpressionNode template, Kind kind, int depth, Set<Symbol> visited) {
        String text = template.content().stream()
                .filter(node -> node.kind() == SyntaxKind.TEMPLATE_STRING)
                .map(Node::toSourceCode)
                .reduce("", String::concat);
        List<Origin> interpolations = template.content().stream()
                .filter(InterpolationNode.class::isInstance)
                .map(interpolation -> resolve(((InterpolationNode) interpolation).expression(), kind, depth, visited))
                .toList();
        Origin textOrigin = isNotSecret(text, kind) ? Origin.PLACEHOLDER : Origin.HARD_CODED;
        return combine(Stream.concat(Stream.of(textOrigin), interpolations.stream()));
    }

    /**
     * A fixed text.
     *
     * @param text              the text of its parts that are not placeholders
     * @param isThroughVariable whether a part is reached through a variable, which makes it a fixed variable
     * @param isPlaceholder     whether every part is a placeholder, as when two templates are joined
     */
    private record FixedText(String text, boolean isThroughVariable, boolean isPlaceholder) {
        static FixedText of(String text) {
            boolean isPlaceholder = SecretValueFilter.isPlaceholder(text);
            return new FixedText(isPlaceholder ? "" : text, false, isPlaceholder);
        }

        FixedText throughVariable() {
            return new FixedText(text, true, isPlaceholder);
        }

        FixedText concat(FixedText other) {
            return new FixedText(text + other.text, isThroughVariable || other.isThroughVariable,
                    isPlaceholder && other.isPlaceholder);
        }
    }

    // The text of a value built only from literals, constants, and variables or fields holding one, as in
    // "sk_live_" + KEY, which is checked as a whole, as its parts can each look like text that is not a secret.
    private Optional<FixedText> fixedText(ExpressionNode expression, int depth, Set<Symbol> visited) {
        if (depth > MAX_DEPTH) {
            return Optional.empty();
        }
        return switch (expression) {
            case BasicLiteralNode literal when literal.kind() == SyntaxKind.STRING_LITERAL ->
                    Optional.of(FixedText.of(SecretValueFilter.literalValue(literal.literalToken().text())));
            case BracedExpressionNode braced -> fixedText(braced.expression(), depth, visited);
            case TypeCastExpressionNode cast -> fixedText(cast.expression(), depth, visited);
            case FieldAccessExpressionNode access -> fieldText(access.expression(),
                    access.fieldName().toSourceCode().trim(), depth, visited);
            case IndexedExpressionNode access -> memberKey(access)
                    .flatMap(key -> fieldText(access.containerExpression(), key, depth, visited));
            case BinaryExpressionNode binary when binary.operator().kind() == SyntaxKind.PLUS_TOKEN
                    && binary.lhsExpr() instanceof ExpressionNode lhs
                    && binary.rhsExpr() instanceof ExpressionNode rhs -> fixedText(lhs, depth, visited)
                    .flatMap(lhsText -> fixedText(rhs, depth, visited).map(lhsText::concat));
            case TemplateExpressionNode template when template.kind() == SyntaxKind.STRING_TEMPLATE_EXPRESSION -> {
                FixedText text = new FixedText("", false, true);
                for (Node part : template.content()) {
                    Optional<FixedText> partText = part instanceof InterpolationNode interpolation
                            ? fixedText(interpolation.expression(), depth, visited)
                            : Optional.of(FixedText.of(part.toSourceCode()));
                    if (partText.isEmpty()) {
                        yield Optional.empty();
                    }
                    text = text.concat(partText.get());
                }
                yield Optional.of(text);
            }
            case SimpleNameReferenceNode ignored -> constantText(expression).map(FixedText::of)
                    .or(() -> variableText(expression, depth, visited));
            case QualifiedNameReferenceNode ignored -> constantText(expression).map(FixedText::of);
            default -> Optional.empty();
        };
    }

    // A field of a fixed mapping, unless the field was already reported where it is written or is configurable.
    private Optional<FixedText> fieldText(ExpressionNode container, String fieldName, int depth, Set<Symbol> visited) {
        Optional<Symbol> root = semanticModel.symbol(rootExpression(container));
        if (root.filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .map(symbol -> isConfigurable((VariableSymbol) symbol)).orElse(false)) {
            return Optional.empty();
        }
        Optional<ExpressionNode> value = findMapping(container, depth, visited, true)
                .flatMap(mapping -> findField(mapping, fieldName))
                .flatMap(SpecificFieldNode::valueExpr);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        String writtenName = SecretNameMatcher.kind(fieldName).isSecret()
                ? fieldName
                : mapName((MappingConstructorExpressionNode) value.get().parent().parent()).orElse(fieldName);
        Kind writtenKind = SecretNameMatcher.kind(writtenName);
        if (writtenKind.isSecret() && isFixed(resolve(value.get(), writtenKind, depth + 1, visited))) {
            return Optional.empty();
        }
        boolean isConstantRoot = root.filter(symbol -> symbol.kind() == SymbolKind.CONSTANT).isPresent();
        return fixedText(value.get(), depth + 1, visited)
                .map(text -> isConstantRoot ? text : text.throughVariable());
    }

    // A configurable can be given another value, and a part already reported where it is written is not joined.
    private Optional<FixedText> variableText(ExpressionNode reference, int depth, Set<Symbol> visited) {
        Optional<VariableSymbol> variable = semanticModel.symbol(reference)
                .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .map(VariableSymbol.class::cast)
                .filter(symbol -> !isConfigurable(symbol));
        if (variable.isEmpty() || !visited.add(variable.get())) {
            return Optional.empty();
        }
        try {
            Optional<ExpressionNode> initializer = findTracedInitializer(variable.get());
            Kind writtenKind = SecretNameMatcher.kind(variable.get().getName().orElse(""));
            if (initializer.isEmpty() || writtenKind.isSecret()
                    && isFixed(resolve(initializer.get(), writtenKind, depth + 1, visited))) {
                return Optional.empty();
            }
            return fixedText(initializer.get(), depth + 1, visited).map(FixedText::throughVariable);
        } finally {
            visited.remove(variable.get());
        }
    }

    private Optional<String> constantText(ExpressionNode reference) {
        return semanticModel.symbol(reference)
                .filter(ConstantSymbol.class::isInstance)
                .map(ConstantSymbol.class::cast)
                .filter(constant -> !isNamedChoice(constant) && isString(constant.broaderTypeDescriptor()))
                .flatMap(ConstantSymbol::resolvedValue)
                .map(SecretValueFilter::literalValue);
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
                                     Kind kind, int depth, Set<Symbol> visited) {
        String name = functionName instanceof QualifiedNameReferenceNode qualifiedName
                ? qualifiedName.identifier().text()
                : functionName.toSourceCode().trim();
        boolean isLangLibConversion = value.isPresent() && TRANSPARENT_CONVERSIONS.contains(name)
                && semanticModel.symbol(call).filter(SemanticUtils::isLangLib).isPresent();
        if (!isLangLibConversion) {
            return Origin.UNKNOWN;
        }
        Origin origin = resolve(value.get(), kind, depth, visited);
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
            // Only the variable that the target starts at is assigned, not one used as a key, as in lookup[key].
            if (ancestor instanceof AssignmentStatementNode assignment) {
                return contains(rootExpression(assignment.varRef()), node);
            }
            if (ancestor instanceof CompoundAssignmentStatementNode compoundAssignment) {
                return contains(rootExpression(compoundAssignment.lhsExpression()), node);
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

    private static boolean isNotSecretConstant(ConstantSymbol constant, Kind kind) {
        // Only a string constant is text; a byte array or a list constant resolves to its source form
        boolean isText = isString(constant.broaderTypeDescriptor());
        return constant.resolvedValue()
                .map(SecretValueFilter::literalValue)
                .map(value -> isText ? isNotSecret(value, kind) : SecretValueFilter.isPlaceholder(value))
                .orElse(false);
    }

    // A string type, or a union of string types, as in "live"|"test".
    private static boolean isString(TypeSymbol typeSymbol) {
        TypeSymbol type = SemanticUtils.unwrap(typeSymbol);
        if (type instanceof UnionTypeSymbol union) {
            return union.memberTypeDescriptors().stream().allMatch(SecretValueResolver::isString);
        }
        return type.typeKind() == TypeDescKind.STRING || type.typeKind() == TypeDescKind.STRING_CHAR;
    }

    // An enum member whose value is its own name, such as BASIC in enum AuthScheme { BASIC }, names a choice.
    private static boolean isNamedChoice(ConstantSymbol constant) {
        return constant.kind() == SymbolKind.ENUM_MEMBER && constant.resolvedValue()
                .map(SecretValueFilter::literalValue)
                .equals(constant.getName());
    }

    private static boolean isNumericList(ListConstructorExpressionNode list) {
        return !list.expressions().isEmpty() && list.expressions().stream()
                .allMatch(element -> element.kind() == SyntaxKind.NUMERIC_LITERAL);
    }

    private static boolean isConfigurable(VariableSymbol variable) {
        return variable.qualifiers().contains(Qualifier.CONFIGURABLE);
    }
}
