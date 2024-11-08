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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.scan.ScannerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SensitiveParameterTracker extends NodeVisitor {
    public static final String FUNCTIONS_WITH_SENSITIVE_PARAMETERS = "functionsWithSensitiveParameters";

    private final SyntaxTree syntaxTree;
    private final SemanticModel semanticModel;
    private final Map<String, Object> userData;

    SensitiveParameterTracker(SyntaxTree syntaxTree, ScannerContext scannerContext, SemanticModel semanticModel) {
        this.syntaxTree = syntaxTree;
        this.semanticModel = semanticModel;
        this.userData = scannerContext.userData();
    }

    public void scan() {
        if (!userData.containsKey(FUNCTIONS_WITH_SENSITIVE_PARAMETERS)) {
            userData.put(FUNCTIONS_WITH_SENSITIVE_PARAMETERS, new HashMap<Integer, FunctionWithSensitiveParams>());
        }
        this.visit((ModulePartNode) syntaxTree.rootNode());
    }

    @Override
    public void visit(MethodDeclarationNode methodDeclarationNode) {
        SeparatedNodeList<ParameterNode> parameters = methodDeclarationNode.methodSignature().parameters();
        List<Integer> sensitiveParamsPositions = getSensitiveParameterPositions(parameters);
        if (sensitiveParamsPositions.isEmpty()) {
            super.visit(methodDeclarationNode);
            return;
        }
        Optional<Symbol> functionSymbol = this.semanticModel.symbol(methodDeclarationNode);
        if (functionSymbol.isEmpty()) {
            super.visit(methodDeclarationNode);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Integer, FunctionWithSensitiveParams> functionWithSensitiveParams =
                (Map<Integer, FunctionWithSensitiveParams>) userData.get(FUNCTIONS_WITH_SENSITIVE_PARAMETERS);
        functionWithSensitiveParams.put(functionSymbol.get().hashCode(),
                new FunctionWithSensitiveParams(functionSymbol.get(), sensitiveParamsPositions));
        super.visit(methodDeclarationNode);
    }

    private List<Integer> getSensitiveParameterPositions(SeparatedNodeList<ParameterNode> parameters) {
        List<Integer> positions = new ArrayList<>();
        int position = 0;
        for (ParameterNode parameter : parameters) {
            String paramName = getParameterName(parameter);
            if (SecretChecker.isSecretName(paramName)) {
                positions.add(position);
            }
            position++;
        }
        return positions;
    }

    private String getParameterName(ParameterNode parameter) {
        if (parameter.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
            return ((DefaultableParameterNode) parameter).paramName().map(Token::text)
                    .orElse(parameter.toSourceCode().trim());
        }
        if (parameter.kind() == SyntaxKind.REQUIRED_PARAM) {
            return ((RequiredParameterNode) parameter).paramName().map(Token::text)
                    .orElse(parameter.toSourceCode().trim());
        }
        return parameter.toSourceCode().trim();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        SeparatedNodeList<ParameterNode> parameters = functionDefinitionNode.functionSignature().parameters();
        List<Integer> sensitiveParameterPositions = getSensitiveParameterPositions(parameters);
        if (sensitiveParameterPositions.isEmpty()) {
            super.visit(functionDefinitionNode);
            return;
        }
        Optional<Symbol> functionSymbol = this.semanticModel.symbol(functionDefinitionNode);
        if (functionSymbol.isEmpty()) {
            super.visit(functionDefinitionNode);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Integer, FunctionWithSensitiveParams> map = (Map<Integer, FunctionWithSensitiveParams>)
                userData.get(FUNCTIONS_WITH_SENSITIVE_PARAMETERS);
        map.put(functionSymbol.get().hashCode(),
                new FunctionWithSensitiveParams(functionSymbol.get(), sensitiveParameterPositions));
        super.visit(functionDefinitionNode);
    }
}


