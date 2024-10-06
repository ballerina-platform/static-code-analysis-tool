package io.ballerina.scan.utils;

import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.tools.diagnostics.Location;

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

        if (n1 instanceof BuiltinSimpleNameReferenceNode lhsExp
                && n2 instanceof BuiltinSimpleNameReferenceNode rhsExpr) {
            return lhsExp.name().text().equals(rhsExpr.name().text());
        }
        return false;
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
