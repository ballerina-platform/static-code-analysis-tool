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
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
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
import io.ballerina.scan.ScannerContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.ballerina.scan.internal.CoreRule.HARD_CODED_SECRET;
import static io.ballerina.scan.internal.CoreRule.NON_CONFIGURABLE_SECRET;
import static io.ballerina.scan.internal.SensitiveParameterTracker.FUNCTIONS_WITH_SENSITIVE_PARAMETERS;

public class SecretChecker extends NodeVisitor {
    private static final Pattern SECRET_WORDS = Pattern
            .compile("password|passwd|pwd|passphrase|secret|clientSecret|PASSWORD|PASSWD|PWD|PASSPHRASE" +
                    "|PASS_PHRASE|SECRET|CLIENTSECRET|CLIENT_SECRET|APIKEY|API_KEY");
    private static final Pattern URL_PREFIX = Pattern.compile("^\\w{1,8}://");
    private static final Pattern NON_EMPTY_URL_CREDENTIAL = Pattern.compile("(?<user>[^\\s:]*+):(?<password>\\S++)");
    private static final String PLACE_HOLDER_STRING = "xyz";

    private final Document document;
    private final SyntaxTree syntaxTree;
    private final ScannerContext scannerContext;
    private final SemanticModel semanticModel;
    private final Map<Integer, FunctionWithSensitiveParams> functionsWithCredentialParams;

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
        if (identifier.isPresent() && identifier.get().kind() == SymbolKind.VARIABLE) {
            VariableSymbol variableSymbol = (VariableSymbol) identifier.get();
            if (!isConfigurable(variableSymbol)) {
                reportNonConfigurableSecret(specificFieldNode);
            }
        }
    }

    @Override
    public void visit(BasicLiteralNode basicLiteralNode) {
        if (basicLiteralNode.kind() == SyntaxKind.STRING_LITERAL) {
            String cleanedLiteral = stripQuotes(basicLiteralNode.literalToken().text());
            if (isUrlWithCredentials(cleanedLiteral)) {
                reportHardCodedSecret(basicLiteralNode);
            }
        }
        super.visit(basicLiteralNode);
    }

    private String stripQuotes(String text) {
        return text.length() > 1 ? text.substring(1, text.length() - 1) : text;
    }

    private boolean isUrlWithCredentials(String stringLiteral) {
        if (URL_PREFIX.matcher(stringLiteral).find()) {
            try {
                String userInfo = new URL(stringLiteral).getUserInfo();
                return userInfo != null && NON_EMPTY_URL_CREDENTIAL.matcher(userInfo).matches();
            } catch (MalformedURLException ignored) {
                // Ignore since this not a valid url
            }
        }
        return false;
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
                pattern.append(PLACE_HOLDER_STRING);
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
        switch (expressionNode.kind()) {
            case STRING_LITERAL -> reportHardCodedSecret(expressionNode);
            case SIMPLE_NAME_REFERENCE -> validateSimpleNameReference(expressionNode);
            default -> reportNonConfigurableSecret(expressionNode);
        }
    }

    private void reportHardCodedSecret(Node node) {
        scannerContext.getReporter().reportIssue(document, node.location(), HARD_CODED_SECRET.rule());
    }

    private void reportNonConfigurableSecret(Node node) {
        scannerContext.getReporter().reportIssue(document, node.location(), NON_CONFIGURABLE_SECRET.rule());
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
        if (identifier.get().kind() == SymbolKind.CONSTANT) {
            reportHardCodedSecret(expressionNode);
        }
    }

    private boolean isConfigurable(VariableSymbol variableSymbol) {
        return variableSymbol.qualifiers().stream()
                .anyMatch(qualifier -> Qualifier.CONFIGURABLE.name().equals(qualifier.name().trim()));
    }
}
