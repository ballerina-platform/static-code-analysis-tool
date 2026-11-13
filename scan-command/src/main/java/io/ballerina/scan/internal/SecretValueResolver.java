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
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.SingletonTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
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
import io.ballerina.compiler.syntax.tree.MapTypeDescriptorNode;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    private static final Pattern SEPARATORS_ONLY = Pattern.compile("[^A-Za-z0-9]*");
    private static final Pattern NUMERIC_LIST = Pattern.compile("\\[\\s*\\d+(\\s*,\\s*\\d+)*\\s*]");
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
        // A value computed at run time, a parameter, or a value already reported where it is written.
        UNKNOWN
    }

    /**
     * A resolved value.
     *
     * @param origin        where the value comes from
     * @param texts         the texts a fixed value can be a secret in: its placeholder parts are left out, and the
     *                      parts of a concatenation or template are joined into one text
     * @param opaque        whether a fixed value is not text, such as a byte array, which is a secret whatever it holds
     * @param part          the text it adds to a concatenation or template, without its angle-bracket placeholders
     * @param isPlaceholder whether that text is a placeholder
     */
    record Value(Origin origin, List<String> texts, boolean opaque, String part, boolean isPlaceholder) {
        static final Value UNKNOWN = new Value(Origin.UNKNOWN, List.of(), false, "", false);
        static final Value CONFIGURABLE = new Value(Origin.CONFIGURABLE, List.of(), false, "", false);
        static final Value OPAQUE = new Value(Origin.HARD_CODED, List.of(), true, "", false);

        // A text is checked without its "<...>" placeholders, as in "Bearer <token>".
        static Value text(String text) {
            String part = SecretValueFilter.withoutAnglePlaceholders(text);
            boolean isPlaceholder = SecretValueFilter.isPlaceholder(text)
                    || (!part.equals(text) && SEPARATORS_ONLY.matcher(part).matches());
            return new Value(Origin.HARD_CODED, isPlaceholder ? List.of() : List.of(part), false, part,
                    isPlaceholder);
        }

        boolean isFixed() {
            return origin == Origin.HARD_CODED || origin == Origin.FIXED_VARIABLE;
        }

        boolean isReported(Kind kind) {
            return isFixed() && (opaque || texts.stream().anyMatch(text -> isReported(text, kind)));
        }

        // Any fixed text can be a password, but only a text that looks generated can be another secret.
        static boolean isReported(String text, Kind kind) {
            return kind == Kind.PASSWORD || SecretValueFilter.looksGenerated(text);
        }

        Value withOrigin(Origin fixedOrigin) {
            return isFixed() ? new Value(fixedOrigin, texts, opaque, part, isPlaceholder) : this;
        }

        // What reaches past a name that reported it where it is written: nothing, or for a list the other members.
        Value unreportedBy(Kind writtenKind) {
            if (!writtenKind.isSecret() || !isReported(writtenKind)) {
                return this;
            }
            if (opaque || texts.size() <= 1) {
                return UNKNOWN;
            }
            return new Value(origin, texts.stream().filter(text -> !isReported(text, writtenKind)).toList(), false,
                    "", false);
        }
    }

    private final SemanticModel semanticModel;
    private final Module module;
    private final Map<Symbol, Optional<ExpressionNode>> tracedInitializers = new HashMap<>();
    private final Map<ConstantSymbol, Value> constantValues = new HashMap<>();
    private final Map<Symbol, Boolean> assignedVariables = new HashMap<>();
    private final Map<Symbol, Boolean> sharedMappings = new HashMap<>();
    private Map<String, Document> documentsByName;

    SecretValueResolver(SemanticModel semanticModel, Module module) {
        this.semanticModel = semanticModel;
        this.module = module;
    }

    Value resolve(ExpressionNode expression) {
        return resolve(expression, 0, new HashSet<>());
    }

    Value resolveSymbol(Symbol symbol) {
        return resolveSymbol(symbol, 0, new HashSet<>());
    }

    private Value resolve(ExpressionNode expression, int depth, Set<Symbol> visited) {
        if (depth > MAX_DEPTH) {
            return Value.UNKNOWN;
        }
        return switch (expression.kind()) {
            case STRING_LITERAL ->
                    Value.text(SecretValueFilter.literalValue(((BasicLiteralNode) expression).literalToken().text()));
            case TYPE_CAST_EXPRESSION -> resolve(((TypeCastExpressionNode) expression).expression(), depth, visited);
            case BYTE_ARRAY_LITERAL -> Value.OPAQUE;
            case LIST_CONSTRUCTOR -> isNumericList((ListConstructorExpressionNode) expression)
                    ? Value.OPAQUE
                    : members(((ListConstructorExpressionNode) expression).expressions().stream()
                    .map(member -> member instanceof ExpressionNode memberExpression
                            ? resolve(memberExpression, depth, visited)
                            : Value.UNKNOWN));
            case MAPPING_CONSTRUCTOR -> resolveMapping((MappingConstructorExpressionNode) expression, depth, visited);
            case REQUIRED_EXPRESSION -> Value.CONFIGURABLE;
            case BRACED_EXPRESSION -> resolve(((BracedExpressionNode) expression).expression(), depth, visited);
            case SIMPLE_NAME_REFERENCE, QUALIFIED_NAME_REFERENCE -> semanticModel.symbol(expression)
                    .map(symbol -> resolveSymbol(symbol, depth, visited))
                    .orElse(Value.UNKNOWN);
            case FIELD_ACCESS -> resolveField(((FieldAccessExpressionNode) expression).expression(),
                    ((FieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth, visited);
            case OPTIONAL_FIELD_ACCESS -> resolveField(((OptionalFieldAccessExpressionNode) expression).expression(),
                    ((OptionalFieldAccessExpressionNode) expression).fieldName().toSourceCode().trim(), depth,
                    visited);
            case INDEXED_EXPRESSION -> memberKey((IndexedExpressionNode) expression)
                    .map(key -> resolveField(((IndexedExpressionNode) expression).containerExpression(), key,
                            depth, visited))
                    .orElse(Value.UNKNOWN);
            case BINARY_EXPRESSION -> resolveConcatenation((BinaryExpressionNode) expression, depth, visited);
            case STRING_TEMPLATE_EXPRESSION -> resolveTemplate((TemplateExpressionNode) expression, depth, visited);
            case METHOD_CALL -> resolveConversion(expression, ((MethodCallExpressionNode) expression).methodName(),
                    ((MethodCallExpressionNode) expression).arguments().isEmpty()
                            ? Optional.of(((MethodCallExpressionNode) expression).expression())
                            : Optional.empty(), depth, visited);
            case FUNCTION_CALL -> resolveConversion(expression,
                    ((FunctionCallExpressionNode) expression).functionName(),
                    singlePositionalArgument((FunctionCallExpressionNode) expression), depth, visited);
            default -> Value.UNKNOWN;
        };
    }

    private Value resolveSymbol(Symbol symbol, int depth, Set<Symbol> visited) {
        if (symbol instanceof ConstantSymbol constant) {
            return resolveConstant(constant);
        }
        if (symbol.kind() != SymbolKind.VARIABLE || !visited.add(symbol)) {
            return Value.UNKNOWN;
        }
        try {
            VariableSymbol variable = (VariableSymbol) symbol;
            Value value = findTracedInitializer(variable)
                    .map(initializer -> resolve(initializer, depth + 1, visited))
                    .orElse(Value.UNKNOWN);
            if (!value.isFixed()) {
                return isConfigurable(variable) ? Value.CONFIGURABLE : value;
            }
            value = value.unreportedBy(SecretNameMatcher.kind(variable.getName().orElse("")));
            if (!value.isFixed()) {
                return value;
            }
            // A configurable's fixed default is a fallback for the secret, which keeps the origin of the default.
            return isConfigurable(variable) ? value : value.withOrigin(Origin.FIXED_VARIABLE);
        } finally {
            visited.remove(symbol);
        }
    }

    // A constant is a hard-coded value wherever it is used; its declaration is not a secret on its own. Its value
    // does not depend on where it is used, and constants cannot refer to each other in a cycle.
    private Value resolveConstant(ConstantSymbol constant) {
        Value value = constantValues.get(constant);
        if (value == null) {
            value = computeConstant(constant);
            constantValues.put(constant, value);
        }
        return value;
    }

    private Value computeConstant(ConstantSymbol constant) {
        if (isNamedChoice(constant)) {
            return Value.UNKNOWN;
        }
        boolean isDeclaredInModule = isDeclaredInModule(constant);
        // The initializer of a constant of this module is resolved as written, as for a list of texts
        Value value = isDeclaredInModule
                ? constant.getLocation().flatMap(this::findNode)
                        .flatMap(SecretValueResolver::findConstantInitializer)
                        .map(initializer -> resolve(initializer, 1, new HashSet<>()))
                        .orElse(Value.UNKNOWN)
                : Value.UNKNOWN;
        if (value.isFixed()) {
            return value.withOrigin(Origin.HARD_CODED);
        }
        TypeSymbol constantType = SemanticUtils.unwrap(constant.typeDescriptor());
        boolean isStructured = constantType instanceof ArrayTypeSymbol || constantType instanceof TupleTypeSymbol
                || constantType instanceof MapTypeSymbol || constantType instanceof RecordTypeSymbol;
        Optional<String> text = constant.resolvedValue().map(SecretValueFilter::literalValue);
        if (!isStructured) {
            return text.map(Value::text).orElse(Value.UNKNOWN);
        }
        if (text.filter(resolved -> NUMERIC_LIST.matcher(resolved).matches()).isPresent()) {
            return Value.OPAQUE;
        }
        // An imported list or map constant is typed by the shape of its value, which holds each of its texts
        return isDeclaredInModule ? Value.UNKNOWN : resolveShape(constantType);
    }

    // The fields of an imported mapping are checked by their names where they are written, as in this module.
    private static Value resolveShape(TypeSymbol shape) {
        if (shape instanceof SingletonTypeSymbol singleton) {
            return singleton.originalType().typeKind() == TypeDescKind.STRING
                    ? Value.text(SecretValueFilter.literalValue(singleton.signature()))
                    : Value.UNKNOWN;
        }
        return switch (SemanticUtils.unwrap(shape)) {
            case TupleTypeSymbol tuple -> members(tuple.memberTypeDescriptors().stream()
                    .map(SecretValueResolver::resolveShape));
            case RecordTypeSymbol record -> members(record.fieldDescriptors().entrySet().stream()
                    .map(field -> resolveShape(field.getValue().typeDescriptor())
                            .unreportedBy(SecretNameMatcher.kind(field.getKey()))));
            default -> Value.UNKNOWN;
        };
    }

    // An imported constant whose shape holds only texts, which a map of secrets can be.
    boolean isImportedMapConstant(ExpressionNode expression) {
        return semanticModel.symbol(expression)
                .filter(symbol -> symbol instanceof ConstantSymbol && !isDeclaredInModule(symbol))
                .map(constant -> SemanticUtils.unwrap(((ConstantSymbol) constant).typeDescriptor()))
                .filter(RecordTypeSymbol.class::isInstance)
                .map(shape -> resolveShape(shape).isFixed())
                .orElse(false);
    }

    private Value resolveField(ExpressionNode container, String fieldName, int depth, Set<Symbol> visited) {
        Optional<Symbol> root = semanticModel.symbol(rootExpression(container));
        boolean isConfigurableRoot = root.filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .map(symbol -> isConfigurable((VariableSymbol) symbol))
                .orElse(false);
        Optional<SpecificFieldNode> field = findMapping(container, depth, visited, true)
                .flatMap(mapping -> findField(mapping, fieldName));
        if (field.isEmpty()) {
            return isConfigurableRoot ? Value.CONFIGURABLE : Value.UNKNOWN;
        }
        Value value = resolveFieldValue(field.get(), depth, visited);
        if (!value.isFixed()) {
            return isConfigurableRoot ? Value.CONFIGURABLE : value;
        }
        boolean isConstantRoot = root.filter(symbol -> symbol.kind() == SymbolKind.CONSTANT).isPresent();
        return isConstantRoot ? value.withOrigin(Origin.HARD_CODED)
                : isConfigurableRoot ? value : value.withOrigin(Origin.FIXED_VARIABLE);
    }

    // A map is its values, as a list is its members; a record's fields are checked by their own names.
    private Value resolveMapping(MappingConstructorExpressionNode mapping, int depth, Set<Symbol> visited) {
        if (!isMap(mapping)) {
            return Value.UNKNOWN;
        }
        return members(mapping.fields().stream()
                .map(field -> field instanceof SpecificFieldNode specificField
                        ? resolveFieldValue(specificField, depth, visited)
                        : Value.UNKNOWN));
    }

    // A mapping in a constant is typed by the shape of its value, so the constant's declared type tells a map.
    private boolean isMap(MappingConstructorExpressionNode mapping) {
        int depth = 0;
        Node outermost = mapping;
        while (outermost.parent() instanceof SpecificFieldNode field
                && field.parent() instanceof MappingConstructorExpressionNode outer) {
            depth++;
            outermost = outer;
        }
        if (outermost.parent() instanceof ConstantDeclarationNode constantDeclaration) {
            int levels = depth;
            return constantDeclaration.typeDescriptor().map(type -> isMapType(type, levels)).orElse(false);
        }
        return semanticModel.typeOf(mapping).map(SecretChecker::isMap).orElse(false);
    }

    // A map constant is typed by the shape of its value, a record, so only its declaration tells it is a map.
    boolean isMapConstant(ExpressionNode expression) {
        return semanticModel.symbol(expression)
                .filter(symbol -> symbol instanceof ConstantSymbol && isDeclaredInModule(symbol))
                .flatMap(Symbol::getLocation)
                .flatMap(this::findNode)
                .flatMap(SecretValueResolver::findConstantDeclaration)
                .flatMap(ConstantDeclarationNode::typeDescriptor)
                .map(type -> isMapType(type, 0))
                .orElse(false);
    }

    // Whether a type is a map, and so are its value types for the given number of levels, as in map<map<string>>.
    private boolean isMapType(Node typeDescriptor, int levels) {
        if (typeDescriptor instanceof MapTypeDescriptorNode mapType) {
            return levels == 0 || isMapType(mapType.mapTypeParamsNode().typeNode(), levels - 1);
        }
        return semanticModel.symbol(typeDescriptor)
                .filter(TypeSymbol.class::isInstance)
                .map(type -> isMapType((TypeSymbol) type, levels))
                .orElse(false);
    }

    private static boolean isMapType(TypeSymbol type, int levels) {
        return SemanticUtils.unwrap(type) instanceof MapTypeSymbol mapType
                && (levels == 0 || isMapType(mapType.typeParam(), levels - 1));
    }

    // A field of a map of secrets is checked where it is written with the stricter of its name and the maps'.
    private Value resolveFieldValue(SpecificFieldNode field, int depth, Set<Symbol> visited) {
        Value value = field.valueExpr()
                .map(fieldValue -> resolve(fieldValue, depth + 1, visited))
                .orElseGet(() -> semanticModel.symbol(field)
                        .map(symbol -> resolveSymbol(symbol, depth + 1, visited))
                        .orElse(Value.UNKNOWN));
        if (!value.isFixed()) {
            return value;
        }
        Kind writtenKind = SecretNameMatcher.kind(SecretChecker.fieldNameText(field))
                .stricter(mapKind((MappingConstructorExpressionNode) field.parent()));
        return value.unreportedBy(writtenKind);
    }

    // The kind a map of secrets is checked with where it is written: the stricter of the names of the maps it is
    // in, as in passwords = {tokens: {github: "..."}}, as SecretChecker checks a map's values.
    private Kind mapKind(MappingConstructorExpressionNode mapping) {
        if (!semanticModel.typeOf(mapping).map(SecretChecker::isMap).orElse(true)) {
            return Kind.NONE;
        }
        Node parent = mapping.parent();
        while (parent instanceof BracedExpressionNode || parent instanceof TypeCastExpressionNode) {
            parent = parent.parent();
        }
        return switch (parent) {
            case VariableDeclarationNode variable ->
                    SecretNameMatcher.kind(SecretChecker.variableName(variable.typedBindingPattern()));
            case ModuleVariableDeclarationNode variable ->
                    SecretNameMatcher.kind(SecretChecker.variableName(variable.typedBindingPattern()));
            case SpecificFieldNode field when field.parent() instanceof MappingConstructorExpressionNode outer ->
                    SecretNameMatcher.kind(SecretChecker.fieldNameText(field)).stricter(mapKind(outer));
            case null, default -> Kind.NONE;
        };
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

    private Value resolveConcatenation(BinaryExpressionNode binaryExpression, int depth, Set<Symbol> visited) {
        if (binaryExpression.operator().kind() != SyntaxKind.PLUS_TOKEN) {
            return Value.UNKNOWN;
        }
        return joined(Stream.of(binaryExpression.lhsExpr(), binaryExpression.rhsExpr())
                .map(operand -> operand instanceof ExpressionNode expression
                        ? resolve(expression, depth, visited)
                        : Value.UNKNOWN));
    }

    private Value resolveTemplate(TemplateExpressionNode template, int depth, Set<Symbol> visited) {
        return joined(template.content().stream()
                .map(part -> part instanceof InterpolationNode interpolation
                        ? resolve(interpolation.expression(), depth, visited)
                        : Value.text(part.toSourceCode())));
    }

    // The parts of a concatenation or template form one text, checked as a whole, as each part can look like text
    // that is not a secret, as in "sk_live_" + KEY.
    private static Value joined(Stream<Value> parts) {
        List<Value> values = parts.toList();
        Origin origin = combinedOrigin(values);
        if (origin == Origin.UNKNOWN) {
            return Value.UNKNOWN;
        }
        String text = values.stream().map(Value::part).collect(Collectors.joining());
        // A part of one character, such as ":", is a separator, which does not make the text a placeholder
        List<Value> words = values.stream().filter(value -> value.part().length() > 1).toList();
        boolean isPlaceholder = SecretValueFilter.isPlaceholder(text)
                || (!words.isEmpty() && words.stream().allMatch(Value::isPlaceholder));
        return new Value(origin, isPlaceholder || text.isEmpty() ? List.of() : List.of(text),
                values.stream().anyMatch(Value::opaque), text, isPlaceholder);
    }

    // The members of a list are separate values, any of which can be the secret.
    private static Value members(Stream<Value> members) {
        List<Value> values = members.toList();
        Origin origin = combinedOrigin(values);
        if (origin == Origin.UNKNOWN) {
            return Value.UNKNOWN;
        }
        return new Value(origin, values.stream().flatMap(value -> value.texts().stream()).toList(),
                values.stream().anyMatch(Value::opaque), "", false);
    }

    // A value built from fixed parts is fixed, and hard-coded only when no part is reached through a variable.
    private static Origin combinedOrigin(List<Value> values) {
        if (!values.stream().allMatch(Value::isFixed)) {
            return Origin.UNKNOWN;
        }
        return values.stream().anyMatch(value -> value.origin() == Origin.FIXED_VARIABLE)
                ? Origin.FIXED_VARIABLE : Origin.HARD_CODED;
    }

    // A value converted by a lang library function, such as "secret".toBytes() or string:trim(secret), keeps the
    // origin of the value.
    private Value resolveConversion(ExpressionNode call, Node functionName, Optional<ExpressionNode> value,
                                    int depth, Set<Symbol> visited) {
        String name = functionName instanceof QualifiedNameReferenceNode qualifiedName
                ? qualifiedName.identifier().text()
                : functionName.toSourceCode().trim();
        boolean isLangLibConversion = value.isPresent() && TRANSPARENT_CONVERSIONS.contains(name)
                && semanticModel.symbol(call).filter(SemanticUtils::isLangLib).isPresent();
        if (!isLangLibConversion) {
            return Value.UNKNOWN;
        }
        Value converted = resolve(value.get(), depth, visited);
        return converted.isFixed() ? converted : Value.UNKNOWN;
    }

    private static Optional<ExpressionNode> singlePositionalArgument(FunctionCallExpressionNode functionCall) {
        return functionCall.arguments().size() == 1
                && functionCall.arguments().get(0) instanceof PositionalArgumentNode argument
                ? Optional.of(argument.expression())
                : Optional.empty();
    }

    private static Optional<ExpressionNode> findConstantInitializer(Node constantNode) {
        return findConstantDeclaration(constantNode)
                .filter(constantDeclaration -> constantDeclaration.initializer() instanceof ExpressionNode)
                .map(constantDeclaration -> (ExpressionNode) constantDeclaration.initializer());
    }

    private static Optional<ConstantDeclarationNode> findConstantDeclaration(Node constantNode) {
        for (Node node = constantNode; node != null; node = node.parent()) {
            if (node instanceof ConstantDeclarationNode constantDeclaration) {
                return Optional.of(constantDeclaration);
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
