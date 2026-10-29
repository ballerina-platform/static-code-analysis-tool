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
import io.ballerina.compiler.api.symbols.Annotatable;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ExternalFunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.DistinctTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ExplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ExternalFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.IncludedRecordParameterNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MemberTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.ballerina.scan.utils.Constants.IMPORT_GENERATOR_FILE;

/**
 * Collects the {@code @scan:Suppress} annotations of a package. Each annotation is read from the value the compiler
 * resolved for it, so constants, string templates and escapes count as written and comments do not interfere.
 *
 * @since 0.13.0
 */
final class SuppressionCollector {

    static final String SCAN_ORG = "ballerina";
    static final String SCAN_PACKAGE = "scan";
    static final String ANNOTATION_NAME = "Suppress";
    private static final String RULES_FIELD = "rules";
    private static final String REASON_FIELD = "reason";
    private static final Pattern NUMERIC_ESCAPE = Pattern.compile("\\\\u\\{([0-9A-Fa-f]+)}");
    private static final Pattern CHARACTER_ESCAPE = Pattern.compile("\\\\(.)");

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
        String fileName = ScanUtils.getReportFileName(document);
        modulePart.accept(new NodeVisitor() {
            @Override
            public void visit(AnnotationNode annotation) {
                resolve(semanticModel, annotation).ifPresent(value ->
                        suppressions.add(toSuppression(annotation, value, filePath, fileName)));
                // A construct inside another annotation's value can carry suppressions of its own
                annotation.annotValue().ifPresent(value -> value.accept(this));
            }
        });
    }

    private static SuppressionAnnotation toSuppression(AnnotationNode annotation, Map<?, ?> value, String filePath,
                                                       String fileName) {
        Node construct = annotation.parent() instanceof MetadataNode ? annotation.parent().parent()
                : annotation.parent();
        List<String> rules = null;
        if (unwrap(value.get(RULES_FIELD)) instanceof List<?> members) {
            rules = new ArrayList<>();
            for (Object member : members) {
                // Members are compiler-internal values whose string form is the member's value
                Object rule = unwrap(member);
                rules.add(rule instanceof String text ? text : String.valueOf(rule));
            }
        }
        String reason = unwrap(value.get(REASON_FIELD)) instanceof String text && !text.isBlank() ? text : null;
        return new SuppressionAnnotation(filePath, fileName, annotation.lineRange(), construct.lineRange(),
                construct.textRange().length(), memberRanges(construct), rules == null,
                rules == null ? List.of() : rules, rules == null ? List.of() : ruleLocations(annotation, rules.size()),
                reason);
    }

    // A list written in place locates each rule at its member; a constant locates them all at its reference
    private static List<LineRange> ruleLocations(AnnotationNode annotation, int ruleCount) {
        Node rules = rulesValue(annotation).orElse(annotation);
        if (rules instanceof ListConstructorExpressionNode list && list.expressions().size() == ruleCount) {
            return list.expressions().stream().map(Node::lineRange).toList();
        }
        return Collections.nCopies(ruleCount, rules.lineRange());
    }

    private static Optional<Node> rulesValue(AnnotationNode annotation) {
        if (annotation.annotValue().isEmpty()) {
            return Optional.empty();
        }
        for (MappingFieldNode field : annotation.annotValue().get().fields()) {
            if (field instanceof SpecificFieldNode specific && fieldName(specific).equals(RULES_FIELD)) {
                // A shorthand field such as `{rules}` takes the value of the constant its name refers to
                return Optional.of(specific.valueExpr().map(Node.class::cast).orElse(specific.fieldName()));
            }
        }
        return Optional.empty();
    }

    // `rules`, `'rules`, `"rules"` and their escaped forms all name the same field
    private static String fieldName(SpecificFieldNode field) {
        Node name = field.fieldName();
        String text = name instanceof BasicLiteralNode literal ? literal.literalToken().text() : ((Token) name).text();
        if (text.startsWith("\"")) {
            return unescape(text.substring(1, text.length() - 1));
        }
        return unescape(text.startsWith("'") ? text.substring(1) : text);
    }

    // io.ballerina.identifier does not export its unescaping to this module
    private static String unescape(String text) {
        String decoded = NUMERIC_ESCAPE.matcher(text).replaceAll(escape ->
                Matcher.quoteReplacement(Character.toString(Integer.parseInt(escape.group(1), 16))));
        return CHARACTER_ESCAPE.matcher(decoded).replaceAll("$1");
    }

    // The resolved value of a @scan:Suppress annotation, absent for any other annotation
    private static Optional<Map<?, ?>> resolve(SemanticModel semanticModel, AnnotationNode annotation) {
        return attachments(semanticModel, annotation).stream()
                .filter(attachment -> attachment.getLocation()
                        .map(location -> location.lineRange().equals(annotation.lineRange())).orElse(false))
                .filter(SuppressionCollector::isSuppress)
                .findFirst()
                .flatMap(AnnotationAttachmentSymbol::attachmentValue)
                .map(ConstantValue::value)
                .filter(Map.class::isInstance)
                .map(Map.class::cast);
    }

    // The construct's symbol holds the attachments, except where the annotation is on a part of the construct
    private static List<AnnotationAttachmentSymbol> attachments(SemanticModel semanticModel,
                                                                AnnotationNode annotation) {
        Node parent = annotation.parent();
        if (parent instanceof ReturnTypeDescriptorNode) {
            return functionType(semanticModel, parent.parent().parent())
                    .flatMap(FunctionTypeSymbol::returnTypeAnnotations)
                    .map(Annotatable::annotAttachments)
                    .orElse(List.of());
        }
        if (parent instanceof ExternalFunctionBodyNode body) {
            return semanticModel.symbol(body.parent())
                    .filter(ExternalFunctionSymbol.class::isInstance)
                    .map(symbol -> ((ExternalFunctionSymbol) symbol).annotAttachmentsOnExternal())
                    .orElse(List.of());
        }
        if (parent instanceof MemberTypeDescriptorNode member
                && member.parent() instanceof TupleTypeDescriptorNode tuple) {
            int index = tuple.memberTypeDesc().stream().toList().indexOf(member);
            return definedType(semanticModel, tuple)
                    .filter(TupleTypeSymbol.class::isInstance)
                    .map(type -> ((TupleTypeSymbol) type).members())
                    .filter(members -> index >= 0 && index < members.size())
                    .map(members -> members.get(index).annotAttachments())
                    .orElse(List.of());
        }
        Node construct = parent instanceof MetadataNode ? parent.parent() : parent;
        Node named = switch (construct) {
            case ListenerDeclarationNode listener -> listener.variableName();
            case IncludedRecordParameterNode parameter -> parameter.paramName().map(Node.class::cast).orElse(parameter);
            default -> construct;
        };
        return semanticModel.symbol(named)
                .filter(Annotatable.class::isInstance)
                .map(symbol -> ((Annotatable) symbol).annotAttachments())
                .orElse(List.of());
    }

    private static Optional<FunctionTypeSymbol> functionType(SemanticModel semanticModel, Node owner) {
        Optional<? extends Symbol> type = owner instanceof ExplicitAnonymousFunctionExpressionNode function
                ? semanticModel.typeOf(function)
                : semanticModel.symbol(owner).map(symbol ->
                        symbol instanceof FunctionSymbol function ? function.typeDescriptor() : symbol);
        return type.filter(FunctionTypeSymbol.class::isInstance).map(FunctionTypeSymbol.class::cast);
    }

    // A type descriptor has no symbol of its own; only the type definition it defines can be looked up
    private static Optional<TypeSymbol> definedType(SemanticModel semanticModel, Node typeDescriptor) {
        return typeDescriptor.parent() instanceof TypeDefinitionNode definition
                ? semanticModel.symbol(definition)
                        .filter(TypeDefinitionSymbol.class::isInstance)
                        .map(symbol -> ((TypeDefinitionSymbol) symbol).typeDescriptor())
                : Optional.empty();
    }

    // Identified by the attachment's annotation, since a worker's annotation node has no symbol of its own
    private static boolean isSuppress(AnnotationAttachmentSymbol attachment) {
        AnnotationSymbol annotation = attachment.typeDescriptor();
        return annotation.getName().filter(ANNOTATION_NAME::equals).isPresent()
                && annotation.getModule().map(module -> SCAN_ORG.equals(module.id().orgName())
                && SCAN_PACKAGE.equals(module.id().moduleName())).orElse(false);
    }

    private static Object unwrap(Object value) {
        return value instanceof ConstantValue constant ? constant.value() : value;
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
}
