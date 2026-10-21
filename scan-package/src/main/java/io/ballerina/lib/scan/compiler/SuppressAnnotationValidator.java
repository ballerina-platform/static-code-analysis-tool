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
import io.ballerina.compiler.syntax.tree.ExpressionNode;
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
import org.wso2.ballerinalang.compiler.tree.BLangConstantValue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks that each rule named in a {@code @scan:Suppress} annotation is one {@code bal scan} can report for the
 * package being compiled.
 *
 * @since 0.13.0
 */
class SuppressAnnotationValidator implements AnalysisTask<CompilationAnalysisContext> {

    private static final String SCAN_ORG = "ballerina";
    private static final String SCAN_MODULE = "scan";
    private static final String ANNOTATION_NAME = "Suppress";
    private static final String RULES_FIELD = "rules";
    private static final String REASON_FIELD = "reason";
    private static final Pattern RULE_ID = Pattern.compile("^(ballerina|[A-Za-z0-9_.]+/[A-Za-z0-9_.]+):([1-9][0-9]*)$");

    @Override
    public void perform(CompilationAnalysisContext context) {
        Package currentPackage = context.currentPackage();
        LazyCatalog catalog = new LazyCatalog(currentPackage);
        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel = context.compilation().getSemanticModel(moduleId);
            Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream())
                    .map(module::document)
                    .forEach(document -> validateDocument(context, document, semanticModel, catalog));
        }
    }

    private static void validateDocument(CompilationAnalysisContext context, Document document,
                                         SemanticModel semanticModel, LazyCatalog catalog) {
        Node root = document.syntaxTree().rootNode();
        if (!(root instanceof ModulePartNode modulePart)) {
            return;
        }
        modulePart.accept(new NodeVisitor() {
            @Override
            public void visit(AnnotationNode annotation) {
                if (isSuppressAnnotation(semanticModel, annotation)) {
                    validateAnnotation(context, semanticModel, catalog, annotation);
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
                                           LazyCatalog catalog, AnnotationNode annotation) {
        if (annotation.annotValue().isEmpty()) {
            return;
        }
        for (MappingFieldNode field : annotation.annotValue().get().fields()) {
            if (!(field instanceof SpecificFieldNode specific) || specific.valueExpr().isEmpty()) {
                continue;
            }
            String name = specific.fieldName().toSourceCode().trim();
            ExpressionNode value = specific.valueExpr().get();
            if (name.equals(RULES_FIELD) && value instanceof ListConstructorExpressionNode rules) {
                validateRules(context, semanticModel, catalog.get(), rules);
            } else if (name.equals(RULES_FIELD)) {
                validateConstantRules(context, semanticModel, catalog.get(), value);
            } else if (name.equals(REASON_FIELD)) {
                stringValue(semanticModel, value).filter(String::isBlank)
                        .ifPresent(reason -> report(context, value, DiagnosticCode.BLANK_REASON));
            }
        }
    }

    private static void validateRules(CompilationAnalysisContext context, SemanticModel semanticModel,
                                      RuleCatalog catalog, ListConstructorExpressionNode rules) {
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
            validateRule(context, catalog, element, ruleId.get(), seen);
        }
    }

    // A reference to a constant list is checked as a whole; every diagnostic points at the reference.
    private static void validateConstantRules(CompilationAnalysisContext context, SemanticModel semanticModel,
                                              RuleCatalog catalog, ExpressionNode rules) {
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
        ruleIds.get().forEach(id -> validateRule(context, catalog, rules, id, seen));
    }

    private static void validateRule(CompilationAnalysisContext context, RuleCatalog catalog, Node node, String id,
                                     Set<String> seen) {
        if (!seen.add(id)) {
            report(context, node, DiagnosticCode.DUPLICATE_RULE, id);
            return;
        }
        Matcher matcher = RULE_ID.matcher(id);
        if (!matcher.matches()) {
            report(context, node, DiagnosticCode.INVALID_RULE_FORMAT, id);
            return;
        }
        String provider = matcher.group(1);
        Optional<Set<Integer>> providerRules = catalog.rulesOf(provider);
        if (providerRules.isEmpty()) {
            report(context, node, DiagnosticCode.UNKNOWN_PROVIDER, id, provider);
        } else if (!providerRules.get().contains(Integer.parseInt(matcher.group(2)))) {
            report(context, node, DiagnosticCode.UNKNOWN_RULE, id, provider);
        }
    }

    private static Optional<String> stringValue(SemanticModel semanticModel, Node expression) {
        if (expression.kind() == SyntaxKind.STRING_LITERAL) {
            String text = ((BasicLiteralNode) expression).literalToken().text();
            return Optional.of(text.substring(1, text.length() - 1));
        }
        return constantValue(semanticModel, expression).filter(String.class::isInstance).map(String.class::cast);
    }

    private static Optional<List<String>> constantStrings(SemanticModel semanticModel, Node expression) {
        return constantValue(semanticModel, expression)
                .filter(List.class::isInstance)
                .map(list -> ((List<?>) list).stream().map(SuppressAnnotationValidator::unwrap).toList())
                .filter(values -> values.stream().allMatch(String.class::isInstance))
                .map(values -> values.stream().map(String.class::cast).toList());
    }

    private static Optional<Object> constantValue(SemanticModel semanticModel, Node expression) {
        return semanticModel.symbol(expression)
                .filter(ConstantSymbol.class::isInstance)
                .map(symbol -> ((ConstantSymbol) symbol).constValue())
                .map(SuppressAnnotationValidator::unwrap);
    }

    // Members of a constant list are compiler-internal values rather than ConstantValue instances.
    private static Object unwrap(Object value) {
        if (value instanceof ConstantValue constant) {
            return constant.value();
        }
        return value instanceof BLangConstantValue constant ? constant.value : value;
    }

    private static void report(CompilationAnalysisContext context, Node node, DiagnosticCode code, Object... args) {
        DiagnosticInfo info = new DiagnosticInfo(code.code(), code.messageFormat(), code.severity());
        context.reportDiagnostic(DiagnosticFactory.createDiagnostic(info, node.location(), args));
    }

    /**
     * Builds the rule catalog on first use, so a package without suppressions never reads its dependencies' rules.
     */
    private static final class LazyCatalog {
        private final Package currentPackage;
        private RuleCatalog catalog;

        private LazyCatalog(Package currentPackage) {
            this.currentPackage = currentPackage;
        }

        private RuleCatalog get() {
            if (catalog == null) {
                catalog = RuleCatalog.of(currentPackage);
            }
            return catalog;
        }
    }
}
