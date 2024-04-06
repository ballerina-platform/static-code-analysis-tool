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

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;

/**
 * {@code StaticCodeAnalyzer} contains the logic to perform core static code analysis on Ballerina documents.
 *
 * @since 0.1.0
 * */
class StaticCodeAnalyzer extends NodeVisitor {
    private final SyntaxTree syntaxTree;

    StaticCodeAnalyzer(Document document, ScannerContextImpl scannerContext) {
        this.syntaxTree = document.syntaxTree();
    }

    void analyze() {
        this.visit((ModulePartNode) syntaxTree.rootNode());
    }
}
