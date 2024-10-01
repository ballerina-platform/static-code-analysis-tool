package io.ballerina.scan.utils;

import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

import java.util.List;

public class ScanCodeAnalyzerUtils {
    public static boolean getQualifier(List<Qualifier> qualifierList, String qualifierValue) {
        for (Qualifier qualifier : qualifierList) {
            if (qualifier.getValue() == qualifierValue) {
                return true;
            }
        }
        return false;
    }

    public static boolean getQualifier(NodeList<Token> qualifierList, SyntaxKind qualifier) {
        for (Token token : qualifierList) {
            if (qualifier == token.kind()) {
                return true;
            }
        }
        return false;
    }

    public static void reportIssue(ScannerContext scannerContext, Document document, Node node, Rule rule) {
        scannerContext.getReporter().reportIssue(document, node.location(), rule);
    }
}
