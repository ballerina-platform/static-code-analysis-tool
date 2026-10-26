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
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.DistinctTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.ballerina.scan.internal.ScanToolConstants.IMPORT_GENERATOR_FILE;

/**
 * Collects the {@code @scan:Suppress} annotations of a package.
 *
 * @since 0.11.1
 */
final class SuppressionCollector {

    private static final String SCAN_ORG = "ballerina";
    private static final String SCAN_PACKAGE = "scan";
    private static final String ANNOTATION_NAME = "Suppress";
    private static final String RULES_FIELD = "rules";
    private static final String REASON_FIELD = "reason";

    private SuppressionCollector() {
    }

    /**
     * Returns the suppressions declared in the given project's current package. A package that does not depend on
     * {@code ballerina/scan} cannot declare any, so its documents are not visited.
     *
     * @param project the project being scanned
     * @return the suppressions in source order of each document
     */
    static List<SuppressionAnnotation> collect(Project project) {
        Package currentPackage = project.currentPackage();
        if (!dependsOnScanPackage(currentPackage)) {
            return List.of();
        }
        List<SuppressionAnnotation> suppressions = new ArrayList<>();
        currentPackage.moduleIds().forEach(moduleId -> {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel = module.getCompilation().getSemanticModel();
            Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream())
                    .forEach(documentId -> collect(project, module, documentId, semanticModel, suppressions));
        });
        return suppressions;
    }

    private static boolean dependsOnScanPackage(Package currentPackage) {
        for (ResolvedPackageDependency dependency : currentPackage.getResolution().allDependencies()) {
            Package dependencyPackage = dependency.packageInstance();
            if (SCAN_ORG.equals(dependencyPackage.packageOrg().value())
                    && SCAN_PACKAGE.equals(dependencyPackage.packageName().value())) {
                return true;
            }
        }
        return false;
    }

    private static void collect(Project project, Module module, DocumentId documentId, SemanticModel semanticModel,
                                List<SuppressionAnnotation> suppressions) {
        Document document = module.document(documentId);
        if (document.name().startsWith(IMPORT_GENERATOR_FILE)
                || !(document.syntaxTree().rootNode() instanceof ModulePartNode modulePart)) {
            return;
        }
        // Derived exactly as the reporter derives an issue's file path, so that the two can be compared
        String filePath = project.documentPath(documentId).orElse(Path.of(document.name())).toString();
        String fileName = module.moduleName() + "/" + document.name();
        modulePart.accept(new NodeVisitor() {
            @Override
            public void visit(AnnotationNode annotation) {
                if (isSuppressAnnotation(semanticModel, annotation)) {
                    suppressions.add(toSuppression(semanticModel, annotation, filePath, fileName));
                }
            }
        });
    }

    private static boolean isSuppressAnnotation(SemanticModel semanticModel, AnnotationNode annotation) {
        if (!(semanticModel.symbol(annotation).orElse(null) instanceof AnnotationSymbol symbol)) {
            return false;
        }
        return symbol.getName().filter(ANNOTATION_NAME::equals).isPresent()
                && symbol.getModule().map(module -> SCAN_ORG.equals(module.id().orgName())
                && SCAN_PACKAGE.equals(module.id().moduleName())).orElse(false);
    }

    private static SuppressionAnnotation toSuppression(SemanticModel semanticModel, AnnotationNode annotation,
                                                       String filePath, String fileName) {
        Node construct = annotation.parent() instanceof MetadataNode ? annotation.parent().parent()
                : annotation.parent();
        List<String> rules = null;
        List<String> unresolvedRules = new ArrayList<>();
        String reason = null;
        if (annotation.annotValue().isPresent()) {
            for (MappingFieldNode field : annotation.annotValue().get().fields()) {
                if (!(field instanceof SpecificFieldNode specific)) {
                    continue;
                }
                String name = fieldName(specific);
                // A shorthand field such as `{rules}` takes the value of the constant its name refers to
                Node value = specific.valueExpr().map(Node.class::cast).orElse(specific.fieldName());
                if (RULES_FIELD.equals(name)) {
                    rules = new ArrayList<>();
                    resolveRules(semanticModel, value, rules, unresolvedRules);
                } else if (REASON_FIELD.equals(name)) {
                    reason = stringValue(semanticModel, value).filter(text -> !text.isBlank()).orElse(null);
                }
            }
        }
        return new SuppressionAnnotation(filePath, fileName, annotation.lineRange(), construct.lineRange(),
                construct.textRange().length(), memberRanges(construct), rules == null,
                rules == null ? List.of() : rules, unresolvedRules, reason);
    }

    private static void resolveRules(SemanticModel semanticModel, Node value, List<String> rules,
                                     List<String> unresolvedRules) {
        if (value instanceof ListConstructorExpressionNode list) {
            list.expressions().forEach(rule -> stringValue(semanticModel, rule)
                    .ifPresentOrElse(rules::add, () -> unresolvedRules.add(rule.toSourceCode().trim())));
            return;
        }
        constantStrings(semanticModel, value)
                .ifPresentOrElse(rules::addAll, () -> unresolvedRules.add(value.toSourceCode().trim()));
    }

    private static List<LineRange> memberRanges(Node construct) {
        Iterable<Node> members = switch (construct) {
            case ServiceDeclarationNode service -> service.members();
            case ClassDefinitionNode classDefinition -> classDefinition.members();
            case TypeDefinitionNode typeDefinition -> objectTypeMembers(typeDefinition.typeDescriptor());
            default -> List.of();
        };
        List<LineRange> ranges = new ArrayList<>();
        members.forEach(member -> ranges.add(member.lineRange()));
        return ranges;
    }

    private static Iterable<Node> objectTypeMembers(Node typeDescriptor) {
        return switch (typeDescriptor) {
            case ObjectTypeDescriptorNode objectType -> objectType.members();
            case DistinctTypeDescriptorNode distinctType -> objectTypeMembers(distinctType.typeDescriptor());
            default -> List.of();
        };
    }

    // `rules`, `'rules` and `"rules"` all name the same field
    private static String fieldName(SpecificFieldNode field) {
        Node name = field.fieldName();
        if (name.kind() == SyntaxKind.STRING_LITERAL) {
            return literalText((BasicLiteralNode) name);
        }
        String text = name.toSourceCode().trim();
        return text.startsWith("'") ? text.substring(1) : text;
    }

    // The value of a string literal, with its character and numeric escapes decoded
    private static String literalText(BasicLiteralNode literal) {
        String text = literal.literalToken().text();
        text = text.substring(1, text.length() - 1);
        StringBuilder value = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 == text.length()) {
                value.append(c);
                continue;
            }
            char escape = text.charAt(++i);
            switch (escape) {
                case 't' -> value.append('\t');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 'u' -> {
                    int close = text.indexOf('}', i);
                    if (i + 1 < text.length() && text.charAt(i + 1) == '{' && close > i + 2
                            && appendCodePoint(value, text.substring(i + 2, close))) {
                        i = close;
                    } else {
                        value.append('\\').append(escape);
                    }
                }
                default -> value.append(escape);
            }
        }
        return value.toString();
    }

    // A malformed escape is a compile error already; it is kept as written
    private static boolean appendCodePoint(StringBuilder value, String hex) {
        try {
            value.appendCodePoint(Integer.parseInt(hex, 16));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Optional<String> stringValue(SemanticModel semanticModel, Node expression) {
        if (expression.kind() == SyntaxKind.STRING_LITERAL) {
            return Optional.of(literalText((BasicLiteralNode) expression));
        }
        return constantValue(semanticModel, expression).filter(String.class::isInstance).map(String.class::cast);
    }

    private static Optional<List<String>> constantStrings(SemanticModel semanticModel, Node expression) {
        return constantValue(semanticModel, expression)
                .filter(List.class::isInstance)
                .map(list -> ((List<?>) list).stream().map(SuppressionCollector::memberValue).toList());
    }

    private static Optional<Object> constantValue(SemanticModel semanticModel, Node expression) {
        return semanticModel.symbol(expression)
                .filter(ConstantSymbol.class::isInstance)
                .map(symbol -> ((ConstantSymbol) symbol).constValue())
                .map(SuppressionCollector::unwrap);
    }

    private static Object unwrap(Object value) {
        return value instanceof ConstantValue constant ? constant.value() : value;
    }

    // Members of a constant list are compiler-internal values whose string form is the member's value.
    private static String memberValue(Object member) {
        Object value = unwrap(member);
        return value instanceof String text ? text : String.valueOf(value);
    }
}
