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

package io.ballerina.scan.utils;

import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.Optional;

/**
 * {@code SymbolResolver} resolves enclosing symbol names from AST given a file path and line number.
 * This is used to create symbol-based exclusions that are stable across code edits.
 *
 * @since 0.11.1
 */
public final class SymbolResolver {

    private SymbolResolver() {
    }

    /**
     * Resolves the enclosing symbol name at the given line in the specified file.
     *
     * @param project  the Ballerina project
     * @param filePath the file path (relative to project root or absolute)
     * @param line     the 0-indexed line number
     * @return the enclosing symbol name, or {@link Constants#MODULE_LEVEL_SYMBOL} if at module level
     */
    public static String resolveSymbol(Project project, String filePath, int line) {
        Optional<Document> document = findDocument(project, filePath);
        if (document.isEmpty()) {
            return Constants.MODULE_LEVEL_SYMBOL;
        }

        SyntaxTree syntaxTree = document.get().syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();

        String enclosingSymbol = findEnclosingSymbol(modulePartNode, line);
        return enclosingSymbol != null ? enclosingSymbol : Constants.MODULE_LEVEL_SYMBOL;
    }

    /**
     * Resolves the hash of the line content at the given line in the specified file.
     *
     * @param project  the Ballerina project
     * @param filePath the file path (relative to project root or absolute)
     * @param line     the 0-indexed line number
     * @return the line hash as a hex string, or empty string on failure
     */
    public static String resolveLineHash(Project project, String filePath, int line) {
        Optional<Document> document = findDocument(project, filePath);
        if (document.isEmpty()) {
            return "";
        }

        try {
            String lineContent = document.get().textDocument().line(line).text();
            return Integer.toHexString(lineContent.trim().hashCode());
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Finds the document in the project matching the given file path.
     *
     * @param project  the Ballerina project
     * @param filePath the file path to search for
     * @return the optional document matching the file path
     */
    private static Optional<Document> findDocument(Project project, String filePath) {
        Path targetPath = Path.of(filePath).normalize();

        for (Module module : project.currentPackage().modules()) {
            // Main
            for (DocumentId docId : module.documentIds()) {
                Optional<Path> docPath = project.documentPath(docId);
                if (docPath.isPresent()) {
                    Path normalizedDocPath = docPath.get().normalize();
                    if (normalizedDocPath.endsWith(targetPath) || targetPath.endsWith(normalizedDocPath)
                            || normalizedDocPath.toString().equals(targetPath.toString())) {
                        return Optional.of(module.document(docId));
                    }
                }
            }
            // Tests
            for (DocumentId docId : module.testDocumentIds()) {
                Optional<Path> docPath = project.documentPath(docId);
                if (docPath.isPresent()) {
                    Path normalizedDocPath = docPath.get().normalize();
                    if (normalizedDocPath.endsWith(targetPath) || targetPath.endsWith(normalizedDocPath)
                            || normalizedDocPath.toString().equals(targetPath.toString())) {
                        return Optional.of(module.document(docId));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Walks the top-level members of a module to find the enclosing symbol for a given line.
     *
     * @param modulePartNode the root node of the module
     * @param line           the 0-indexed line number
     * @return the symbol name, or null if at module level
     */
    private static String findEnclosingSymbol(ModulePartNode modulePartNode, int line) {
        for (Node member : modulePartNode.members()) {
            if (coversLine(member, line)) {
                String topLevelName = extractSymbolName(member);
                String innerName = findClosestInnerNamedSymbol(member, line);
                if (innerName != null && topLevelName != null) {
                    return topLevelName + " -> " + innerName;
                }
                return topLevelName;
            }
        }
        return null;
    }

    private static String findClosestInnerNamedSymbol(Node node, int line) {
        if (node instanceof io.ballerina.compiler.syntax.tree.NonTerminalNode nonTerminal) {
            for (Node child : nonTerminal.children()) {
                if (child != null && coversLine(child, line)) {
                    String childName = extractLocalSymbolName(child);
                    String deepResult = findClosestInnerNamedSymbol(child, line);
                    
                    if (childName != null && deepResult != null) {
                        return childName + " -> " + deepResult;
                    } else if (deepResult != null) {
                        return deepResult;
                    } else if (childName != null) {
                        return childName;
                    }
                }
            }
        }
        return null;
    }

    private static String extractLocalSymbolName(Node node) {
        if (node instanceof io.ballerina.compiler.syntax.tree.FunctionDefinitionNode funcNode) {
            return funcNode.functionName().text();
        }
        if (node instanceof io.ballerina.compiler.syntax.tree.MethodDeclarationNode declNode) {
            return declNode.methodName().text();
        }
        if (node instanceof io.ballerina.compiler.syntax.tree.VariableDeclarationNode varNode) {
            return varNode.typedBindingPattern().bindingPattern().toString().trim();
        }
        if (node instanceof io.ballerina.compiler.syntax.tree.RecordFieldNode recordNode) {
            return recordNode.fieldName().text();
        }
        if (node instanceof io.ballerina.compiler.syntax.tree.RequiredParameterNode paramNode) {
            return paramNode.paramName().map(t -> t.text()).orElse(null);
        }
        return null;
    }

    private static boolean coversLine(Node node, int line) {
        LineRange lineRange = node.lineRange();
        LinePosition startLine = lineRange.startLine();
        LinePosition endLine = lineRange.endLine();
        return line >= startLine.line() && line <= endLine.line();
    }

    /**
     * Extracts the symbol name from a syntax tree node.
     *
     * @param node the syntax tree node
     * @return the symbol name, or null if the node is not a named construct
     */
    private static String extractSymbolName(Node node) {
        if (node instanceof FunctionDefinitionNode functionNode) {
            return functionNode.functionName().text();
        }
        if (node instanceof ClassDefinitionNode classNode) {
            return classNode.className().text();
        }
        if (node instanceof TypeDefinitionNode typeNode) {
            return typeNode.typeName().text();
        }
        if (node instanceof ServiceDeclarationNode serviceNode) {
            if (serviceNode.absoluteResourcePath().isEmpty()) {
                return "service";
            }
            StringBuilder servicePath = new StringBuilder("service");
            serviceNode.absoluteResourcePath().forEach(pathNode ->
                    servicePath.append(pathNode.toString().trim()));
            return servicePath.toString();
        }
        return null;
    }
}
