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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.scan.Issue;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.scan.internal.SuppressionCollector.ANNOTATION_NAME;
import static io.ballerina.scan.internal.SuppressionCollector.SCAN_ORG;
import static io.ballerina.scan.internal.SuppressionCollector.SCAN_PACKAGE;

/**
 * Computes the edits that suppress an issue with a {@code @scan:Suppress} annotation, importing {@code ballerina/scan}
 * when the file does not. The first fix targets the innermost construct: a declaration that can carry the
 * annotation, or a statement that cannot and is restructured into a declaration first. The second, when there is
 * one, annotates the enclosing declaration, such as the function, which also covers similar findings around it.
 *
 * @since 0.13.0
 */
final class SuppressionFixes {
    static final String REASON_TABSTOP = "$1";

    // Restructuring changes what these rules see: http:4 only fires on a directly returned redirect, and a rewritten
    // self-assignment is no longer one, so the rewritten statement would carry an annotation that matches nothing.
    private static final Set<String> NOT_RESTRUCTURED = Set.of("ballerina/http:4", "ballerina:10");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final Project project;
    private final Map<String, Document> documents = new HashMap<>();

    SuppressionFixes(Project project) {
        this.project = project;
        project.currentPackage().modules().forEach(module -> module.documentIds().forEach(documentId ->
                project.documentPath(documentId).ifPresent(path ->
                        documents.put(path.toAbsolutePath().normalize().toString(), module.document(documentId)))));
    }

    /**
     * Edits that suppress an issue.
     *
     * @param construct what the annotation is placed on
     * @param title     title of the action that applies the fix
     * @param edits     non-overlapping edits against the scanned text of the file
     */
    record Fix(String construct, String title, List<Edit> edits) {
    }

    /**
     * A text edit. A snippet edit marks where the reason goes with {@link #REASON_TABSTOP}; its text, including the
     * indentation of each line, is meant to be inserted as is.
     *
     * @param range   range to replace
     * @param newText replacement text
     * @param snippet whether the new text is in the LSP snippet syntax
     */
    record Edit(LineRange range, String newText, boolean snippet) {
    }

    private record Target(int offset, String construct) {
    }

    private record Context(Document document, ModulePartNode modulePart, TextDocument text, String ruleId,
                           String annotation) {
    }

    List<Fix> find(Issue issue, String filePath) {
        Document document = documents.get(Path.of(filePath).toAbsolutePath().normalize().toString());
        if (document == null || !(document.syntaxTree().rootNode() instanceof ModulePartNode modulePart)) {
            return List.of();
        }
        TextDocument text = document.textDocument();
        LineRange lineRange = issue.location().lineRange();
        int start = text.textPositionFrom(lineRange.startLine());
        int end = text.textPositionFrom(lineRange.endLine());
        TextRange issueRange = TextRange.from(start, Math.max(0, end - start));
        String ruleId = issue.rule().id();
        Context context = new Context(document, modulePart, text, ruleId, annotation(scanImportPrefix(modulePart)
                .orElse(SCAN_PACKAGE), ruleId));
        boolean restructure = !NOT_RESTRUCTURED.contains(ruleId);

        List<Fix> fixes = new ArrayList<>();
        for (Node node = modulePart.findNode(issueRange, true); node != null && fixes.size() < 2;
             node = node.parent()) {
            // A service, class or type annotation does not reach findings inside its members
            if (!fixes.isEmpty() && isMemberContainer(node)) {
                break;
            }
            Optional<Target> target = target(node, text);
            if (target.isPresent()) {
                fixes.add(annotationFix(context, target.get()));
            } else if (fixes.isEmpty() && restructure && isBlockStatement(node)) {
                restructured(context, node, issueRange).ifPresent(fixes::add);
            }
        }
        return fixes;
    }

    private Fix annotationFix(Context context, Target target) {
        String source = context.text().toString();
        String indent = indentBefore(source, target.offset());
        String newText = context.annotation() + (indent != null ? "\n" + indent : " ");
        return fix(context, target.construct(), List.of(snippetAt(context, target.offset(), target.offset(),
                newText)));
    }

    private Optional<Fix> restructured(Context context, Node statement, TextRange issueRange) {
        String source = context.text().toString();
        int start = statement.textRange().startOffset();
        String indent = indentBefore(source, start);
        String lineBreak = indent != null ? "\n" + indent : " ";
        String annotated = context.annotation() + lineBreak;
        return switch (statement) {
            // `f(...);` becomes `var _ = f(...);`; a typed wildcard such as `() _` crashes the compiler today
            case ExpressionStatementNode expressionStatement -> Optional.of(fix(context, "this statement",
                    List.of(snippetAt(context, start, start, annotated + "var _ = "))));
            case ReturnStatementNode returned -> returned.expression().flatMap(expression ->
                    returnType(returned).map(type -> {
                        String name = freshName(context, statement, "result");
                        return fix(context, "the returned value", List.of(
                                snippetAt(context, start, expression.textRange().startOffset(),
                                        annotated + escapeSnippet(type) + " " + name + " = "),
                                plainAt(context, statement.textRange().endOffset(), lineBreak + "return " + name
                                        + ";")));
                    }));
            case IfElseStatementNode ifElse when contains(ifElse.condition(), issueRange) ->
                    Optional.of(extracted(context, statement, ifElse.condition(), "the if condition", "boolean",
                            "condition", annotated, lineBreak));
            case ForEachStatementNode forEach when contains(forEach.actionOrExpressionNode(), issueRange) ->
                    Optional.of(extracted(context, statement, forEach.actionOrExpressionNode(), "the foreach range",
                            "var", "range", annotated, lineBreak));
            case AssignmentStatementNode assignment when contains(assignment.expression(), issueRange) ->
                    selfFieldType(assignment).map(type -> {
                        String name = freshName(context, statement, "value");
                        return fix(context, "the assigned value", List.of(
                                snippetAt(context, start, assignment.expression().textRange().startOffset(),
                                        annotated + escapeSnippet(type) + " " + name + " = "),
                                plainAt(context, statement.textRange().endOffset(), lineBreak
                                        + assignment.varRef().toSourceCode().trim() + " = " + name + ";")));
                    });
            default -> Optional.empty();
        };
    }

    // `if cond {` becomes `boolean condition = cond; if condition {`, and likewise for a foreach range.
    private Fix extracted(Context context, Node statement, Node expression, String construct, String type,
                         String baseName, String annotated, String lineBreak) {
        String name = freshName(context, statement, baseName);
        int start = statement.textRange().startOffset();
        TextRange range = expression.textRange();
        String declaration = annotated + type + " " + name + " = " + escapeSnippet(expression.toSourceCode().trim())
                + ";" + lineBreak;
        return fix(context, construct, List.of(snippetAt(context, start, start, declaration),
                plainAt(context, range.startOffset(), range.endOffset(), name)));
    }

    private Fix fix(Context context, String construct, List<Edit> edits) {
        List<Edit> all = new ArrayList<>();
        if (scanImportPrefix(context.modulePart()).isEmpty()) {
            all.add(importEdit(context.modulePart(), context.text()));
        }
        all.addAll(edits);
        return new Fix(construct, "Suppress " + context.ruleId() + " on " + construct + " with @scan:Suppress", all);
    }

    private static boolean isMemberContainer(Node node) {
        return node instanceof ServiceDeclarationNode || node instanceof ClassDefinitionNode
                || node instanceof TypeDefinitionNode;
    }

    private static boolean isBlockStatement(Node node) {
        Node parent = node.parent();
        return parent != null && (parent.kind() == SyntaxKind.BLOCK_STATEMENT
                || parent.kind() == SyntaxKind.FUNCTION_BODY_BLOCK);
    }

    private static boolean contains(Node node, TextRange range) {
        TextRange nodeRange = node.textRange();
        return nodeRange.startOffset() <= range.startOffset() && range.endOffset() <= nodeRange.endOffset();
    }

    // Only a function's own return type is used; inside a worker or an anonymous function the statement is left as is.
    private static Optional<String> returnType(Node statement) {
        for (Node node = statement.parent(); node != null; node = node.parent()) {
            switch (node.kind()) {
                case EXPLICIT_ANONYMOUS_FUNCTION_EXPRESSION, IMPLICIT_ANONYMOUS_FUNCTION_EXPRESSION,
                     NAMED_WORKER_DECLARATION, OBJECT_CONSTRUCTOR -> {
                    return Optional.empty();
                }
                default -> {
                    if (node instanceof FunctionDefinitionNode function) {
                        return function.functionSignature().returnTypeDesc()
                                .map(returnType -> returnType.type().toSourceCode().trim());
                    }
                }
            }
        }
        return Optional.empty();
    }

    // `self.field = expr;` takes the field's declared type, which keeps a literal typed by its context.
    private static Optional<String> selfFieldType(AssignmentStatementNode assignment) {
        if (!(assignment.varRef() instanceof FieldAccessExpressionNode access)
                || !(access.expression() instanceof SimpleNameReferenceNode self)
                || !"self".equals(self.name().text())
                || !(access.fieldName() instanceof SimpleNameReferenceNode field)) {
            return Optional.empty();
        }
        for (Node node = assignment.parent(); node != null; node = node.parent()) {
            if (node instanceof ClassDefinitionNode cls) {
                for (Node member : cls.members()) {
                    if (member instanceof ObjectFieldNode objectField
                            && objectField.fieldName().text().equals(field.name().text())) {
                        return Optional.of(objectField.typeName().toSourceCode().trim());
                    }
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    // Avoids every identifier in the enclosing function and every symbol visible at the statement, so the new
    // variable shadows nothing and clashes with no declaration later in the function.
    private String freshName(Context context, Node statement, String base) {
        Node scope = statement;
        while (scope.parent() != null && !(scope instanceof FunctionDefinitionNode)) {
            scope = scope.parent();
        }
        Set<String> taken = new HashSet<>();
        Matcher matcher = IDENTIFIER.matcher(scope.toSourceCode());
        while (matcher.find()) {
            taken.add(matcher.group());
        }
        SemanticModel semanticModel = project.currentPackage().getCompilation()
                .getSemanticModel(context.document().module().moduleId());
        for (Symbol symbol : semanticModel.visibleSymbols(context.document(),
                context.text().linePositionFrom(statement.textRange().startOffset()))) {
            symbol.getName().ifPresent(taken::add);
        }
        String name = base;
        for (int suffix = 1; taken.contains(name); suffix++) {
            name = base + suffix;
        }
        return name;
    }

    private static String indentBefore(String source, int offset) {
        int lineStart = source.lastIndexOf('\n', offset - 1) + 1;
        String before = source.substring(lineStart, offset);
        return before.isBlank() ? before : null;
    }

    private static String annotation(String prefix, String ruleId) {
        return "@" + prefix + ":" + ANNOTATION_NAME + " {rules: [\""
                + escapeSnippet(ruleId.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"], reason: \""
                + REASON_TABSTOP + "\"}";
    }

    private static Edit snippetAt(Context context, int start, int end, String newText) {
        return new Edit(lineRange(context.text(), start, end), newText, true);
    }

    private static Edit plainAt(Context context, int offset, String newText) {
        return plainAt(context, offset, offset, newText);
    }

    private static Edit plainAt(Context context, int start, int end, String newText) {
        return new Edit(lineRange(context.text(), start, end), newText, false);
    }

    private static LineRange lineRange(TextDocument text, int start, int end) {
        return LineRange.from(null, text.linePositionFrom(start), text.linePositionFrom(end));
    }

    private static Optional<String> scanImportPrefix(ModulePartNode modulePart) {
        for (ImportDeclarationNode importNode : modulePart.imports()) {
            if (importNode.orgName().map(org -> SCAN_ORG.equals(org.orgName().text())).orElse(false)
                    && importNode.moduleName().size() == 1
                    && SCAN_PACKAGE.equals(importNode.moduleName().get(0).text())) {
                return Optional.of(importNode.prefix().map(prefix -> prefix.prefix().text()).orElse(SCAN_PACKAGE));
            }
        }
        return Optional.empty();
    }

    private static Edit importEdit(ModulePartNode modulePart, TextDocument text) {
        NodeList<ImportDeclarationNode> imports = modulePart.imports();
        if (!imports.isEmpty()) {
            LinePosition end = text.linePositionFrom(imports.get(imports.size() - 1).textRange().endOffset());
            return new Edit(LineRange.from(null, end, end), "\nimport " + SCAN_ORG + "/" + SCAN_PACKAGE + ";",
                    false);
        }
        // Leading comments, such as a license header, stay above the import.
        int offset = modulePart.members().isEmpty() ? text.toString().length()
                : modulePart.members().get(0).textRange().startOffset();
        LinePosition position = text.linePositionFrom(offset);
        return new Edit(LineRange.from(null, position, position), "import " + SCAN_ORG + "/" + SCAN_PACKAGE
                + ";\n\n", false);
    }

    private static String escapeSnippet(String value) {
        return value.replace("\\", "\\\\").replace("$", "\\$").replace("}", "\\}");
    }

    private static Optional<Target> target(Node node, TextDocument text) {
        return switch (node) {
            case FunctionDefinitionNode function -> at(text, function, function.metadata(),
                    switch (function.kind()) {
                        case RESOURCE_ACCESSOR_DEFINITION -> "resource function";
                        case OBJECT_METHOD_DEFINITION -> "method";
                        default -> "function";
                    } + " '" + function.functionName().text() + "'");
            case VariableDeclarationNode variable -> at(text, variable, variable.annotations(), "variable '"
                    + variable.typedBindingPattern().bindingPattern().toSourceCode().trim() + "'");
            case ModuleVariableDeclarationNode variable -> at(text, variable, variable.metadata(), "variable '"
                    + variable.typedBindingPattern().bindingPattern().toSourceCode().trim() + "'");
            case ConstantDeclarationNode constant -> at(text, constant, constant.metadata(),
                    "constant '" + constant.variableName().text() + "'");
            case TypeDefinitionNode type -> at(text, type, type.metadata(),
                    "type '" + type.typeName().text() + "'");
            case ClassDefinitionNode cls -> at(text, cls, cls.metadata(), "class '" + cls.className().text() + "'");
            case ServiceDeclarationNode service -> at(text, service, service.metadata(), "service");
            case ListenerDeclarationNode listener -> at(text, listener, listener.metadata(),
                    "listener '" + listener.variableName().text() + "'");
            case NamedWorkerDeclarationNode worker -> at(text, worker, worker.annotations(),
                    "worker '" + worker.workerName().text() + "'");
            case ObjectFieldNode field -> at(text, field, field.metadata(),
                    "field '" + field.fieldName().text() + "'");
            case RecordFieldNode field -> at(text, field, field.metadata(),
                    "field '" + field.fieldName().text() + "'");
            case RecordFieldWithDefaultValueNode field -> at(text, field, field.metadata(),
                    "field '" + field.fieldName().text() + "'");
            case RequiredParameterNode parameter -> at(text, parameter, parameter.annotations(),
                    parameterLabel(parameter.paramName()));
            case DefaultableParameterNode parameter -> at(text, parameter, parameter.annotations(),
                    parameterLabel(parameter.paramName()));
            case RestParameterNode parameter -> at(text, parameter, parameter.annotations(),
                    parameterLabel(parameter.paramName()));
            default -> Optional.empty();
        };
    }

    private static String parameterLabel(Optional<Token> name) {
        return name.map(token -> "parameter '" + token.text() + "'").orElse("parameter");
    }

    private static Optional<Target> at(TextDocument text, Node construct, Optional<MetadataNode> metadata,
                                       String label) {
        return Optional.of(new Target(firstTokenAfter(text, construct, metadata.map(Node.class::cast)), label));
    }

    private static Optional<Target> at(TextDocument text, Node construct, NodeList<AnnotationNode> annotations,
                                       String label) {
        Optional<Node> last = annotations.isEmpty() ? Optional.empty() : Optional.of(annotations.get(
                annotations.size() - 1));
        return Optional.of(new Target(firstTokenAfter(text, construct, last), label));
    }

    // Comments between the metadata and the construct stay above the inserted annotation.
    private static int firstTokenAfter(TextDocument text, Node construct, Optional<Node> metadata) {
        if (metadata.isEmpty()) {
            return construct.textRange().startOffset();
        }
        String source = text.toString();
        int offset = metadata.get().textRange().endOffset();
        while (offset < source.length()) {
            if (Character.isWhitespace(source.charAt(offset))) {
                offset++;
            } else if (source.startsWith("//", offset)) {
                int lineEnd = source.indexOf('\n', offset);
                offset = lineEnd < 0 ? source.length() : lineEnd;
            } else {
                break;
            }
        }
        return offset;
    }
}
