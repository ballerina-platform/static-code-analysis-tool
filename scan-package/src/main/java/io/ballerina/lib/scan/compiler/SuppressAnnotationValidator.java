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

package io.ballerina.lib.scan.compiler;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks the form of each {@code @scan:Suppress} annotation: that every rule ID is well formed and listed once, and
 * that the reason is not blank. Whether a rule exists is checked by {@code bal scan}, which knows the rules of the
 * scan tool and analyzers it runs.
 *
 * @since 1.0.0
 */
class SuppressAnnotationValidator implements AnalysisTask<CompilationAnalysisContext> {

    private static final String SCAN_ORG = "ballerina";
    private static final String SCAN_MODULE = "scan";
    private static final String ANNOTATION_NAME = "Suppress";
    private static final String RULES_FIELD = "rules";
    private static final String REASON_FIELD = "reason";
    private static final Pattern RULE_ID = Pattern.compile("^(ballerina|[A-Za-z0-9_.]+/[A-Za-z0-9_.]+):[1-9][0-9]*$");

    @Override
    public void perform(CompilationAnalysisContext context) {
        Package currentPackage = context.currentPackage();
        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel = context.compilation().getSemanticModel(moduleId);
            Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream())
                    .map(module::document)
                    .forEach(document -> validateDocument(context, document, semanticModel));
        }
    }

    private static void validateDocument(CompilationAnalysisContext context, Document document,
                                         SemanticModel semanticModel) {
        Node root = document.syntaxTree().rootNode();
        if (!(root instanceof ModulePartNode modulePart)) {
            return;
        }
        modulePart.accept(new NodeVisitor() {
            @Override
            public void visit(AnnotationNode annotation) {
                if (isSuppressAnnotation(semanticModel, annotation)) {
                    validateAnnotation(context, semanticModel, annotation);
                }
            }
        });
    }

    private static boolean isSuppressAnnotation(SemanticModel semanticModel, AnnotationNode annotation) {
        if (!(semanticModel.symbol(annotation).orElse(null) instanceof AnnotationSymbol symbol)) {
            return false;
        }
        return symbol.getName().filter(ANNOTATION_NAME::equals).isPresent()
                && symbol.getModule().map(module -> module.id().orgName().equals(SCAN_ORG)
                && module.id().moduleName().equals(SCAN_MODULE)).orElse(false);
    }

    private static void validateAnnotation(CompilationAnalysisContext context, SemanticModel semanticModel,
                                           AnnotationNode annotation) {
        if (annotation.annotValue().isEmpty()) {
            return;
        }
        for (MappingFieldNode field : annotation.annotValue().get().fields()) {
            if (!(field instanceof SpecificFieldNode specific)) {
                continue;
            }
            String name = fieldName(specific);
            // A shorthand field such as `{rules}` takes the value of the constant its name refers to
            Node value = specific.valueExpr().map(Node.class::cast).orElse(specific.fieldName());
            if (name.equals(RULES_FIELD) && value instanceof ListConstructorExpressionNode rules) {
                validateRules(context, semanticModel, rules);
            } else if (name.equals(RULES_FIELD)) {
                validateConstantRules(context, semanticModel, value);
            } else if (name.equals(REASON_FIELD)) {
                stringValue(semanticModel, value).filter(String::isBlank)
                        .ifPresent(reason -> report(context, value, DiagnosticCode.BLANK_REASON));
            }
        }
    }

    private static void validateRules(CompilationAnalysisContext context, SemanticModel semanticModel,
                                      ListConstructorExpressionNode rules) {
        // `[]` compiles against the non-empty tuple type because the first member takes the string filler value
        if (rules.expressions().isEmpty()) {
            report(context, rules, DiagnosticCode.EMPTY_RULE_LIST);
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Node element : rules.expressions()) {
            Optional<String> ruleId = stringValue(semanticModel, element);
            if (ruleId.isEmpty()) {
                report(context, element, DiagnosticCode.UNVERIFIABLE_RULE, element.toSourceCode().trim());
                continue;
            }
            validateRule(context, element, ruleId.get(), seen);
        }
    }

    // A reference to a constant list is checked as a whole; every diagnostic points at the reference.
    private static void validateConstantRules(CompilationAnalysisContext context, SemanticModel semanticModel,
                                              Node rules) {
        Optional<List<String>> ruleIds = constantStrings(semanticModel, rules);
        if (ruleIds.isEmpty()) {
            report(context, rules, DiagnosticCode.UNVERIFIABLE_RULE, rules.toSourceCode().trim());
            return;
        }
        if (ruleIds.get().isEmpty()) {
            report(context, rules, DiagnosticCode.EMPTY_RULE_LIST);
            return;
        }
        Set<String> seen = new HashSet<>();
        ruleIds.get().forEach(id -> validateRule(context, rules, id, seen));
    }

    private static void validateRule(CompilationAnalysisContext context, Node node, String id, Set<String> seen) {
        if (!seen.add(id)) {
            report(context, node, DiagnosticCode.DUPLICATE_RULE, id);
        } else if (!RULE_ID.matcher(id).matches()) {
            report(context, node, DiagnosticCode.INVALID_RULE_FORMAT, id);
        }
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

    private static String literalText(BasicLiteralNode literal) {
        String text = literal.literalToken().text();
        return text.substring(1, text.length() - 1);
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
                .map(list -> ((List<?>) list).stream().map(SuppressAnnotationValidator::memberValue).toList());
    }

    private static Optional<Object> constantValue(SemanticModel semanticModel, Node expression) {
        return semanticModel.symbol(expression)
                .filter(ConstantSymbol.class::isInstance)
                .map(symbol -> ((ConstantSymbol) symbol).constValue())
                .map(SuppressAnnotationValidator::unwrap);
    }

    private static Object unwrap(Object value) {
        return value instanceof ConstantValue constant ? constant.value() : value;
    }

    // Members of a constant list are compiler-internal values whose string form is the member's value.
    private static String memberValue(Object member) {
        Object value = unwrap(member);
        return value instanceof String text ? text : String.valueOf(value);
    }

    private static void report(CompilationAnalysisContext context, Node node, DiagnosticCode code, Object... args) {
        DiagnosticInfo info = new DiagnosticInfo(code.code(), code.messageFormat(), code.severity());
        context.reportDiagnostic(DiagnosticFactory.createDiagnostic(info, node.location(), args));
    }
}
