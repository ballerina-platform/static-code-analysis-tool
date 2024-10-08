package io.ballerina.scan.utils;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.projects.Document;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

public class ScanCodeAnalyzerUtils {
    public static void reportIssue(ScannerContext scannerContext, Document document, Node node, Rule rule) {
        scannerContext.getReporter().reportIssue(document, node.location(), rule);
    }
}
