/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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
package io.ballerina.scan.utils;

import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.tools.diagnostics.Location;

/**
 * {@code ScanCodeAnalyzerUtils} contains the util functions for the static code analysis.
 *
 * @since 0.1.0
 * */
public class ScanCodeAnalyzerUtils {
    public static void reportIssue(ScannerContext scannerContext, Document document, Node node, Rule rule) {
        scannerContext.getReporter().reportIssue(document, node.location(), rule);
    }

    public static void reportIssue(ScannerContext scannerContext, Document document, Location location, Rule rule) {
        scannerContext.getReporter().reportIssue(document, location, rule);
    }

    public static boolean isSameSimpleExpression(Node n1, Node n2) {
        if (n1 instanceof SimpleNameReferenceNode lhsExp && n2 instanceof SimpleNameReferenceNode rhsExpr) {
            return lhsExp.name().text().equals(rhsExpr.name().text());
        }

        if (n1 instanceof QualifiedNameReferenceNode lhsExp && n2 instanceof QualifiedNameReferenceNode rhsExpr) {
            return lhsExp.modulePrefix() != null && rhsExpr.modulePrefix() != null
                    && lhsExp.modulePrefix().text().equals(rhsExpr.modulePrefix().text())
                    && lhsExp.identifier().text().equals(rhsExpr.identifier().text());
        }

        if (n1 instanceof FieldAccessExpressionNode lhsExp && n2 instanceof FieldAccessExpressionNode rhsExpr) {
            // only the simple field access expressions will be considered.
            return isSameSimpleExpression(lhsExp.fieldName(), rhsExpr.fieldName())
                    && isSameSimpleExpression(lhsExp.expression(), rhsExpr.expression());
        }

        if (n1 instanceof IndexedExpressionNode lhsExp && n2 instanceof IndexedExpressionNode rhsExpr) {
            // only the simple field access expressions will be considered.
            return isSameSimpleExpression(lhsExp.containerExpression(), rhsExpr.containerExpression())
                    && isSameKeyExpression(lhsExp.keyExpression(), rhsExpr.keyExpression());
        }

        if (n1 instanceof BuiltinSimpleNameReferenceNode lhsExp
                && n2 instanceof BuiltinSimpleNameReferenceNode rhsExpr) {
            return lhsExp.name().text().equals(rhsExpr.name().text());
        }

        if (n1 instanceof BasicLiteralNode lhsExp && n2 instanceof BasicLiteralNode rhsExpr) {
            return lhsExp.literalToken().text().equals(rhsExpr.literalToken().text());
        }

        return false;
    }

    private static boolean isSameKeyExpression(SeparatedNodeList<ExpressionNode> lhsKeyExpNodes,
                                               SeparatedNodeList<ExpressionNode> rhsKeyExpNodes) {
        if (lhsKeyExpNodes.size() != rhsKeyExpNodes.size()) {
            return false;
        }

        for (int i = 0; i < lhsKeyExpNodes.size(); i++) {
            if (!isSameSimpleExpression(lhsKeyExpNodes.get(i), rhsKeyExpNodes.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isDefinedQualifiedNameReference(Node node, String module, String identifier) {
        if (node instanceof QualifiedNameReferenceNode ref) {
            return ref.modulePrefix() != null && ref.identifier() != null
                    && ref.modulePrefix().text().equals(module) && ref.identifier().text().equals(identifier);
        }
        return false;
    }

    public static boolean isEqualToProvidedLiteralIdentifier(Node node, String identifierName) {
        if (identifierName.equals(Constants.Token.MINUS_ONE)) {
            if (node instanceof UnaryExpressionNode exp) {
                return exp.unaryOperator().kind().equals(SyntaxKind.MINUS_TOKEN)
                        && isEqualToProvidedLiteralIdentifier(exp.expression(), Constants.Token.ONE);
            }
            return false;
        }
        if (node instanceof BasicLiteralNode ref) {
            return ref.literalToken().text().equals(identifierName);
        }
        return false;
    }
}
